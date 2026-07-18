package com.example.praxis.apiquickstart.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.example.praxis.apiquickstart.hr.service.AbsenceCriticalityPolicy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the versioned HR golden suite and evaluates it with an oracle independent from the
 * PostgreSQL view. PostgreSQL parity is added in the P2 Testcontainers proof.
 */
class AbsenceAnalyticsGoldenContractTest {

    private static final String DIAGNOSTIC_ASSIGNMENT_MISSING = "DEPARTMENT_ASSIGNMENT_MISSING";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void goldenSuiteIsSchemaValidAndItsIndependentOracleMatchesEveryCase() throws IOException {
        JsonNode schemaNode = read("/absence-analytics-lab/absence-analytics-golden-suite.schema.json");
        JsonNode suite = read("/absence-analytics-lab/absence-analytics-golden-suite.json");

        Set<ValidationMessage> violations = JsonSchemaFactory
                .getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(schemaNode)
                .validate(suite);
        assertThat(violations).as("golden suite JSON Schema violations").isEmpty();

        assertThat(suite.path("policy").path("id").asText()).isEqualTo(AbsenceCriticalityPolicy.POLICY_ID);
        assertThat(suite.path("policy").path("version").asText()).isEqualTo(AbsenceCriticalityPolicy.POLICY_VERSION);

        Set<String> caseIds = new HashSet<>();
        for (JsonNode goldenCase : suite.path("cases")) {
            assertThat(caseIds.add(goldenCase.path("id").asText()))
                    .as("duplicate golden case %s", goldenCase.path("id").asText())
                    .isTrue();

            OracleResult actual = calculate(goldenCase, suite.path("policy"));
            assertThat(actual.rows()).containsExactlyInAnyOrderElementsOf(expectedRows(goldenCase));
            assertThat(actual.diagnostics()).containsExactlyInAnyOrderElementsOf(expectedDiagnostics(goldenCase));
            assertThat(actual.comparison()).containsExactlyInAnyOrderElementsOf(expectedComparison(goldenCase));
        }
        assertThat(caseIds).containsExactlyInAnyOrder("G2A-01", "G2A-02", "G2A-03", "G2A-04", "G2A-05");
    }

    private OracleResult calculate(JsonNode goldenCase, JsonNode policy) {
        List<Assignment> assignments = assignments(goldenCase.path("assignments"));
        Set<AttributedDay> uniqueDays = new HashSet<>();
        Set<String> diagnostics = new HashSet<>();

        for (JsonNode absence : goldenCase.path("absences")) {
            int employeeId = absence.path("employeeId").asInt();
            LocalDate from = LocalDate.parse(absence.path("from").asText());
            LocalDate to = LocalDate.parse(absence.path("to").asText());
            for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
                LocalDate attributedDay = day;
                Assignment assignment = assignments.stream()
                        .filter(candidate -> candidate.employeeId() == employeeId && candidate.includes(attributedDay))
                        .findFirst()
                        .orElse(null);
                if (assignment == null) {
                    diagnostics.add(DIAGNOSTIC_ASSIGNMENT_MISSING);
                    continue;
                }
                uniqueDays.add(new AttributedDay(employeeId, assignment.departmentId(), firstDayOfMonth(attributedDay), attributedDay));
            }
        }

        Map<RowKey, List<LocalDate>> daysByRow = new HashMap<>();
        uniqueDays.forEach(day -> daysByRow
                .computeIfAbsent(new RowKey(day.employeeId(), day.departmentId(), day.competencia()), ignored -> new ArrayList<>())
                .add(day.day()));

        List<GoldenRow> rows = new ArrayList<>();
        daysByRow.forEach((key, dates) -> {
            dates.sort(Comparator.naturalOrder());
            long absentDays = dates.size();
            rows.add(new GoldenRow(
                    key.employeeId(), key.departmentId(), key.competencia(), dates.getFirst(), dates.getLast(), absentDays,
                    classify(absentDays, policy)
            ));
        });

        return new OracleResult(rows, diagnostics, comparison(rows, goldenCase));
    }

    private List<Comparison> comparison(List<GoldenRow> rows, JsonNode goldenCase) {
        LocalDate current = LocalDate.parse(goldenCase.path("currentCompetencia").asText());
        LocalDate previous = LocalDate.parse(goldenCase.path("previousCompetencia").asText());
        Set<Integer> departments = new HashSet<>();
        rows.stream().filter(row -> row.competencia().equals(current) || row.competencia().equals(previous))
                .forEach(row -> departments.add(row.departmentId()));

        return departments.stream().map(departmentId -> new Comparison(
                departmentId,
                employeeCount(rows, departmentId, current),
                absentDays(rows, departmentId, current),
                employeeCount(rows, departmentId, previous),
                absentDays(rows, departmentId, previous)
        )).toList();
    }

    private long employeeCount(List<GoldenRow> rows, int departmentId, LocalDate competencia) {
        return rows.stream().filter(row -> row.departmentId() == departmentId && row.competencia().equals(competencia))
                .map(GoldenRow::employeeId).distinct().count();
    }

    private long absentDays(List<GoldenRow> rows, int departmentId, LocalDate competencia) {
        return rows.stream().filter(row -> row.departmentId() == departmentId && row.competencia().equals(competencia))
                .mapToLong(GoldenRow::absentDays).sum();
    }

    private String classify(long absentDays, JsonNode policy) {
        if (absentDays >= policy.path("criticalMinDays").asLong()) {
            return "CRITICAL";
        }
        if (absentDays >= policy.path("attentionMinDays").asLong()) {
            return "ATTENTION";
        }
        return "STANDARD";
    }

    private List<Assignment> assignments(JsonNode nodes) {
        List<Assignment> assignments = new ArrayList<>();
        nodes.forEach(node -> assignments.add(new Assignment(
                node.path("employeeId").asInt(),
                node.path("departmentId").asInt(),
                LocalDate.parse(node.path("effectiveFrom").asText()),
                node.path("effectiveTo").isNull() ? null : LocalDate.parse(node.path("effectiveTo").asText())
        )));
        return assignments;
    }

    private List<GoldenRow> expectedRows(JsonNode goldenCase) {
        List<GoldenRow> rows = new ArrayList<>();
        goldenCase.path("expectedRows").forEach(node -> rows.add(new GoldenRow(
                node.path("employeeId").asInt(), node.path("departmentId").asInt(),
                LocalDate.parse(node.path("competencia").asText()), LocalDate.parse(node.path("periodoInicio").asText()),
                LocalDate.parse(node.path("periodoFim").asText()), node.path("diasAfastado").asLong(),
                node.path("criticalityLevel").asText()
        )));
        return rows;
    }

    private List<Comparison> expectedComparison(JsonNode goldenCase) {
        List<Comparison> comparison = new ArrayList<>();
        goldenCase.path("expectedComparison").forEach(node -> comparison.add(new Comparison(
                node.path("departmentId").asInt(), node.path("currentEmployees").asLong(),
                node.path("currentDays").asLong(), node.path("previousEmployees").asLong(), node.path("previousDays").asLong()
        )));
        return comparison;
    }

    private Set<String> expectedDiagnostics(JsonNode goldenCase) {
        Set<String> diagnostics = new HashSet<>();
        goldenCase.path("expectedDiagnostics").forEach(node -> diagnostics.add(node.asText()));
        return diagnostics;
    }

    private LocalDate firstDayOfMonth(LocalDate date) {
        return YearMonth.from(date).atDay(1);
    }

    private JsonNode read(String resource) throws IOException {
        try (InputStream input = AbsenceAnalyticsGoldenContractTest.class.getResourceAsStream(resource)) {
            assertThat(input).as("missing classpath resource %s", resource).isNotNull();
            return objectMapper.readTree(input);
        }
    }

    private record Assignment(int employeeId, int departmentId, LocalDate effectiveFrom, LocalDate effectiveTo) {
        boolean includes(LocalDate day) {
            return !day.isBefore(effectiveFrom) && (effectiveTo == null || day.isBefore(effectiveTo));
        }
    }

    private record AttributedDay(int employeeId, int departmentId, LocalDate competencia, LocalDate day) {
    }

    private record RowKey(int employeeId, int departmentId, LocalDate competencia) {
    }

    private record GoldenRow(int employeeId, int departmentId, LocalDate competencia, LocalDate periodStart,
                             LocalDate periodEnd, long absentDays, String criticalityLevel) {
    }

    private record Comparison(int departmentId, long currentEmployees, long currentDays, long previousEmployees,
                              long previousDays) {
    }

    private record OracleResult(List<GoldenRow> rows, Set<String> diagnostics, List<Comparison> comparison) {
    }
}
