package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.praxisplatform.config.dto.DomainRuleCandidateProbeRequest;
import org.praxisplatform.config.dto.DomainRuleCandidateProbeResponse;
import org.praxisplatform.config.dto.DomainRuleSnapshotStoredResponse;
import org.praxisplatform.config.service.DomainRuleImplementationCatalog;
import org.praxisplatform.config.service.DomainRuleRolloutService;
import org.praxisplatform.config.service.DomainRuleSnapshotService;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleImplementationRef;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;

class RuleCandidatePreloaderTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

    @Test
    void compilesAndReportsCandidateWithoutChangingActiveRuntime() {
        var snapshots = mock(DomainRuleSnapshotService.class);
        var rollouts = mock(DomainRuleRolloutService.class);
        var registry = registry();
        var candidate = snapshot("candidate-v2");
        String hash = new PraxisRuleSnapshotCompiler(registry)
                .compile(candidate, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION)
                .snapshotContentHash();
        when(snapshots.findSnapshot("desenv", "local", "candidate-v2"))
                .thenReturn(Optional.of(new DomainRuleSnapshotStoredResponse(candidate, hash)));
        when(rollouts.probe(eq(UUID.fromString("10000000-0000-0000-0000-000000000001")),
                any(), any())).thenReturn(new DomainRuleCandidateProbeResponse(true, NOW));
        var runtime = new ExtraordinaryGrantRuleSnapshotRuntime(
                registry, new ExtraordinaryGrantRuleRuntimeTelemetry(new SimpleMeterRegistry()));
        var before = runtime.status();
        var preloader = preloader(snapshots, rollouts, registry);

        var result = preloader.preload(new RuleCandidatePreloadCommand(
                UUID.fromString("10000000-0000-0000-0000-000000000001"), "candidate-v2", hash));

        assertThat(result.preloadReady()).isTrue();
        assertThat(result.failureCode()).isNull();
        assertThat(runtime.status()).isEqualTo(before);
        ArgumentCaptor<DomainRuleCandidateProbeRequest> request =
                ArgumentCaptor.forClass(DomainRuleCandidateProbeRequest.class);
        verify(rollouts).probe(eq(result.rolloutId()), request.capture(), any());
        assertThat(request.getValue().preloadReady()).isTrue();
        assertThat(request.getValue().candidateContentHash()).isEqualTo(hash);
    }

    @Test
    void hashMismatchReportsFailClosedAndStillCannotTouchActiveRuntime() {
        var snapshots = mock(DomainRuleSnapshotService.class);
        var rollouts = mock(DomainRuleRolloutService.class);
        var registry = registry();
        var candidate = snapshot("candidate-v2");
        String storedHash = new PraxisRuleSnapshotCompiler(registry)
                .compile(candidate, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION)
                .snapshotContentHash();
        when(snapshots.findSnapshot("desenv", "local", "candidate-v2"))
                .thenReturn(Optional.of(new DomainRuleSnapshotStoredResponse(candidate, storedHash)));
        when(rollouts.probe(any(), any(), any()))
                .thenReturn(new DomainRuleCandidateProbeResponse(true, NOW));
        var preloader = preloader(snapshots, rollouts, registry);

        var result = preloader.preload(new RuleCandidatePreloadCommand(
                UUID.randomUUID(), "candidate-v2", "F".repeat(64)));

        assertThat(result.preloadReady()).isFalse();
        assertThat(result.failureCode()).isEqualTo("CANDIDATE_HASH_MISMATCH");
        ArgumentCaptor<DomainRuleCandidateProbeRequest> request =
                ArgumentCaptor.forClass(DomainRuleCandidateProbeRequest.class);
        verify(rollouts).probe(any(), request.capture(), any());
        assertThat(request.getValue().preloadReady()).isFalse();
    }

    private RuleCandidatePreloader preloader(DomainRuleSnapshotService snapshots,
            DomainRuleRolloutService rollouts, RuleBindingExecutorRegistry registry) {
        DomainRuleImplementationCatalog catalog = scope -> List.of(
                new RuleImplementationRef("customer:extraordinary-grant-additional-eligibility", "1.0.0"),
                new RuleImplementationRef("benefits:extraordinary-grant-amount-transformation", "1.0.0"),
                new RuleImplementationRef("benefits:extraordinary-grant-effect-plan", "1.0.0"));
        return new RuleCandidatePreloader(snapshots, rollouts, registry, catalog,
                new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC),
                "desenv", "local", "service:praxis-api-quickstart");
    }

    private static RuleBindingExecutorRegistry registry() {
        return new ExtraordinaryGrantRuleLabConfiguration().extraordinaryGrantRuleExecutorRegistry();
    }

    private static PublishedRuleSnapshot snapshot(String key) {
        return new PublishedRuleSnapshot(
                PublishedRuleSnapshot.SNAPSHOT_CONTRACT_VERSION, key, "desenv", "local",
                ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY, 2,
                NOW.minusSeconds(60).toString(), null,
                ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION,
                NOW.minusSeconds(60).toString(), null,
                List.of(new RuleSnapshotSource("definition-1", "grant:eligibility", 1, "D".repeat(64))),
                List.of(new RuleSnapshotApproval(
                        "approval-1", "RULE_DEFINITION_APPROVER", "reviewer", NOW.toString(),
                        "E".repeat(64))),
                ExtraordinaryGrantRuleSetFactory.definition());
    }
}
