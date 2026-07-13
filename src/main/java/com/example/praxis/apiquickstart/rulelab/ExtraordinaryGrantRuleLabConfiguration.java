package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.RoundingMode;
import java.util.List;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleExecutorResult;
import org.praxisplatform.rules.plan.PraxisRulePlanCompiler;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.praxisplatform.rules.runtime.PraxisRuleSetEngine;
import org.praxisplatform.rules.runtime.RuleBindingExecutor;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.runtime.RuleExecutorContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring host wiring for the QL-02 Rule Lab; no engine semantics are redefined here. */
@Configuration(proxyBeanMethods = false)
public class ExtraordinaryGrantRuleLabConfiguration {
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Registers exact, pure host implementations referenced by the pilot RuleSet. */
    @Bean("extraordinaryGrantRuleExecutorRegistry")
    RuleBindingExecutorRegistry extraordinaryGrantRuleExecutorRegistry() {
        return new RuleBindingExecutorRegistry(List.of(
                new ExtraordinaryGrantAmountExecutor(),
                new ExtraordinaryGrantEffectPlanExecutor()));
    }

    /** Compiles the immutable pilot definition once during host bootstrap. */
    @Bean("extraordinaryGrantRuleDecisionPlan")
    RuleDecisionPlan extraordinaryGrantRuleDecisionPlan(
            @Qualifier("extraordinaryGrantRuleExecutorRegistry") RuleBindingExecutorRegistry registry) {
        return new PraxisRulePlanCompiler(registry).compile(ExtraordinaryGrantRuleSetFactory.definition());
    }

    /** Exposes the narrow service-level evaluation boundary used by the pilot tests. */
    @Bean
    ExtraordinaryGrantRuleLabService extraordinaryGrantRuleLabService(
            @Qualifier("extraordinaryGrantRuleExecutorRegistry") RuleBindingExecutorRegistry registry,
            @Qualifier("extraordinaryGrantRuleDecisionPlan") RuleDecisionPlan plan) {
        return new ExtraordinaryGrantRuleLabService(new PraxisRuleSetEngine(registry), plan);
    }

    private static final class ExtraordinaryGrantAmountExecutor implements RuleBindingExecutor {
        @Override
        public String implementationKey() {
            return "benefits:extraordinary-grant-amount";
        }

        @Override
        public String implementationVersion() {
            return "1.0.0";
        }

        @Override
        public RuleExecutorResult evaluate(RuleExecutorContext context) {
            var amount = context.facts()
                    .path("request")
                    .path("requestedAmount")
                    .decimalValue()
                    .setScale(2, RoundingMode.HALF_EVEN);
            ObjectNode output = JSON.createObjectNode();
            output.put("recommendedAmount", amount);
            output.put("currency", "BRL");
            output.put("roundingMode", RoundingMode.HALF_EVEN.name());
            return new RuleExecutorResult(RuleDecision.ALLOW, List.of(), output);
        }
    }

    private static final class ExtraordinaryGrantEffectPlanExecutor implements RuleBindingExecutor {
        @Override
        public String implementationKey() {
            return "benefits:extraordinary-grant-effect-plan";
        }

        @Override
        public String implementationVersion() {
            return "1.0.0";
        }

        @Override
        public RuleExecutorResult evaluate(RuleExecutorContext context) {
            ObjectNode output = JSON.createObjectNode();
            output.put("intentType", "REGISTER_EXTRAORDINARY_GRANT");
            output.put("operationKey", context.ruleSetRef().operationKey());
            output.put("status", "PLANNED_NOT_EXECUTED");
            return new RuleExecutorResult(RuleDecision.ALLOW, List.of(), output);
        }
    }
}
