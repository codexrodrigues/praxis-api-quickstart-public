package com.example.praxis.apiquickstart.rulelab;

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
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.praxisplatform.rules.contract.DecisionBinding;

class PolicyStudioProjectionContractTest {

    private static final String PROJECTION =
            "/policy-studio/extraordinary-benefit-policy-studio-projection.v1.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void projectionRemainsADerivedAndCompleteViewOfTheReferenceRuleSet() throws IOException {
        JsonNode projection = read(PROJECTION);
        var definition = ExtraordinaryGrantRuleSetFactory.definition();

        assertThat(projection.path("kind").asText()).isEqualTo("POLICY_STUDIO_PROJECTION_V1");
        assertThat(projection.path("ruleSetRef").path("domainKey").asText())
                .isEqualTo(definition.ref().domainKey());
        assertThat(projection.path("ruleSetRef").path("boundedContextKey").asText())
                .isEqualTo(definition.ref().boundedContextKey());
        assertThat(projection.path("ruleSetRef").path("ruleSetKey").asText())
                .isEqualTo(definition.ref().ruleSetKey());
        assertThat(projection.path("ruleSetRef").path("operationKeys").get(0).asText())
                .isEqualTo(definition.ref().operationKey());

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
            assertThat(decision.path("bindingRefs")).hasSize(bindings.size());
            assertThat(textValues(decision.path("bindingRefs"), "bindingKey"))
                    .containsExactlyElementsOf(bindings.stream().map(DecisionBinding::bindingKey).toList());
            assertThat(integerValues(decision.path("bindingRefs"), "order"))
                    .containsExactlyElementsOf(bindings.stream().map(DecisionBinding::order).toList());
            assertThat(textValues(decision.path("bindingRefs"), "source"))
                    .containsExactlyElementsOf(bindings.stream().map(binding -> binding.source().name()).toList());
            assertThat(textValues(decision.path("bindingRefs"), "executorType"))
                    .containsExactlyElementsOf(bindings.stream().map(binding -> binding.executor().type().name()).toList());

            LinkedHashSet<String> requiredFacts = new LinkedHashSet<>();
            bindings.forEach(binding -> requiredFacts.addAll(binding.requiredFactPaths()));
            assertThat(textValues(decision.path("factPaths"), null)).containsExactlyElementsOf(requiredFacts);
        }
    }

    @Test
    void projectionSourceDigestsAndGoldenEvidenceCannotDriftSilently() throws IOException {
        JsonNode projection = read(PROJECTION);
        for (JsonNode source : projection.path("sourceArtifacts")) {
            Path sourcePath = Path.of(source.path("path").asText());
            assertThat(sourcePath).exists();
            assertThat(sha256(sourcePath)).isEqualTo(source.path("sha256").asText());
        }

        JsonNode goldenSuite = objectMapper.readTree(Path.of(
                projection.path("testEvidence").path("suitePath").asText()).toFile());
        assertThat(goldenSuite.path("cases"))
                .hasSize(projection.path("testEvidence").path("caseCount").asInt());
        assertThat(textValues(projection.path("testEvidence").path("outcomes"), null))
                .containsExactlyInAnyOrder("ALLOW", "DENY", "NOT_APPLICABLE", "INCONCLUSIVE", "TECHNICAL_ERROR");
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

    private JsonNode read(String resource) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            assertThat(input).as("missing classpath resource %s", resource).isNotNull();
            return objectMapper.readTree(input);
        }
    }
}
