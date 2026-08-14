package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.praxisplatform.config.dto.DomainRuleCandidateProbeRequest;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleImplementationCatalog;
import org.praxisplatform.config.service.DomainRuleImplementationCatalogFingerprint;
import org.praxisplatform.config.service.DomainRuleImplementationScope;
import org.praxisplatform.config.service.DomainRuleRolloutService;
import org.praxisplatform.config.service.DomainRuleSnapshotService;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleRuntimeCompatibility;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;
import org.praxisplatform.rules.snapshot.RuleSnapshotException;

/**
 * Compiles one governed candidate in an isolated lane and reports compatibility.
 *
 * <p>This component deliberately has no reference to the active runtime. Successful preload only
 * proves that the candidate can be compiled by this host; it cannot evaluate, activate or replace
 * the last-known-good snapshot.</p>
 */
public final class RuleCandidatePreloader {
    private final DomainRuleSnapshotService snapshots;
    private final DomainRuleRolloutService rollouts;
    private final PraxisRuleSnapshotCompiler compiler;
    private final DomainRuleGovernancePrincipal principal;
    private final RuleRuntimeCompatibility compatibility;
    private final String catalogDigest;
    private final Clock clock;

    RuleCandidatePreloader(
            DomainRuleSnapshotService snapshots,
            DomainRuleRolloutService rollouts,
            RuleBindingExecutorRegistry registry,
            DomainRuleImplementationCatalog implementationCatalog,
            ObjectMapper objectMapper,
            Clock clock,
            String tenantId,
            String environment,
            String actorRef) {
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots is required");
        this.rollouts = Objects.requireNonNull(rollouts, "rollouts is required");
        this.compiler = new PraxisRuleSnapshotCompiler(Objects.requireNonNull(registry, "registry is required"));
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.principal = new DomainRuleGovernancePrincipal(
                requireText(tenantId, "tenantId"), requireText(actorRef, "actorRef"),
                requireText(environment, "environment"));
        this.compatibility = RuleRuntimeCompatibility.current();
        var scope = new DomainRuleImplementationScope(
                tenantId, environment, ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY);
        this.catalogDigest = DomainRuleImplementationCatalogFingerprint.sha256(
                Objects.requireNonNull(objectMapper, "objectMapper is required"), scope,
                Objects.requireNonNull(implementationCatalog, "implementationCatalog is required")
                        .allowedImplementations(scope));
    }

    public RuleCandidatePreloadResult preload(RuleCandidatePreloadCommand command) {
        Objects.requireNonNull(command, "command is required");
        Instant observedAt = clock.instant();
        boolean ready = false;
        String failureCode = null;
        try {
            var stored = snapshots.findSnapshot(
                    principal.tenantId(), principal.environment(),
                    requireText(command.candidateSnapshotKey(), "candidateSnapshotKey"))
                    .orElseThrow(() -> new IllegalStateException("CANDIDATE_NOT_FOUND"));
            if (!requireText(command.candidateContentHash(), "candidateContentHash")
                    .equals(stored.snapshotContentHash())) {
                throw new IllegalStateException("CANDIDATE_HASH_MISMATCH");
            }
            PublishedRuleSnapshot snapshot = stored.snapshot();
            verifyHostBoundary(snapshot);
            var compiled = compiler.compile(snapshot, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION);
            if (!stored.snapshotContentHash().equals(compiled.snapshotContentHash())) {
                throw new IllegalStateException("COMPILED_HASH_MISMATCH");
            }
            ready = true;
        } catch (RuleSnapshotException failure) {
            failureCode = failure.getCode().name();
        } catch (RuntimeException failure) {
            failureCode = safeCode(failure);
        }
        var response = rollouts.probe(command.rolloutId(), new DomainRuleCandidateProbeRequest(
                command.candidateSnapshotKey(), command.candidateContentHash(), ready,
                ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION,
                compatibility.engineContractVersion(), compatibility.jsonLogicDialectVersion(),
                compatibility.jsonLogicCorpusSha256(), catalogDigest, failureCode, observedAt), principal);
        return new RuleCandidatePreloadResult(
                command.rolloutId(), ready, response.updated(), failureCode, observedAt);
    }

    private void verifyHostBoundary(PublishedRuleSnapshot snapshot) {
        if (!principal.tenantId().equals(snapshot.tenantId())
                || !principal.environment().equals(snapshot.environment())
                || !ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY.equals(snapshot.ownerServiceKey())
                || !ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY.equals(snapshot.ruleSet().ref().ruleSetKey())) {
            throw new IllegalArgumentException("CANDIDATE_SCOPE_MISMATCH");
        }
    }

    private static String safeCode(RuntimeException failure) {
        String message = failure.getMessage();
        if (message != null && message.matches("[A-Z][A-Z0-9_]{2,63}")) return message;
        return "CANDIDATE_PRELOAD_FAILED";
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }
}
