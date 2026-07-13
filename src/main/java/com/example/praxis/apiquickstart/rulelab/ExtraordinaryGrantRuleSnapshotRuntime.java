package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.praxisplatform.config.dto.DomainRuleSnapshotActivationResponse;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.runtime.PraxisRuleSetEngine;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.snapshot.CompiledRuleSnapshot;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;
import org.praxisplatform.rules.snapshot.RuleSnapshotException;

/**
 * Atomically compiles and activates last-known-good extraordinary-grant snapshots.
 *
 * <p>Candidate compilation happens before the active reference changes. Invalid,
 * incompatible, stale, not-yet-valid or corrupted candidates update diagnostics
 * but never evict the last-known-good plan.</p>
 */
public final class ExtraordinaryGrantRuleSnapshotRuntime {
    static final String RULE_SET_KEY = "extraordinary-grant-eligibility";
    static final String OWNER_SERVICE_KEY = "praxis-api-quickstart";
    static final String HOST_CONTRACT_VERSION = "quickstart/1.0";

    private final PraxisRuleSnapshotCompiler compiler;
    private final PraxisRuleSetEngine engine;
    private final AtomicReference<ActiveSnapshot> active = new AtomicReference<>();
    private final AtomicReference<ExtraordinaryGrantRuleSnapshotStatus> status = new AtomicReference<>(
            new ExtraordinaryGrantRuleSnapshotStatus(
                    false, null, null, null, 0, null, null, "HEAD_NOT_LOADED",
                    "No governed RuleSet snapshot has been loaded"));

    public ExtraordinaryGrantRuleSnapshotRuntime(RuleBindingExecutorRegistry registry) {
        RuleBindingExecutorRegistry trustedRegistry = Objects.requireNonNull(registry, "registry is required");
        this.compiler = new PraxisRuleSnapshotCompiler(trustedRegistry);
        this.engine = new PraxisRuleSetEngine(trustedRegistry);
    }

    /** Validates and atomically activates a newer control-plane head. */
    public synchronized ExtraordinaryGrantRuleSnapshotStatus activate(
            DomainRuleSnapshotActivationResponse candidate,
            String expectedTenantId,
            String expectedEnvironment,
            Instant attemptAtUtc) {
        Objects.requireNonNull(candidate, "candidate is required");
        Instant attemptedAt = Objects.requireNonNull(attemptAtUtc, "attemptAtUtc is required");
        ActiveSnapshot current = active.get();
        if (current != null && current.headEtag().equals(candidate.headEtag())) {
            if (!isEffective(current.compiled().snapshot(), attemptedAt)) {
                return reject(
                        "ACTIVE_SNAPSHOT_NOT_EFFECTIVE",
                        "The active RuleSet snapshot is outside its governed validity interval",
                        attemptedAt);
            }
            return updateHealthyStatus(current, attemptedAt, current.activatedAtUtc());
        }
        if (current != null && candidate.activationRevision() <= current.activationRevision()) {
            return reject("STALE_HEAD", "Control plane returned an older or reordered head", attemptedAt);
        }
        if (candidate.activationRevision() < 1) {
            return reject("INVALID_HEAD_REVISION", "Control-plane activationRevision must be positive", attemptedAt);
        }

        try {
            PublishedRuleSnapshot snapshot = Objects.requireNonNull(candidate.snapshot(), "snapshot is required");
            verifyIdentity(snapshot, expectedTenantId, expectedEnvironment);
            verifyValidity(snapshot, attemptedAt);
            CompiledRuleSnapshot compiled = compiler.compile(snapshot, HOST_CONTRACT_VERSION);
            if (!compiled.snapshotContentHash().equals(candidate.snapshotContentHash())) {
                return reject("SNAPSHOT_HASH_MISMATCH", "Compiled content hash differs from the control-plane hash", attemptedAt);
            }
            ActiveSnapshot activated = new ActiveSnapshot(
                    compiled,
                    requireText(candidate.headEtag(), "headEtag"),
                    candidate.activationRevision(),
                    attemptedAt);
            active.set(activated);
            return updateHealthyStatus(activated, attemptedAt, attemptedAt);
        } catch (RuleSnapshotException exception) {
            return reject(exception.getCode().name(), exception.getMessage(), attemptedAt);
        } catch (RuntimeException exception) {
            return reject("SNAPSHOT_REJECTED", safeMessage(exception), attemptedAt);
        }
    }

    /** Records a safe loader failure while retaining any last-known-good plan. */
    public synchronized ExtraordinaryGrantRuleSnapshotStatus reject(
            String code, String message, Instant attemptAtUtc) {
        Instant attemptedAt = Objects.requireNonNull(attemptAtUtc, "attemptAtUtc is required");
        ActiveSnapshot current = active.get();
        ExtraordinaryGrantRuleSnapshotStatus previous = status.get();
        ExtraordinaryGrantRuleSnapshotStatus rejected = new ExtraordinaryGrantRuleSnapshotStatus(
                current != null && isEffective(current.compiled().snapshot(), attemptedAt),
                current == null ? null : current.compiled().snapshot().snapshotKey(),
                current == null ? null : current.compiled().snapshotContentHash(),
                current == null ? null : current.headEtag(),
                current == null ? 0 : current.activationRevision(),
                attemptedAt,
                previous.lastActivatedAtUtc(),
                requireText(code, "failure code"),
                requireText(message, "failure message"));
        status.set(rejected);
        return rejected;
    }

    /** Evaluates against one atomic last-known-good snapshot reference. */
    public RuleEvaluationResult evaluate(JsonNode facts, Instant nowUtc, ZoneId userTimeZone) {
        ActiveSnapshot selected = active.get();
        if (selected == null) {
            throw new ExtraordinaryGrantRuleSnapshotUnavailableException(
                    "No governed extraordinary-grant RuleSet snapshot is active");
        }
        Instant evaluationInstant = Objects.requireNonNull(nowUtc, "nowUtc is required");
        verifyValidity(selected.compiled().snapshot(), evaluationInstant);
        return engine.evaluate(
                selected.compiled().plan(),
                Objects.requireNonNull(facts, "facts are required"),
                evaluationInstant.toString(),
                Objects.requireNonNull(userTimeZone, "userTimeZone is required").getId());
    }

    public ExtraordinaryGrantRuleSnapshotStatus status() {
        return status.get();
    }

    private void verifyIdentity(
            PublishedRuleSnapshot snapshot, String expectedTenantId, String expectedEnvironment) {
        if (!requireText(expectedTenantId, "expectedTenantId").equals(snapshot.tenantId())
                || !requireText(expectedEnvironment, "expectedEnvironment").equals(snapshot.environment())
                || !OWNER_SERVICE_KEY.equals(snapshot.ownerServiceKey())
                || !RULE_SET_KEY.equals(snapshot.ruleSet().ref().ruleSetKey())) {
            throw new IllegalArgumentException(
                    "Snapshot tenant, environment, owner or RuleSet identity does not match this host boundary");
        }
    }

    private void verifyValidity(PublishedRuleSnapshot snapshot, Instant instant) {
        if (!isEffective(snapshot, instant)) {
            throw new ExtraordinaryGrantRuleSnapshotUnavailableException(
                    "The selected RuleSet snapshot is outside its governed validity interval");
        }
    }

    private boolean isEffective(PublishedRuleSnapshot snapshot, Instant instant) {
        Instant validFrom = Instant.parse(snapshot.validFromUtc());
        Instant validUntil = snapshot.validUntilUtc() == null ? null : Instant.parse(snapshot.validUntilUtc());
        return !instant.isBefore(validFrom) && (validUntil == null || instant.isBefore(validUntil));
    }

    private ExtraordinaryGrantRuleSnapshotStatus updateHealthyStatus(
            ActiveSnapshot selected, Instant attemptedAt, Instant activatedAt) {
        ExtraordinaryGrantRuleSnapshotStatus healthy = new ExtraordinaryGrantRuleSnapshotStatus(
                true,
                selected.compiled().snapshot().snapshotKey(),
                selected.compiled().snapshotContentHash(),
                selected.headEtag(),
                selected.activationRevision(),
                attemptedAt,
                activatedAt,
                null,
                null);
        status.set(healthy);
        return healthy;
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private record ActiveSnapshot(
            CompiledRuleSnapshot compiled,
            String headEtag,
            long activationRevision,
            Instant activatedAtUtc) {
    }
}
