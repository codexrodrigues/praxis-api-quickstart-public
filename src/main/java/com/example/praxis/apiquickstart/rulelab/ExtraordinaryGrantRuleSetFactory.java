package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.praxisplatform.rules.contract.CompositionPolicy;
import org.praxisplatform.rules.contract.DecisionAggregationPolicy;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.DecisionSlot;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.DecisionStage;
import org.praxisplatform.rules.contract.OverridePolicy;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleExecutorRef;
import org.praxisplatform.rules.contract.RuleFailPolicy;
import org.praxisplatform.rules.contract.RuleRuntimeCompatibility;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.contract.RuleSetRef;
import org.praxisplatform.rules.contract.SlotCardinality;

/** Builds the neutral QL-02 authoring/test definition; runtime activation comes only from Config snapshots. */
final class ExtraordinaryGrantRuleSetFactory {
    private static final ObjectMapper JSON = new ObjectMapper();

    private ExtraordinaryGrantRuleSetFactory() {
    }

    /** Returns the reference definition used to publish fixtures and verify the pilot contract. */
    static RuleSetDefinition definition() {
        return definition(1);
    }

    /** Returns the same governed graph under a new immutable publication version. */
    static RuleSetDefinition definition(int version) {
        if (version < 1) {
            throw new IllegalArgumentException("RuleSet version must be positive");
        }
        List<DecisionSlot> slots = List.of(
                single("request.authorization-integrity", DecisionStage.PROTECTED_GUARD, OverridePolicy.FORBIDDEN),
                single("worker.legal-eligibility", DecisionStage.PROTECTED_GUARD, OverridePolicy.FORBIDDEN),
                single("grant.duplicate-conflict", DecisionStage.DOMAIN_DECISION, OverridePolicy.FORBIDDEN),
                single("program.applicability", DecisionStage.DOMAIN_DECISION, OverridePolicy.FORBIDDEN),
                new DecisionSlot(
                        "customer.additional-eligibility",
                        DecisionStage.DOMAIN_DECISION,
                        SlotCardinality.MULTIPLE,
                        OverridePolicy.RESTRICT_ONLY,
                        DecisionAggregationPolicy.DENY_OVERRIDES),
                single("payment.calendar-policy", DecisionStage.DOMAIN_DECISION, OverridePolicy.REPLACEABLE),
                single("grant.amount-parameters", DecisionStage.DOMAIN_DECISION, OverridePolicy.PARAMETERIZABLE),
                single("grant.amount-transformation", DecisionStage.TRANSFORMATION_INTENT, OverridePolicy.FORBIDDEN),
                single("budget.availability", DecisionStage.POST_DECISION, OverridePolicy.RESTRICT_ONLY),
                single("grant.effect-plan", DecisionStage.EFFECT_INTENT, OverridePolicy.FORBIDDEN));

        List<DecisionBinding> bindings = List.of(
                jsonBinding(
                        "request.authorization-integrity",
                        "request.authorization-integrity",
                        DecisionSource.SECURITY,
                        null,
                        """
                        {"and":[
                          {"!==":[{"var":"actor.permissions"},null]},
                          {"in":["benefit:request",{"var":"actor.permissions"}]}
                        ]}
                        """,
                        List.of(),
                        10,
                        RuleDecision.DENY,
                        "REQUEST_NOT_AUTHORIZED",
                        List.of("actor.permissions")),
                jsonBinding(
                        "worker.legal-eligibility",
                        "worker.legal-eligibility",
                        DecisionSource.PRODUCT,
                        null,
                        "{\"===\":[{\"var\":\"worker.status\"},\"ACTIVE\"]}",
                        List.of("request.authorization-integrity"),
                        20,
                        RuleDecision.DENY,
                        "WORKER_NOT_ELIGIBLE",
                        List.of("worker.status")),
                jsonBinding(
                        "grant.duplicate-conflict",
                        "grant.duplicate-conflict",
                        DecisionSource.PRODUCT,
                        null,
                        "{\"===\":[{\"var\":\"grant.hasDuplicate\"},false]}",
                        List.of("worker.legal-eligibility"),
                        30,
                        RuleDecision.DENY,
                        "DUPLICATE_GRANT",
                        List.of("grant.hasDuplicate")),
                jsonBinding(
                        "program.applicability",
                        "program.applicability",
                        DecisionSource.PRODUCT,
                        null,
                        "{\"===\":[{\"var\":\"program.active\"},true]}",
                        List.of("grant.duplicate-conflict"),
                        40,
                        RuleDecision.NOT_APPLICABLE,
                        "PROGRAM_NOT_APPLICABLE",
                        List.of("program.active")),
                jsonBinding(
                        "customer.product-baseline",
                        "customer.additional-eligibility",
                        DecisionSource.PRODUCT,
                        null,
                        "{\"===\":[true,true]}",
                        List.of("program.applicability"),
                        50,
                        RuleDecision.DENY,
                        "PRODUCT_BASELINE_REJECTED",
                        List.of()),
                customerJavaBinding(
                        "customer.additional-eligibility",
                        "customer.additional-eligibility",
                        "customer:extraordinary-grant-additional-eligibility",
                        List.of("program.applicability"),
                        60,
                        List.of("customer.additionalEligible")),
                jsonBinding(
                        "payment.calendar-policy",
                        "payment.calendar-policy",
                        DecisionSource.PRODUCT,
                        null,
                        "{\"in\":[{\"var\":\"payment.requestedDate\"},{\"var\":\"payment.allowedDates\"}]}",
                        List.of("customer.product-baseline", "customer.additional-eligibility"),
                        70,
                        RuleDecision.DENY,
                        "PAYMENT_DATE_NOT_ALLOWED",
                        List.of("payment.requestedDate", "payment.allowedDates")),
                jsonBinding(
                        "grant.amount-parameters",
                        "grant.amount-parameters",
                        DecisionSource.PRODUCT,
                        null,
                        "{\"<=\":[{\"var\":\"request.requestedAmount\"},{\"var\":\"program.maxAmount\"}]}",
                        List.of("payment.calendar-policy"),
                        80,
                        RuleDecision.DENY,
                        "REQUESTED_AMOUNT_EXCEEDS_PROGRAM_LIMIT",
                        List.of("request.requestedAmount", "program.maxAmount")),
                javaBinding(
                        "grant.amount-transformation",
                        "grant.amount-transformation",
                        "benefits:extraordinary-grant-amount-transformation",
                        List.of("budget.availability"),
                        110,
                        List.of("request.requestedAmount")),
                jsonBinding(
                        "budget.availability",
                        "budget.availability",
                        DecisionSource.PRODUCT,
                        null,
                        "{\">=\":[{\"var\":\"budget.availableAmount\"},{\"var\":\"request.requestedAmount\"}]}",
                        List.of("grant.amount-parameters"),
                        100,
                        RuleDecision.DENY,
                        "BUDGET_INSUFFICIENT",
                        List.of("budget.availableAmount", "request.requestedAmount")),
                javaBinding(
                        "grant.effect-plan",
                        "grant.effect-plan",
                        "benefits:extraordinary-grant-effect-plan",
                        List.of("budget.availability", "grant.amount-transformation"),
                        120,
                        List.of()));

        return new RuleSetDefinition(
                new RuleSetRef(
                        "workforce-benefits",
                        "extraordinary-assistance",
                        "extraordinary-grant-eligibility",
                        "evaluate-extraordinary-grant",
                        version),
                List.of("actor", "budget", "customer", "grant", "payment", "program", "request", "worker"),
                slots,
                bindings,
                RuleRuntimeCompatibility.current(),
                RuleFailPolicy.FAIL_CLOSED);
    }

    private static DecisionSlot single(String key, DecisionStage stage, OverridePolicy overridePolicy) {
        return new DecisionSlot(
                key,
                stage,
                SlotCardinality.SINGLE,
                overridePolicy,
                DecisionAggregationPolicy.SINGLE_RESULT);
    }

    private static DecisionBinding jsonBinding(
            String bindingKey,
            String slotKey,
            DecisionSource source,
            CompositionPolicy composition,
            String expression,
            List<String> dependencies,
            int order,
            RuleDecision falseDecision,
            String falseReason,
            List<String> requiredFacts) {
        return new DecisionBinding(
                bindingKey,
                slotKey,
                source,
                composition,
                RuleExecutorRef.jsonLogic(expression(expression)),
                dependencies,
                order,
                true,
                falseDecision,
                falseReason,
                requiredFacts);
    }

    private static DecisionBinding javaBinding(
            String bindingKey,
            String slotKey,
            String implementationKey,
            List<String> dependencies,
            int order,
            List<String> requiredFacts) {
        return new DecisionBinding(
                bindingKey,
                slotKey,
                DecisionSource.PRODUCT,
                null,
                RuleExecutorRef.java(implementationKey, "1.0.0"),
                dependencies,
                order,
                true,
                null,
                null,
                requiredFacts);
    }

    private static DecisionBinding customerJavaBinding(
            String bindingKey,
            String slotKey,
            String implementationKey,
            List<String> dependencies,
            int order,
            List<String> requiredFacts) {
        return new DecisionBinding(
                bindingKey,
                slotKey,
                DecisionSource.CUSTOMER,
                CompositionPolicy.RESTRICT,
                RuleExecutorRef.java(implementationKey, "1.0.0"),
                dependencies,
                order,
                true,
                null,
                null,
                requiredFacts);
    }

    private static JsonNode expression(String source) {
        try {
            return JSON.readTree(source);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid embedded Rule Lab expression", exception);
        }
    }
}
