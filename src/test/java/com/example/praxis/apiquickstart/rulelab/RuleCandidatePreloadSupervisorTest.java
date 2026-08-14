package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.dto.DomainRulePendingRolloutResponse;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleRolloutService;

class RuleCandidatePreloadSupervisorTest {
    @Test
    void discoversAndPreloadsExactlyOneServerScopedCandidate() {
        var rollouts = mock(DomainRuleRolloutService.class);
        @SuppressWarnings("unchecked")
        Function<RuleCandidatePreloadCommand, RuleCandidatePreloadResult> preload = mock(Function.class);
        UUID id = UUID.randomUUID();
        String hash = "A".repeat(64);
        var pending = new DomainRulePendingRolloutResponse(
                id, ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY,
                "candidate-v2", hash, "PREPARING", null);
        when(rollouts.pending(eq(ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY), any()))
                .thenReturn(Optional.of(pending));
        var expected = new RuleCandidatePreloadResult(id, true, true, null, Instant.now());
        when(preload.apply(any())).thenReturn(expected);
        var supervisor = new RuleCandidatePreloadSupervisor(
                rollouts, preload, "desenv", "local", "service:host-a");

        assertThat(supervisor.pollOnce()).contains(expected);
        verify(preload).apply(new RuleCandidatePreloadCommand(id, "candidate-v2", hash));
        verify(rollouts).pending(eq(ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY),
                eq(new DomainRuleGovernancePrincipal("desenv", "service:host-a", "local")));
    }

    @Test
    void noPendingRolloutPerformsNoPreloadWork() {
        var rollouts = mock(DomainRuleRolloutService.class);
        @SuppressWarnings("unchecked")
        Function<RuleCandidatePreloadCommand, RuleCandidatePreloadResult> preload = mock(Function.class);
        when(rollouts.pending(any(), any())).thenReturn(Optional.empty());
        var supervisor = new RuleCandidatePreloadSupervisor(
                rollouts, preload, "desenv", "local", "service:host-a");

        assertThat(supervisor.pollOnce()).isEmpty();
        verifyNoInteractions(preload);
    }
}
