package com.example.praxis.apiquickstart.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.praxis.apiquickstart.rulelab.ExtraordinaryGrantRuleLabConfiguration;
import com.example.praxis.apiquickstart.rulelab.ExtraordinaryGrantRuleLabService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.rules.contract.RuleBindingResult;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleRuntimeCompatibility;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ExtraordinaryGrantRuleLabServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Instant NOW = Instant.parse("2026-07-13T15:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private AnnotationConfigApplicationContext context;
    private ExtraordinaryGrantRuleLabService service;

    @BeforeEach
    void startServiceBoundary() {
        context = new AnnotationConfigApplicationContext(ExtraordinaryGrantRuleLabConfiguration.class);
        service = context.getBean(ExtraordinaryGrantRuleLabService.class);
    }

    @AfterEach
    void closeServiceBoundary() {
        context.close();
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
}
