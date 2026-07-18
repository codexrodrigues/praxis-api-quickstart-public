package com.example.praxis.apiquickstart.hr.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Executable oracle for the absence analytics pilot.
 *
 * <p>This test intentionally contains no production endpoint or DTO dependency. It freezes the
 * domain semantics that Metadata Starter, Quickstart, Config and Angular implementations must
 * reproduce. The operational oracle separately proves the materialized G2 projection.</p>
 */
class AbsenceAnalyticsGoldenContractTest {

    private static final String SUITE_RESOURCE =
            "/absence-analytics-lab/absence-analytics-semantic-golden-suite.json";
    private static final String SCHEMA_RESOURCE =
            "/absence-analytics-lab/absence-analytics-semantic-golden-suite.schema.json";
    private static final Set<String> EXPECTED_SCENARIOS = Set.of("ALG-01", "ALG-02", "ALG-03");
    private static final Set<String> EXPECTED_PROFILES = Set.of(
            "HR_ANALYST", "DEPARTMENT_MANAGER", "AGGREGATE_ONLY", "UNAUTHORIZED");
    private static final Set<String> EXPECTED_WIDGETS = Set.of(
            "absence-summary",
            "absence-period-filter",
            "absence-department-comparison-chart",
            "absence-critical-employees-table");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void goldenSuitePreservesTheSemanticContractAndNumericalOracle() throws IOException {
        JsonNode suite = read(SUITE_RESOURCE);
        JsonNode schema = read(SCHEMA_RESOURCE);

        Set<ValidationMessage> violations = JsonSchemaFactory
                .getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(schema)
                .validate(suite);
        assertThat(violations).as("semantic golden suite JSON Schema violations").isEmpty();

        assertContractIdentity(suite, schema);
        assertAiProjectionIsSafe(suite);
        assertAccessProfiles(suite);
        assertExpectedComposition(suite);
        assertResourceIdentityProjection(suite);

        int criticalityThresholdDays = suite.path("semantics")
                .path("criticalityThresholdDays")
                .asInt();
        Set<String> scenarioIds = new HashSet<>();
        for (JsonNode scenario : suite.path("scenarios")) {
            assertThat(scenarioIds.add(scenario.path("id").asText())).isTrue();
            ScenarioResult actual = calculate(scenario, criticalityThresholdDays);
            assertScenario(scenario.path("expected"), actual, scenario.path("id").asText());
        }
        assertThat(scenarioIds).isEqualTo(EXPECTED_SCENARIOS);
    }

    private void assertContractIdentity(JsonNode suite, JsonNode schema) {
        assertThat(suite.path("schemaVersion").asText()).isEqualTo("1.0");
        assertThat(schema.path("properties").path("schemaVersion").path("const").asText())
                .isEqualTo(suite.path("schemaVersion").asText());
        assertThat(schema.path("properties").path("scenarios").path("minItems").asInt()).isEqualTo(3);
        assertThat(schema.path("properties").path("scenarios").path("maxItems").asInt()).isEqualTo(3);

        JsonNode ref = suite.path("labRef");
        assertThat(ref.path("domainKey").asText()).isEqualTo("human-resources");
        assertThat(ref.path("boundedContextKey").asText()).isEqualTo("absence-management");
        assertThat(ref.path("scenarioKey").asText())
                .isEqualTo("department-period-critical-employees");
        assertThat(ref.path("policyId").asText())
                .isEqualTo("hr-absence-criticality-v1");
        assertThat(ref.path("policyVersion").asText()).isEqualTo("2026-07-15");

        JsonNode semantics = suite.path("semantics");
        assertThat(semantics.path("primaryMetric").asText())
                .isEqualTo("DISTINCT_EMPLOYEES_WITH_ABSENCE_OVERLAP");
        assertThat(semantics.path("secondaryMetric").asText())
                .isEqualTo("UNIQUE_CALENDAR_DAYS_ABSENT");
        assertThat(semantics.path("periodStrategy").asText())
                .isEqualTo("CURRENT_CALENDAR_MONTH_VS_PREVIOUS_CALENDAR_MONTH");
        assertThat(semantics.path("dateBoundary").asText()).isEqualTo("INCLUSIVE");
        assertThat(semantics.path("departmentAttribution").asText())
                .isEqualTo("EFFECTIVE_ASSIGNMENT_PER_ABSENCE_DAY");
        assertThat(semantics.path("overlapPolicy").asText()).isEqualTo("UNION_EMPLOYEE_DAYS");
        assertThat(semantics.path("timezone").asText()).isEqualTo("America/Sao_Paulo");
    }

    private void assertAiProjectionIsSafe(JsonNode suite) {
        Set<String> allowed = textValues(suite.path("aiProjection").path("allowedFields"));
        Set<String> forbidden = textValues(suite.path("aiProjection").path("forbiddenFields"));

        assertThat(forbidden).containsExactlyInAnyOrder("tipo", "observacoes");
        assertThat(allowed).doesNotContainAnyElementsOf(forbidden);
        assertThat(allowed).contains(
                "funcionarioId",
                "departamentoId",
                "diasAfastado",
                "criticidadePolicyId",
                "criticidadePolicyVersion");

        for (JsonNode scenario : suite.path("scenarios")) {
            for (JsonNode absence : scenario.path("absences")) {
                Set<String> restrictedFields = new HashSet<>();
                absence.path("restricted").fieldNames().forEachRemaining(restrictedFields::add);
                assertThat(restrictedFields).isSubsetOf(forbidden);
                assertThat(allowed).doesNotContainAnyElementsOf(restrictedFields);
            }
        }
    }

    private void assertAccessProfiles(JsonNode suite) {
        Map<String, JsonNode> profiles = new HashMap<>();
        for (JsonNode profile : suite.path("accessProfiles")) {
            profiles.put(profile.path("id").asText(), profile);
        }
        assertThat(profiles.keySet()).isEqualTo(EXPECTED_PROFILES);

        assertAccess(profiles.get("HR_ANALYST"), true, true, true, "ALL_AUTHORIZED");
        assertAccess(profiles.get("DEPARTMENT_MANAGER"), true, true, true, "OWN_DEPARTMENT");
        assertAccess(profiles.get("AGGREGATE_ONLY"), true, false, false, "AGGREGATES_ONLY");
        assertAccess(profiles.get("UNAUTHORIZED"), false, false, false, "NONE");
    }

    private void assertResourceIdentityProjection(JsonNode suite) {
        Map<String, Integer> employeeResourceIds = new HashMap<>();
        Map<String, Integer> departmentResourceIds = new HashMap<>();
        Set<Integer> uniqueEmployeeResourceIds = new HashSet<>();
        Set<Integer> uniqueDepartmentResourceIds = new HashSet<>();

        for (JsonNode scenario : suite.path("scenarios")) {
            for (JsonNode employee : scenario.path("employees")) {
                String semanticId = employee.path("id").asText();
                int resourceId = employee.path("resourceId").asInt();
                assertThat(resourceId).isPositive();
                Integer known = employeeResourceIds.putIfAbsent(semanticId, resourceId);
                assertThat(known).isIn((Integer) null, resourceId);
                assertThat(uniqueEmployeeResourceIds.add(resourceId) || resourceId == known).isTrue();
            }
            for (JsonNode department : scenario.path("expected").path("departments")) {
                String semanticId = department.path("departmentId").asText();
                int resourceId = department.path("resourceId").asInt();
                assertThat(resourceId).isPositive();
                Integer known = departmentResourceIds.putIfAbsent(semanticId, resourceId);
                assertThat(known).isIn((Integer) null, resourceId);
                assertThat(uniqueDepartmentResourceIds.add(resourceId) || resourceId == known).isTrue();
            }
        }

        assertThat(employeeResourceIds).containsEntry("E-001", 101).containsEntry("E-004", 104);
        assertThat(departmentResourceIds).containsEntry("D-ENG", 201).containsEntry("D-OPS", 202);
        assertThat(uniqueEmployeeResourceIds).doesNotContainAnyElementsOf(uniqueDepartmentResourceIds);
    }

    private void assertAccess(
            JsonNode profile,
            boolean aggregate,
            boolean criticalEmployees,
            boolean surfaceOpen,
            String scope
    ) {
        assertThat(profile.path("aggregate").asBoolean()).isEqualTo(aggregate);
        assertThat(profile.path("criticalEmployees").asBoolean()).isEqualTo(criticalEmployees);
        assertThat(profile.path("surfaceOpen").asBoolean()).isEqualTo(surfaceOpen);
        assertThat(profile.path("scope").asText()).isEqualTo(scope);
    }

    private void assertExpectedComposition(JsonNode suite) {
        JsonNode composition = suite.path("expectedComposition");
        assertThat(textValues(composition.path("widgets"))).isEqualTo(EXPECTED_WIDGETS);
        assertThat(textValues(composition.path("links"))).containsExactlyInAnyOrder(
                "absence-period-filter.period->absence-department-comparison-chart.queryContext",
                "absence-period-filter.period->absence-critical-employees-table.queryContext",
                "absence-department-comparison-chart.crossFilter->absence-critical-employees-table.queryContext",
                "absence-critical-employees-table.rowAction->surface.open");

        JsonNode surface = composition.path("surfaceOpen");
        assertThat(surface.path("resourceKey").asText()).isEqualTo("human-resources.funcionarios");
        assertThat(surface.path("identityField").asText()).isEqualTo("funcionarioId");
        assertThat(surface.path("surfaceId").asText()).isEqualTo("hero-profile");
        assertThat(surface.path("intent").asText()).isEqualTo("employee-360");
    }

    private ScenarioResult calculate(JsonNode scenario, int criticalityThresholdDays) {
        LocalDate now = LocalDate.parse(scenario.path("nowDate").asText());
        DateWindow current = monthWindow(YearMonth.from(now));
        DateWindow previous = monthWindow(YearMonth.from(now).minusMonths(1));
        Map<String, String> employeeNames = new HashMap<>();
        scenario.path("employees").forEach(employee ->
                employeeNames.put(employee.path("id").asText(), employee.path("name").asText()));
        List<Assignment> assignments = readAssignments(scenario.path("assignments"));

        Set<EmployeeDay> currentDays = new HashSet<>();
        Set<EmployeeDay> previousDays = new HashSet<>();
        Set<String> reasonCodes = new LinkedHashSet<>();

        for (JsonNode absence : scenario.path("absences")) {
            String employeeId = absence.path("employeeId").asText();
            LocalDate start = LocalDate.parse(absence.path("start").asText());
            LocalDate end = LocalDate.parse(absence.path("end").asText());
            assertThat(end).as("absence end must not precede start").isAfterOrEqualTo(start);

            for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
                LocalDate effectiveDay = day;
                Set<EmployeeDay> target = current.contains(day)
                        ? currentDays
                        : previous.contains(day) ? previousDays : null;
                if (target == null) {
                    continue;
                }
                List<Assignment> effective = assignments.stream()
                        .filter(assignment -> assignment.employeeId().equals(employeeId))
                        .filter(assignment -> assignment.contains(effectiveDay))
                        .toList();
                if (effective.size() != 1) {
                    reasonCodes.add(effective.isEmpty()
                            ? "DEPARTMENT_ASSIGNMENT_MISSING"
                            : "DEPARTMENT_ASSIGNMENT_AMBIGUOUS");
                    continue;
                }
                Assignment assignment = effective.getFirst();
                target.add(new EmployeeDay(
                        employeeId,
                        effectiveDay,
                        assignment.departmentId(),
                        assignment.departmentName()));
            }
        }

        if (!reasonCodes.isEmpty()) {
            return new ScenarioResult("INCONCLUSIVE", List.copyOf(reasonCodes), List.of(), List.of());
        }

        List<DepartmentResult> departments = calculateDepartments(currentDays, previousDays);
        List<CriticalEmployeeResult> criticalEmployees = calculateCriticalEmployees(
                currentDays,
                employeeNames,
                criticalityThresholdDays);
        return new ScenarioResult("READY", List.of(), departments, criticalEmployees);
    }

    private List<DepartmentResult> calculateDepartments(
            Set<EmployeeDay> currentDays,
            Set<EmployeeDay> previousDays
    ) {
        Map<String, String> names = new HashMap<>();
        currentDays.forEach(day -> names.put(day.departmentId(), day.departmentName()));
        previousDays.forEach(day -> names.put(day.departmentId(), day.departmentName()));

        List<DepartmentResult> result = new ArrayList<>();
        for (Map.Entry<String, String> department : names.entrySet()) {
            Set<String> currentEmployees = employeeIds(currentDays, department.getKey());
            Set<String> previousEmployees = employeeIds(previousDays, department.getKey());
            int currentCount = currentEmployees.size();
            int previousCount = previousEmployees.size();
            int delta = currentCount - previousCount;
            boolean baselineMissing = previousCount == 0;
            Double deltaPercent = baselineMissing
                    ? null
                    : ((double) delta / previousCount) * 100.0;
            result.add(new DepartmentResult(
                    department.getKey(),
                    department.getValue(),
                    currentCount,
                    previousCount,
                    countDays(currentDays, department.getKey()),
                    countDays(previousDays, department.getKey()),
                    delta,
                    deltaPercent,
                    baselineMissing));
        }
        result.sort(Comparator
                .comparingInt(DepartmentResult::currentEmployeeCount)
                .reversed()
                .thenComparing(DepartmentResult::departmentName));
        return List.copyOf(result);
    }

    private List<CriticalEmployeeResult> calculateCriticalEmployees(
            Set<EmployeeDay> currentDays,
            Map<String, String> employeeNames,
            int thresholdDays
    ) {
        Map<String, Set<LocalDate>> daysByEmployee = new HashMap<>();
        Map<String, Set<String>> departmentsByEmployee = new HashMap<>();
        for (EmployeeDay day : currentDays) {
            daysByEmployee.computeIfAbsent(day.employeeId(), ignored -> new HashSet<>()).add(day.day());
            departmentsByEmployee
                    .computeIfAbsent(day.employeeId(), ignored -> new HashSet<>())
                    .add(day.departmentId());
        }

        List<CriticalEmployeeResult> result = new ArrayList<>();
        for (Map.Entry<String, Set<LocalDate>> employee : daysByEmployee.entrySet()) {
            if (employee.getValue().size() < thresholdDays) {
                continue;
            }
            List<String> departmentIds = departmentsByEmployee.get(employee.getKey()).stream()
                    .sorted()
                    .toList();
            result.add(new CriticalEmployeeResult(
                    employee.getKey(),
                    employeeNames.get(employee.getKey()),
                    employee.getValue().size(),
                    departmentIds));
        }
        result.sort(Comparator
                .comparingInt(CriticalEmployeeResult::currentAbsentDays)
                .reversed()
                .thenComparing(CriticalEmployeeResult::employeeName));
        return List.copyOf(result);
    }

    private void assertScenario(JsonNode expected, ScenarioResult actual, String scenarioId) {
        assertThat(actual.status()).as("status for %s", scenarioId)
                .isEqualTo(expected.path("status").asText());
        assertThat(actual.reasonCodes()).as("reason codes for %s", scenarioId)
                .containsExactlyElementsOf(textList(expected.path("reasonCodes")));
        assertThat(actual.departments()).hasSize(expected.path("departments").size());
        assertThat(actual.criticalEmployees()).hasSize(expected.path("criticalEmployees").size());

        for (int index = 0; index < actual.departments().size(); index++) {
            DepartmentResult actualDepartment = actual.departments().get(index);
            JsonNode expectedDepartment = expected.path("departments").get(index);
            assertThat(actualDepartment.departmentId()).isEqualTo(expectedDepartment.path("departmentId").asText());
            assertThat(actualDepartment.departmentName()).isEqualTo(expectedDepartment.path("departmentName").asText());
            assertThat(actualDepartment.currentEmployeeCount()).isEqualTo(expectedDepartment.path("currentEmployeeCount").asInt());
            assertThat(actualDepartment.previousEmployeeCount()).isEqualTo(expectedDepartment.path("previousEmployeeCount").asInt());
            assertThat(actualDepartment.currentAbsentDays()).isEqualTo(expectedDepartment.path("currentAbsentDays").asInt());
            assertThat(actualDepartment.previousAbsentDays()).isEqualTo(expectedDepartment.path("previousAbsentDays").asInt());
            assertThat(actualDepartment.deltaEmployeeCount()).isEqualTo(expectedDepartment.path("deltaEmployeeCount").asInt());
            assertThat(actualDepartment.baselineMissing()).isEqualTo(expectedDepartment.path("baselineMissing").asBoolean());
            if (expectedDepartment.path("deltaPercent").isNull()) {
                assertThat(actualDepartment.deltaPercent()).isNull();
            } else {
                assertThat(actualDepartment.deltaPercent())
                        .isEqualTo(expectedDepartment.path("deltaPercent").asDouble());
            }
        }

        for (int index = 0; index < actual.criticalEmployees().size(); index++) {
            CriticalEmployeeResult actualEmployee = actual.criticalEmployees().get(index);
            JsonNode expectedEmployee = expected.path("criticalEmployees").get(index);
            assertThat(actualEmployee.employeeId()).isEqualTo(expectedEmployee.path("employeeId").asText());
            assertThat(actualEmployee.employeeName()).isEqualTo(expectedEmployee.path("employeeName").asText());
            assertThat(actualEmployee.currentAbsentDays()).isEqualTo(expectedEmployee.path("currentAbsentDays").asInt());
            assertThat(actualEmployee.departmentIds())
                    .containsExactlyElementsOf(textList(expectedEmployee.path("departmentIds")));
        }
    }

    private List<Assignment> readAssignments(JsonNode assignments) {
        List<Assignment> result = new ArrayList<>();
        for (JsonNode assignment : assignments) {
            result.add(new Assignment(
                    assignment.path("employeeId").asText(),
                    assignment.path("departmentId").asText(),
                    assignment.path("departmentName").asText(),
                    LocalDate.parse(assignment.path("validFrom").asText()),
                    assignment.hasNonNull("validTo")
                            ? LocalDate.parse(assignment.path("validTo").asText())
                            : null));
        }
        return List.copyOf(result);
    }

    private Set<String> employeeIds(Set<EmployeeDay> days, String departmentId) {
        Set<String> result = new HashSet<>();
        days.stream()
                .filter(day -> day.departmentId().equals(departmentId))
                .forEach(day -> result.add(day.employeeId()));
        return result;
    }

    private int countDays(Set<EmployeeDay> days, String departmentId) {
        return (int) days.stream().filter(day -> day.departmentId().equals(departmentId)).count();
    }

    private DateWindow monthWindow(YearMonth month) {
        return new DateWindow(month.atDay(1), month.atEndOfMonth());
    }

    private Set<String> textValues(JsonNode array) {
        return new LinkedHashSet<>(textList(array));
    }

    private List<String> textList(JsonNode array) {
        List<String> result = new ArrayList<>();
        array.forEach(value -> result.add(value.asText()));
        return List.copyOf(result);
    }

    private JsonNode read(String resource) throws IOException {
        try (InputStream input = AbsenceAnalyticsGoldenContractTest.class.getResourceAsStream(resource)) {
            assertThat(input).as("missing classpath resource %s", resource).isNotNull();
            return objectMapper.readTree(input);
        }
    }

    private record DateWindow(LocalDate from, LocalDate to) {
        boolean contains(LocalDate date) {
            return !date.isBefore(from) && !date.isAfter(to);
        }
    }

    private record Assignment(
            String employeeId,
            String departmentId,
            String departmentName,
            LocalDate validFrom,
            LocalDate validTo
    ) {
        boolean contains(LocalDate date) {
            return !date.isBefore(validFrom) && (validTo == null || date.isBefore(validTo));
        }
    }

    private record EmployeeDay(
            String employeeId,
            LocalDate day,
            String departmentId,
            String departmentName
    ) {
    }

    private record DepartmentResult(
            String departmentId,
            String departmentName,
            int currentEmployeeCount,
            int previousEmployeeCount,
            int currentAbsentDays,
            int previousAbsentDays,
            int deltaEmployeeCount,
            Double deltaPercent,
            boolean baselineMissing
    ) {
    }

    private record CriticalEmployeeResult(
            String employeeId,
            String employeeName,
            int currentAbsentDays,
            List<String> departmentIds
    ) {
    }

    private record ScenarioResult(
            String status,
            List<String> reasonCodes,
            List<DepartmentResult> departments,
            List<CriticalEmployeeResult> criticalEmployees
    ) {
    }
}
