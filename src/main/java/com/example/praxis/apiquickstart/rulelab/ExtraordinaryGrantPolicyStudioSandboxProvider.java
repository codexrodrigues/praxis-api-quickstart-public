package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.praxisplatform.rules.plan.PraxisRulePlanCompiler;
import org.praxisplatform.rules.runtime.PraxisRuleSetEngine;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Adapts the extraordinary-grant last-known-good runtime to the neutral sandbox boundary. */
final class ExtraordinaryGrantPolicyStudioSandboxProvider
        implements PolicyStudioSandboxRuleSetProvider {
    private final ExtraordinaryGrantRuleSnapshotRuntime runtime;
    private final PraxisRulePlanCompiler compiler;
    private final PraxisRuleSetEngine engine;
    private final Set<String> ruleKeys;

    ExtraordinaryGrantPolicyStudioSandboxProvider(
            ExtraordinaryGrantRuleSnapshotRuntime runtime,
            RuleBindingExecutorRegistry registry) {
        this.runtime = Objects.requireNonNull(runtime, "runtime is required");
        RuleBindingExecutorRegistry trustedRegistry = Objects.requireNonNull(registry, "registry is required");
        this.compiler = new PraxisRulePlanCompiler(trustedRegistry);
        this.engine = new PraxisRuleSetEngine(trustedRegistry);
        this.ruleKeys = ExtraordinaryGrantRuleSetComposer
                .governedBindings(ExtraordinaryGrantRuleSetFactory.definition()).stream()
                .map(binding -> binding.bindingKey()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> ruleKeys() {
        return ruleKeys;
    }

    @Override
    public PolicyStudioSandboxRuleSetSession prepare(
            String ruleKey, JsonNode condition, Instant nowUtc) {
        ExtraordinaryGrantRuleSnapshotSession active;
        try {
            active = runtime.captureSnapshot(nowUtc);
        } catch (ExtraordinaryGrantRuleSnapshotUnavailableException unavailable) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No effective active snapshot is available for the workspace RuleSet");
        }
        var baseline = active.compiled().plan().definition();
        var candidate = compiler.compile(
                PolicyStudioSandboxRuleSetMaterializer.withCondition(baseline, ruleKey, condition));
        return new PolicyStudioSandboxRuleSetSession(
                candidate, active.compiled().plan(), engine,
                active.snapshotKey(), active.snapshotContentHash(), active.activationRevision());
    }
}
