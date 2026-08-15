package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioOperationalScenarioSelection;
import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioSandboxScenarioResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.contract.DomainRuleTestRunRecordRequest;
import org.praxisplatform.config.dto.DomainRuleTestScenarioResponse;
import org.springframework.web.server.ResponseStatusException;

class ExtraordinaryBenefitOperationalScenarioBindingFactoryTest {
    private static final String DIGEST = "A".repeat(64);
    private static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");
    private final ObjectMapper json = new ObjectMapper();
    private final ExtraordinaryBenefitOperationalScenarioBindingFactory factory =
            new ExtraordinaryBenefitOperationalScenarioBindingFactory();

    @Test
    void mapsExplicitCreateAndUpdateOperationsWithoutAcceptingBusinessCommandsFromTheCaller() throws Exception {
        UUID createId = UUID.randomUUID();
        UUID updateId = UUID.randomUUID();
        var prepared = prepared(
                scenario(createId, "ALLOW", facts("2500.00")),
                scenario(updateId, "DENY", facts("6000.00")));

        var bindings = factory.create(prepared, List.of(
                new PolicyStudioOperationalScenarioSelection(createId, "CREATE"),
                new PolicyStudioOperationalScenarioSelection(updateId, "UPDATE")), "desenv", "local");
        var otherScope = factory.create(prepared, List.of(
                new PolicyStudioOperationalScenarioSelection(createId, "CREATE"),
                new PolicyStudioOperationalScenarioSelection(updateId, "UPDATE")), "another-tenant", "local");

        assertThat(bindings).hasSize(2);
        assertThat(bindings.get(0)).satisfies(binding -> {
            assertThat(binding.operationMode())
                    .isEqualTo(PolicyStudioOperationalEvidenceAdapter.OperationMode.CREATE);
            assertThat(binding.seed().requestReference()).startsWith("policy-studio-proof-");
            assertThat(binding.seed().requestedAmount()).isEqualByComparingTo("2500.00");
            assertThat(binding.update()).isNull();
            assertThat(binding.mutationExpected()).isTrue();
            assertThat(binding.seed().requestReference())
                    .isNotEqualTo(otherScope.get(0).seed().requestReference());
        });
        assertThat(bindings.get(1)).satisfies(binding -> {
            assertThat(binding.operationMode())
                    .isEqualTo(PolicyStudioOperationalEvidenceAdapter.OperationMode.UPDATE);
            assertThat(binding.seed().requestedAmount()).isEqualByComparingTo("2500.00");
            assertThat(binding.update().requestedAmount()).isEqualByComparingTo("6000.00");
            assertThat(binding.mutationExpected()).isFalse();
        });
    }

    @Test
    void failsBeforeOperationalCommandsWhenCandidateAndActiveDoNotBothMatchExpected() throws Exception {
        UUID scenarioId = UUID.randomUUID();
        DomainRuleTestScenarioResponse scenario = scenario(scenarioId, "ALLOW", facts("2500.00"));
        PolicyStudioSandboxScenarioResult mismatch = result(scenarioId, "ALLOW", "ALLOW", "DENY");
        var prepared = new PolicyStudioSandboxService.PolicyStudioSandboxPreparedRun(
                UUID.randomUUID(), record(scenarioId), List.of(scenario), List.of(mismatch));

        assertThatThrownBy(() -> factory.create(
                        prepared,
                        List.of(new PolicyStudioOperationalScenarioSelection(scenarioId, "CREATE")),
                        "desenv", "local"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(failure -> assertThat(((ResponseStatusException) failure).getStatusCode().value())
                        .isEqualTo(422))
                .hasMessageContaining("every governed assertion");
    }

    @Test
    void failsBeforeOperationalCommandsWhenAReasonOrEffectAssertionDoesNotMatch() throws Exception {
        UUID scenarioId = UUID.randomUUID();
        DomainRuleTestScenarioResponse scenario = scenario(scenarioId, "ALLOW", facts("2500.00"));
        PolicyStudioSandboxScenarioResult mismatch = new PolicyStudioSandboxScenarioResult(
                scenarioId, "scenario-" + scenarioId, "ALLOW", "ALLOW", "ALLOW", "MATCH",
                true, true, null, null, null, true, true,
                List.of(), List.of("UNEXPECTED"), List.of(), false, true,
                List.of(), List.of(), List.of(), true, true, DIGEST, DIGEST, DIGEST);
        var prepared = new PolicyStudioSandboxService.PolicyStudioSandboxPreparedRun(
                UUID.randomUUID(), record(scenarioId), List.of(scenario), List.of(mismatch));

        assertThatThrownBy(() -> factory.create(
                        prepared,
                        List.of(new PolicyStudioOperationalScenarioSelection(scenarioId, "CREATE")),
                        "desenv", "local"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(failure -> assertThat(((ResponseStatusException) failure).getStatusCode().value())
                        .isEqualTo(422))
                .hasMessageContaining("every governed assertion");
    }

    @Test
    void failsClosedWhenScenarioFactsDoNotMatchTheVersionedHostFixture() throws Exception {
        UUID scenarioId = UUID.randomUUID();
        JsonNode incompatible = facts("2500.00").deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) incompatible.path("program"))
                .put("maxAmount", 9000.00);
        var prepared = prepared(scenario(scenarioId, "ALLOW", incompatible));

        assertThatThrownBy(() -> factory.create(
                        prepared,
                        List.of(new PolicyStudioOperationalScenarioSelection(scenarioId, "CREATE")),
                        "desenv", "local"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(failure -> assertThat(((ResponseStatusException) failure).getStatusCode().value())
                        .isEqualTo(422))
                .hasMessageContaining("authoritative fixture profile");
    }

    private PolicyStudioSandboxService.PolicyStudioSandboxPreparedRun prepared(
            DomainRuleTestScenarioResponse... scenarios) {
        List<DomainRuleTestScenarioResponse> items = List.of(scenarios);
        return new PolicyStudioSandboxService.PolicyStudioSandboxPreparedRun(
                UUID.randomUUID(),
                record(items.stream().map(DomainRuleTestScenarioResponse::id).toArray(UUID[]::new)),
                items,
                items.stream().map(item -> result(
                        item.id(), item.expectedDecision(), item.expectedDecision(), item.expectedDecision()))
                        .toList());
    }

    private DomainRuleTestRunRecordRequest record(UUID... scenarioIds) {
        return new DomainRuleTestRunRecordRequest(
                "operational-command", 1L, DIGEST, NOW, "America/Sao_Paulo",
                "snapshot", DIGEST, 1L, null,
                java.util.Arrays.stream(scenarioIds).map(id ->
                        new org.praxisplatform.config.contract.DomainRuleTestRunResultRequest(
                                id, "scenario-" + id, "ALLOW", "ALLOW", null, null,
                                List.of(), List.of(), List.of(), List.of(), DIGEST, DIGEST, DIGEST,
                                null, null)).toList());
    }

    private DomainRuleTestScenarioResponse scenario(UUID id, String expected, JsonNode facts) {
        return new DomainRuleTestScenarioResponse(
                id, UUID.randomUUID(), "scenario-" + id, "Scenario", facts, expected,
                null, List.of(), List.of(), "ACTIVE", 1L, UUID.randomUUID().toString(),
                "author", "author", NOW, NOW);
    }

    private PolicyStudioSandboxScenarioResult result(
            UUID id, String expected, String candidate, String active) {
        return new PolicyStudioSandboxScenarioResult(
                id, "scenario-" + id, expected, candidate, active,
                candidate.equals(active) ? "MATCH" : "MISMATCH",
                expected.equals(candidate), expected.equals(active),
                null, null, null, true, true,
                List.of(), List.of(), List.of(), true, true,
                List.of(), List.of(), List.of(), true, true,
                DIGEST, DIGEST, DIGEST);
    }

    private JsonNode facts(String amount) throws Exception {
        return json.readTree("""
                {
                  "actor":{"permissions":["benefit:request"]},
                  "worker":{"status":"ACTIVE"},
                  "grant":{"hasDuplicate":false},
                  "program":{"active":true,"maxAmount":5000.00},
                  "customer":{"additionalEligible":true},
                  "payment":{"requestedDate":"2026-07-20","allowedDates":["2026-07-20","2026-07-27"]},
                  "budget":{"availableAmount":25000.00},
                  "request":{"reasonCode":"FAMILY_HARDSHIP","eventDate":"2026-07-13","requestedAmount":%s}
                }
                """.formatted(amount));
    }
}
