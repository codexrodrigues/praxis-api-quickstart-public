package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.RuleExecutorRef;
import org.praxisplatform.rules.contract.RuleExecutorType;
import org.praxisplatform.rules.contract.RuleSetDefinition;

/** Replaces one exact governed JSON Logic binding without redefining the host RuleSet graph. */
final class PolicyStudioSandboxRuleSetMaterializer {
    private PolicyStudioSandboxRuleSetMaterializer() {
    }

    static boolean supports(RuleSetDefinition baseline, String ruleKey) {
        return governedBindings(baseline).stream()
                .anyMatch(binding -> binding.bindingKey().equals(ruleKey));
    }

    static RuleSetDefinition withCondition(
            RuleSetDefinition baseline,
            String ruleKey,
            JsonNode condition) {
        Objects.requireNonNull(baseline, "baseline is required");
        if (ruleKey == null || ruleKey.isBlank()) {
            throw new IllegalArgumentException("ruleKey is required");
        }
        if (condition == null || !condition.isObject() || condition.isEmpty()) {
            throw new IllegalArgumentException("condition must be a non-empty JSON Logic object");
        }
        if (!supports(baseline, ruleKey)) {
            throw new IllegalArgumentException("ruleKey is outside the governed host RuleSet");
        }
        List<DecisionBinding> bindings = baseline.bindings().stream().map(binding -> {
            if (!binding.bindingKey().equals(ruleKey)) return binding;
            return new DecisionBinding(
                    binding.bindingKey(), binding.slotKey(), binding.source(), binding.compositionPolicy(),
                    RuleExecutorRef.jsonLogic(condition), binding.dependsOn(), binding.order(),
                    binding.enabled(), binding.falseDecision(), binding.falseReasonCode(),
                    binding.requiredFactPaths());
        }).toList();
        return new RuleSetDefinition(
                baseline.ref(), baseline.availableRoots(), baseline.slots(), bindings,
                baseline.compatibility(), baseline.failPolicy());
    }

    private static List<DecisionBinding> governedBindings(RuleSetDefinition ruleSet) {
        return ruleSet.bindings().stream()
                .filter(binding -> binding.bindingKey().equals(binding.slotKey()))
                .filter(binding -> binding.executor().type() == RuleExecutorType.JSON_LOGIC)
                .toList();
    }
}
