package com.example.praxis.apiquickstart.rulelab;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleRolloutService;

/**
 * One-shot rollout discovery seam for a deployment-supervised job.
 *
 * <p>There is intentionally no scheduler, loop or background thread here. The deployment invokes
 * {@link #pollOnce()} according to its own supervised job policy.</p>
 */
public final class RuleCandidatePreloadSupervisor {
    private final DomainRuleRolloutService rollouts;
    private final Function<RuleCandidatePreloadCommand, RuleCandidatePreloadResult> preload;
    private final DomainRuleGovernancePrincipal principal;

    RuleCandidatePreloadSupervisor(
            DomainRuleRolloutService rollouts,
            RuleCandidatePreloader preloader,
            String tenantId,
            String environment,
            String actorRef) {
        this(rollouts, preloader::preload, tenantId, environment, actorRef);
    }

    RuleCandidatePreloadSupervisor(
            DomainRuleRolloutService rollouts,
            Function<RuleCandidatePreloadCommand, RuleCandidatePreloadResult> preload,
            String tenantId,
            String environment,
            String actorRef) {
        this.rollouts = Objects.requireNonNull(rollouts, "rollouts is required");
        this.preload = Objects.requireNonNull(preload, "preload is required");
        this.principal = new DomainRuleGovernancePrincipal(
                requireText(tenantId, "tenantId"), requireText(actorRef, "actorRef"),
                requireText(environment, "environment"));
    }

    public Optional<RuleCandidatePreloadResult> pollOnce() {
        return rollouts.pending(ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY, principal)
                .map(pending -> preload.apply(new RuleCandidatePreloadCommand(
                        pending.rolloutId(), pending.candidateSnapshotKey(),
                        pending.candidateContentHash())));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }
}
