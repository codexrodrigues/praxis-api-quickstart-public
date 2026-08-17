package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.praxisplatform.rules.contract.DecisionBinding;

class PayrollPolicyStudioProjectionContractTest {
    private static final String PROJECTION =
            "/policy-studio/payroll-reactive-determinations-policy-studio-projection.v1.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void projectionRemainsDerivedFromThePublishedHostRuleSet() throws IOException {
        JsonNode projection = read();
        var definition = PayrollReactiveDeterminationRuleSet.definition(2);

        assertThat(projection.path("kind").asText()).isEqualTo("POLICY_STUDIO_PROJECTION_V1");
        assertThat(projection.path("ruleSetRef").path("ruleSetKey").asText())
                .isEqualTo(definition.ref().ruleSetKey());
        assertThat(projection.path("ruleSetRef").path("hostContractVersion").asText())
                .isEqualTo(PayrollReactiveDeterminationRuleSet.HOST_CONTRACT_VERSION);
        assertThat(textValues(projection.path("ruleSetRef").path("operationKeys"), null))
                .containsExactly(
                        PayrollReactiveDeterminationRuleSet.NET_SALARY_OPERATION,
                        PayrollReactiveDeterminationRuleSet.PAYMENT_DATE_OPERATION);

        JsonNode decisions = projection.path("decisionRefs");
        assertThat(decisions).hasSize(definition.slots().size());
        for (JsonNode decision : decisions) {
            String slotKey = decision.path("decisionKey").asText();
            var slot = definition.slots().stream()
                    .filter(candidate -> candidate.slotKey().equals(slotKey))
                    .findFirst()
                    .orElseThrow();
            List<DecisionBinding> bindings = definition.bindings().stream()
                    .filter(binding -> binding.slotKey().equals(slotKey))
                    .sorted(Comparator.comparingInt(DecisionBinding::order))
                    .toList();
            assertThat(decision.path("stage").asText()).isEqualTo(slot.stage().name());
            assertThat(decision.path("cardinality").asText()).isEqualTo(slot.cardinality().name());
            assertThat(decision.path("overridePolicy").asText()).isEqualTo(slot.overridePolicy().name());
            assertThat(textValues(decision.path("bindingRefs"), "bindingKey"))
                    .containsExactlyElementsOf(bindings.stream().map(DecisionBinding::bindingKey).toList());
            assertThat(integerValues(decision.path("bindingRefs"), "order"))
                    .containsExactlyElementsOf(bindings.stream().map(DecisionBinding::order).toList());
            assertThat(textValues(decision.path("factPaths"), null))
                    .containsExactlyElementsOf(bindings.getFirst().requiredFactPaths());
        }

        for (JsonNode source : projection.path("sourceArtifacts")) {
            Path sourcePath = Path.of(source.path("path").asText());
            assertThat(sourcePath).exists();
            assertThat(sha256(sourcePath)).isEqualTo(source.path("sha256").asText());
        }
    }

    private List<String> textValues(JsonNode array, String field) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(value -> field == null ? value.asText() : value.path(field).asText())
                .toList();
    }

    private List<Integer> integerValues(JsonNode array, String field) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(value -> value.path(field).asInt())
                .toList();
    }

    private String sha256(Path path) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private JsonNode read() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(PROJECTION)) {
            assertThat(input).as("missing classpath resource %s", PROJECTION).isNotNull();
            return objectMapper.readTree(input);
        }
    }
}
