package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHead;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHeadActivationType;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;

/**
 * Executable, Oracle-free migration-risk golden corpus for Policy Studio development.
 *
 * <p>The synthetic baseline mirrors migration hazards instead of Ergon identities: nullable and
 * missing facts, inclusive limits, overlapping failures, deterministic precedence, create/update
 * command labels, pure engine evaluation and fail-closed outcomes. This test does not compare a
 * candidate with the active snapshot or an Ergon legacy oracle, and its domain semantics are not
 * interchangeable with RN-013.</p>
 */
class PolicyStudioErgonPortableParityCorpusTest {
    private static final String CORPUS =
            "/policy-studio/ergon-portable-parity-corpus.v1.json";
    private static final ObjectMapper JSON = new ObjectMapper();

    private ExtraordinaryGrantRuleLabService service;

    @BeforeEach
    void activateSyntheticBaseline() {
        var configuration = new ExtraordinaryGrantRuleLabConfiguration();
        var registry = configuration.extraordinaryGrantRuleExecutorRegistry();
        var runtime = new ExtraordinaryGrantRuleSnapshotRuntime(
                registry,
                new ExtraordinaryGrantRuleRuntimeTelemetry(
                        new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        PublishedRuleSnapshot snapshot = snapshot();
        String contentHash = new PraxisRuleSnapshotCompiler(registry)
                .compile(snapshot, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION)
                .snapshotContentHash();
        runtime.activate(new PublishedRuleSnapshotHead(
                        snapshot, contentHash, "ergon-portable-head", 1,
                        PublishedRuleSnapshotHeadActivationType.ACTIVE),
                "desenv", "local", Instant.parse("2026-07-13T15:00:00Z"));
        service = new ExtraordinaryGrantRuleLabService(runtime);
    }

    @Test
    void executesTheSyntheticMigrationRiskGoldenAgainstTheNeutralRuleSet() throws Exception {
        JsonNode corpus = readCorpus();
        assertThat(corpus.path("schemaVersion").asText()).isEqualTo("1.0");
        assertThat(corpus.path("referenceAuthority").asText()).isEqualTo("SYNTHETIC_BASELINE");
        assertThat(corpus.path("ruleSetKey").asText())
                .isEqualTo("extraordinary-grant-eligibility");

        Instant now = Instant.parse(corpus.path("frozenNowUtc").asText());
        ZoneId zone = ZoneId.of(corpus.path("userTimeZone").asText());
        ObjectNode baseFacts = requireObject(corpus.path("baseFacts"), "baseFacts");
        Set<String> ids = new HashSet<>();
        Set<String> modes = new HashSet<>();
        Set<String> characteristics = new HashSet<>();

        for (JsonNode fixture : corpus.path("cases")) {
            String id = fixture.path("id").asText();
            assertThat(ids.add(id)).as("duplicate corpus id %s", id).isTrue();
            modes.add(fixture.path("operationMode").asText());
            characteristics.add(fixture.path("characteristic").asText());
            assertThat(fixture.path("mutationExpected").asBoolean())
                    .as("migration-risk fixture must not authorize mutation: %s", id)
                    .isFalse();

            ObjectNode facts = baseFacts.deepCopy();
            merge(facts, requireObject(fixture.path("factsPatch"), id + ".factsPatch"));
            fixture.path("removePaths").forEach(path -> remove(facts, path.asText()));
            ObjectNode before = facts.deepCopy();

            var result = service.evaluate(facts, now, zone);

            assertThat(result.decision().name())
                    .as("decision for %s", id)
                    .isEqualTo(fixture.path("expectedDecision").asText());
            assertThat(result.reasonCodes())
                    .as("reason codes for %s", id)
                    .containsExactlyElementsOf(textValues(fixture.path("expectedReasonCodes")));
            assertThat(facts).as("frozen facts for %s", id).isEqualTo(before);
            assertFirstTerminalBinding(fixture, result, id);
        }

        assertThat(ids).hasSize(14);
        assertThat(modes).containsExactlyInAnyOrder("CREATE", "UPDATE");
        assertThat(characteristics).contains(
                "HAPPY_PATH",
                "UPPER_BOUNDARY_EQUAL",
                "UPPER_BOUNDARY_EXCEEDED",
                "ALLOWED_NULL_REMAINS_EXPLICIT",
                "GAP_REQUIRED_FACT_MISSING",
                "OVERLAP_PROTECTED_GUARD_PRECEDENCE",
                "OVERLAP_PRODUCT_PRECEDENCE");
    }

    private void assertFirstTerminalBinding(
            JsonNode fixture,
            org.praxisplatform.rules.contract.RuleEvaluationResult result,
            String id) {
        JsonNode expected = fixture.path("expectedFirstTerminalBinding");
        var firstTerminal = result.bindingResults().stream()
                .filter(item -> item.decision() != RuleDecision.ALLOW)
                .findFirst();
        if (expected.isNull()) {
            assertThat(firstTerminal).as("terminal binding for %s", id).isEmpty();
        } else {
            assertThat(firstTerminal).as("terminal binding for %s", id).isPresent();
            assertThat(firstTerminal.orElseThrow().bindingKey())
                    .as("first terminal binding for %s", id)
                    .isEqualTo(expected.asText());
        }
    }

    private ObjectNode requireObject(JsonNode node, String label) {
        assertThat(node.isObject()).as("%s must be an object", label).isTrue();
        return (ObjectNode) node;
    }

    private void merge(ObjectNode target, ObjectNode patch) {
        patch.fields().forEachRemaining(entry -> {
            JsonNode current = target.get(entry.getKey());
            if (entry.getValue().isObject() && current != null && current.isObject()) {
                merge((ObjectNode) current, (ObjectNode) entry.getValue());
            } else {
                target.set(entry.getKey(), entry.getValue().deepCopy());
            }
        });
    }

    private void remove(ObjectNode facts, String dotPath) {
        String[] parts = dotPath.split("\\.");
        ObjectNode owner = facts;
        for (int index = 0; index < parts.length - 1; index++) {
            JsonNode next = owner.get(parts[index]);
            assertThat(next).as("missing owner for removal path %s", dotPath).isInstanceOf(ObjectNode.class);
            owner = (ObjectNode) next;
        }
        owner.remove(parts[parts.length - 1]);
    }

    private List<String> textValues(JsonNode array) {
        assertThat(array.isArray()).isTrue();
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }

    private JsonNode readCorpus() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(CORPUS)) {
            assertThat(input).as("missing classpath resource %s", CORPUS).isNotNull();
            return JSON.readTree(input);
        }
    }

    private PublishedRuleSnapshot snapshot() {
        return new PublishedRuleSnapshot(
                PublishedRuleSnapshot.SNAPSHOT_CONTRACT_VERSION,
                "extraordinary-grant-v1",
                "desenv",
                "local",
                ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY,
                1,
                "2026-07-13T14:00:00Z",
                null,
                ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION,
                "2026-07-13T14:00:00Z",
                null,
                List.of(
                        new RuleSnapshotSource("definition-1", "grant:eligibility", 1, "A".repeat(64)),
                        new RuleSnapshotSource("definition-2", "grant:amount", 1, "B".repeat(64))),
                List.of(
                        new RuleSnapshotApproval(
                                "approval-1", "RULE_DEFINITION_APPROVER", "approver-a",
                                "2026-07-13T13:00:00Z", "A".repeat(64)),
                        new RuleSnapshotApproval(
                                "approval-2", "RULE_DEFINITION_APPROVER", "approver-b",
                                "2026-07-13T13:05:00Z", "B".repeat(64))),
                ExtraordinaryGrantRuleSetFactory.definition());
    }
}
