package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.config.AppliedReactiveDeterminationResolver;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.praxisplatform.rules.plan.PraxisRulePlanCompiler;
import org.praxisplatform.rules.runtime.PraxisRuleSetEngine;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;

/** Uses the same tenant-scoped payroll head admitted by the operational determination host. */
final class PayrollPolicyStudioSandboxProvider implements PolicyStudioSandboxRuleSetProvider {
    private static final Set<String> RULE_KEYS = Set.of(
            AppliedReactiveDeterminationResolver.PAYROLL_DETERMINATION_KEY,
            AppliedReactiveDeterminationResolver.PAYROLL_PAYMENT_DATE_DETERMINATION_KEY);

    private final AppliedReactiveDeterminationResolver resolver;
    private final PraxisRulePlanCompiler compiler;
    private final PraxisRuleSetEngine engine;

    PayrollPolicyStudioSandboxProvider(AppliedReactiveDeterminationResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver is required");
        RuleBindingExecutorRegistry registry = RuleBindingExecutorRegistry.empty();
        this.compiler = new PraxisRulePlanCompiler(registry);
        this.engine = new PraxisRuleSetEngine(registry);
    }

    @Override
    public Set<String> ruleKeys() {
        return RULE_KEYS;
    }

    @Override
    public PolicyStudioSandboxRuleSetSession prepare(
            String ruleKey, JsonNode condition, Instant nowUtc) {
        var active = resolver.capturePolicyStudioSandboxSnapshot();
        var candidate = compiler.compile(PolicyStudioSandboxRuleSetMaterializer.withCondition(
                active.activePlan().definition(), ruleKey, condition));
        return new PolicyStudioSandboxRuleSetSession(
                candidate, active.activePlan(), engine,
                active.snapshotKey(), active.snapshotContentHash(), active.activationRevision());
    }
}
