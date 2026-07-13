package com.example.praxis.apiquickstart.rulelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.dto.DomainRuleSnapshotActivationResponse;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleBindingResult;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleRuntimeCompatibility;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;

class ExtraordinaryGrantRuleLabServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Instant NOW = Instant.parse("2026-07-13T15:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private ExtraordinaryGrantRuleLabService service;

    @BeforeEach
    void startServiceBoundary() {
        var configuration = new ExtraordinaryGrantRuleLabConfiguration();
        var registry = configuration.extraordinaryGrantRuleExecutorRegistry();
        var runtime = new ExtraordinaryGrantRuleSnapshotRuntime(registry);
        PublishedRuleSnapshot snapshot = snapshot();
        String contentHash = new PraxisRuleSnapshotCompiler(registry)
                .compile(snapshot, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION)
                .snapshotContentHash();
        runtime.activate(
                new DomainRuleSnapshotActivationResponse(snapshot, contentHash, "head-1", 1, "ACTIVE"),
                "desenv",
                "local",
                NOW);
        service = new ExtraordinaryGrantRuleLabService(runtime);
    }

    @Test
    void evaluatesTheComplexAllowPathWithoutMutatingFactsOrExecutingEffects() throws Exception {
        ObjectNode facts = eligibleFacts();
        ObjectNode original = facts.deepCopy();

        var result = service.evaluate(facts, NOW, ZONE);

        assertEquals(RuleDecision.ALLOW, result.decision());
        assertEquals(original, facts);
        assertEquals("1.1", result.compatibility().engineContractVersion());
        assertEquals(RuleRuntimeCompatibility.JSON_LOGIC_CORPUS_SHA256, result.compatibility().jsonLogicCorpusSha256());
        assertEquals(2, result.implementationRefs().size());
        assertEquals(11, result.bindingResults().size());
        assertEquals("2500.00", binding(result, "grant.amount-calculation")
                .output().path("recommendedAmount").decimalValue().setScale(2).toPlainString());
        assertEquals("BRL", binding(result, "grant.amount-calculation").output().path("currency").asText());
        assertEquals("PLANNED_NOT_EXECUTED", binding(result, "grant.effect-plan").output().path("status").asText());
    }

    @Test
    void protectedAuthorizationDenyShortCircuitsBeforeBusinessCalculation() throws Exception {
        ObjectNode facts = eligibleFacts();
        facts.withObject("/actor").putArray("permissions").add("benefit:read");

        var result = service.evaluate(facts, NOW, ZONE);

        assertEquals(RuleDecision.DENY, result.decision());
        assertEquals(List.of("REQUEST_NOT_AUTHORIZED"), result.reasonCodes());
        assertEquals(1, result.bindingResults().size());
        assertFalse(result.bindingResults().stream()
                .anyMatch(item -> item.bindingKey().equals("grant.amount-calculation")));
    }

    @Test
    void terminalProgramNotApplicableIsNotLiftedByEarlierGuards() throws Exception {
        ObjectNode facts = eligibleFacts();
        facts.withObject("/program").put("active", false);

        var result = service.evaluate(facts, NOW, ZONE);

        assertEquals(RuleDecision.NOT_APPLICABLE, result.decision());
        assertEquals(List.of("PROGRAM_NOT_APPLICABLE"), binding(result, "program.applicability").reasonCodes());
        assertNull(binding(result, "grant.amount-calculation").output());
        assertNull(binding(result, "grant.effect-plan").output());
    }

    @Test
    void missingCustomerFactRemainsInconclusiveAndBlocksEffectPlanning() throws Exception {
        ObjectNode facts = eligibleFacts();
        facts.withObject("/customer").remove("additionalEligible");

        var result = service.evaluate(facts, NOW, ZONE);

        assertEquals(RuleDecision.INCONCLUSIVE, result.decision());
        assertEquals(List.of("FACT_REQUIRED_MISSING", "PRIOR_DECISION_INCONCLUSIVE"), result.reasonCodes());
        assertNull(binding(result, "grant.effect-plan").output());
    }

    @Test
    void insufficientBudgetDeniesAfterPureCalculationAndBeforeEffectPlan() throws Exception {
        ObjectNode facts = eligibleFacts();
        facts.withObject("/budget").put("availableAmount", 1000.00);

        var result = service.evaluate(facts, NOW, ZONE);

        assertEquals(RuleDecision.DENY, result.decision());
        assertEquals(List.of("BUDGET_INSUFFICIENT"), result.reasonCodes());
        assertEquals("BRL", binding(result, "grant.amount-calculation").output().path("currency").asText());
        assertFalse(result.bindingResults().stream()
                .anyMatch(item -> item.bindingKey().equals("grant.effect-plan")));
    }

    private RuleBindingResult binding(org.praxisplatform.rules.contract.RuleEvaluationResult result, String key) {
        return result.bindingResults().stream()
                .filter(item -> item.bindingKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing binding result: " + key));
    }

    private ObjectNode eligibleFacts() throws Exception {
        return (ObjectNode) JSON.readTree("""
                {
                  "request": {"requestedAmount": 2500.00},
                  "actor": {"permissions": ["benefit:request"]},
                  "worker": {"status": "ACTIVE"},
                  "grant": {"hasDuplicate": false},
                  "program": {"active": true, "maxAmount": 5000.00},
                  "customer": {"additionalEligible": true},
                  "payment": {
                    "requestedDate": "2026-07-20",
                    "allowedDates": ["2026-07-20", "2026-08-05"]
                  },
                  "budget": {"availableAmount": 100000.00}
                }
                """);
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
