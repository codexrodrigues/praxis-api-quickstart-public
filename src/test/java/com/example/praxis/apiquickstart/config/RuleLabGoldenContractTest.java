package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Locks the internal contract fixtures that must be stable before the
 * Quickstart starts implementing the RuleSet laboratory.
 */
class RuleLabGoldenContractTest {

    private static final Set<String> DECISIONS = Set.of(
            "ALLOW", "DENY", "NOT_APPLICABLE", "INCONCLUSIVE", "TECHNICAL_ERROR");
    private static final Set<String> KINDS = Set.of(
            "COMPILATION", "EVALUATION", "RELOAD", "SHADOW", "COMMAND", "OBSERVATION");
    private static final Set<String> TEMPORAL_KINDS = Set.of("EVALUATION", "SHADOW");
    private static final Set<String> EXPECTED_CASES = Set.of(
            "QLG-01", "QLG-02", "QLG-03", "QLG-04", "QLG-05",
            "QLG-06", "QLG-07", "QLG-08", "QLG-09", "QLG-10",
            "QLG-11", "QLG-12", "QLG-13", "QLG-14", "QLG-15");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void goldenSuitePreservesTheContractReadyGate() throws IOException {
        JsonNode suite = read("/rule-lab/rule-lab-golden-suite.json");
        JsonNode schema = read("/rule-lab/rule-lab-golden-suite.schema.json");

        assertThat(suite.path("schemaVersion").asText()).isEqualTo("1.0");
        assertThat(schema.path("properties").path("schemaVersion").path("const").asText())
                .isEqualTo(suite.path("schemaVersion").asText());
        JsonNode caseSchema = schema.path("properties").path("cases");
        assertThat(caseSchema.path("minItems").asInt()).isEqualTo(EXPECTED_CASES.size());
        assertThat(caseSchema.path("maxItems").asInt()).isEqualTo(EXPECTED_CASES.size());
        JsonNode caseProperties = caseSchema.path("items").path("properties");
        assertThat(textValues(caseProperties.path("kind").path("enum"))).isEqualTo(KINDS);
        assertThat(textValues(caseProperties.path("expectedDecision").path("enum"))).isEqualTo(DECISIONS);
        assertRuleSetIdentity(suite.path("ruleSetRef"));

        JsonNode cases = suite.path("cases");
        assertThat(cases.isArray()).isTrue();
        assertThat(cases).hasSize(EXPECTED_CASES.size());

        Set<String> ids = new HashSet<>();
        for (JsonNode golden : cases) {
            String id = golden.path("id").asText();
            String kind = golden.path("kind").asText();
            assertThat(id).isNotBlank();
            assertThat(ids.add(id)).as("duplicate golden id %s", id).isTrue();
            assertThat(kind).isIn(KINDS);
            assertThat(golden.path("expectedOutcome").asText()).isIn("ACCEPTED", "REJECTED");
            assertThat(golden.path("assertions").isArray()).isTrue();
            assertThat(golden.path("assertions").isEmpty()).isFalse();
            assertThat(golden.path("input").isObject()).isTrue();
            assertThat(golden.has("mutationExpected")).isTrue();
            assertThat(golden.path("mutationExpected").asBoolean())
                    .as("QL-01 fixtures must never authorize mutation: %s", id)
                    .isFalse();

            if (golden.has("expectedDecision")) {
                assertThat(golden.path("expectedDecision").asText()).isIn(DECISIONS);
            }
            if (TEMPORAL_KINDS.contains(kind)) {
                assertThat(golden.has("expectedDecision")).isTrue();
                assertThat(golden.path("nowUtc").asText()).isNotBlank();
                assertThat(golden.path("userTimeZone").asText()).isNotBlank();
            }
            if ("REJECTED".equals(golden.path("expectedOutcome").asText())) {
                assertThat(golden.path("expectedReasonCodes").isArray()).isTrue();
                assertThat(golden.path("expectedReasonCodes").isEmpty()).isFalse();
            }
        }
        assertThat(ids).isEqualTo(EXPECTED_CASES);
    }

    private void assertRuleSetIdentity(JsonNode ref) {
        assertThat(ref.path("domainKey").asText()).isEqualTo("workforce-benefits");
        assertThat(ref.path("boundedContextKey").asText()).isEqualTo("extraordinary-assistance");
        assertThat(ref.path("ruleSetKey").asText()).isEqualTo("extraordinary-grant-eligibility");
        assertThat(ref.path("operationKey").asText()).isEqualTo("evaluate-extraordinary-grant");
        assertThat(ref.path("version").asInt()).isPositive();
    }

    private Set<String> textValues(JsonNode array) {
        Set<String> values = new HashSet<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }

    private JsonNode read(String resource) throws IOException {
        try (InputStream input = RuleLabGoldenContractTest.class.getResourceAsStream(resource)) {
            assertThat(input).as("missing classpath resource %s", resource).isNotNull();
            return objectMapper.readTree(input);
        }
    }
}
