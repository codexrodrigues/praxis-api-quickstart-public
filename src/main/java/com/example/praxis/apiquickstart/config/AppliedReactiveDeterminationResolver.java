package com.example.praxis.apiquickstart.config;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHead;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHeadReader;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHeadScope;
import org.praxisplatform.config.service.AiPrincipalContext;
import org.praxisplatform.config.service.AiPrincipalContextResolver;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves one tenant-scoped governed snapshot aggregate for the payroll pilot.
 *
 * <p>The complete two-step chain is selected from one canonical Config head and pinned to the
 * current HTTP request. A request can therefore never combine net-salary v1 with payment-date v2.
 * Tenant and environment come from the server principal resolver, not identity headers.</p>
 */
@Component
public class AppliedReactiveDeterminationResolver {

    public static final String PAYROLL_DETERMINATION_KEY = PayrollReactiveDeterminationRuleSet.NET_SALARY_KEY;
    public static final String PAYROLL_PAYMENT_DATE_DETERMINATION_KEY =
            PayrollReactiveDeterminationRuleSet.PAYMENT_DATE_KEY;
    private static final String REQUEST_AGGREGATE_ATTRIBUTE =
            AppliedReactiveDeterminationResolver.class.getName() + ".aggregate";

    private final PublishedRuleSnapshotHeadReader headReader;
    private final AiPrincipalContextResolver principalContextResolver;
    private final Clock clock;
    private final Duration maxStaleness;
    private final ReactiveDeterminationLkgTelemetry telemetry;
    private final Map<Scope, CachedAggregate> lastAcceptedHeads = new ConcurrentHashMap<>();

    @Autowired
    public AppliedReactiveDeterminationResolver(
            PublishedRuleSnapshotHeadReader headReader,
            AiPrincipalContextResolver principalContextResolver,
            @Value("${praxis.reactive-determinations.lkg.max-staleness:PT5M}") Duration maxStaleness,
            ReactiveDeterminationLkgTelemetry telemetry
    ) {
        this(headReader, principalContextResolver, maxStaleness, telemetry, Clock.systemUTC());
    }

    AppliedReactiveDeterminationResolver(
            PublishedRuleSnapshotHeadReader headReader,
            AiPrincipalContextResolver principalContextResolver,
            Duration maxStaleness,
            ReactiveDeterminationLkgTelemetry telemetry,
            Clock clock
    ) {
        this.headReader = Objects.requireNonNull(headReader, "headReader is required");
        this.principalContextResolver = Objects.requireNonNull(
                principalContextResolver, "principalContextResolver is required");
        this.maxStaleness = requirePositive(maxStaleness);
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    /** Requires an applied governed selection before the net-salary provider may run. */
    public AppliedSelection requirePayrollSelection() {
        return requireAggregate().netSalary();
    }

    /** Resolves the governed provider selection for the downstream payroll calendar step. */
    public AppliedSelection requirePayrollPaymentDateSelection() {
        return requireAggregate().paymentDate();
    }

    /**
     * Captures the exact payroll plan and activation evidence already admitted by the operational
     * host. Policy Studio uses this read-only handle for candidate-versus-active comparison; it
     * does not select providers, mutate the head, or execute payroll effects.
     */
    public PayrollSandboxSnapshot capturePolicyStudioSandboxSnapshot() {
        PayrollAggregate aggregate = requireAggregate();
        return new PayrollSandboxSnapshot(
                aggregate.activePlan(), aggregate.snapshotKey(), aggregate.snapshotContentHash(),
                aggregate.activationRevision());
    }

    private PayrollAggregate requireAggregate() {
        HttpServletRequest request = currentRequest();
        AiPrincipalContext principal = principalContextResolver.resolve(request, null, null, null);
        Scope scope = new Scope(principal.tenantId(), principal.environment());
        Object pinnedAttribute = request == null
                ? null
                : request.getAttribute(REQUEST_AGGREGATE_ATTRIBUTE);
        if (pinnedAttribute != null && !(pinnedAttribute instanceof PayrollAggregate)) {
            throw unavailable("Request payroll snapshot state is invalid");
        }
        PayrollAggregate requestPinned = (PayrollAggregate) pinnedAttribute;
        if (requestPinned != null) {
            if (!requestPinned.scope().equals(scope)) {
                throw unavailable("Request principal scope changed after payroll snapshot selection");
            }
            return requestPinned;
        }

        Instant now = clock.instant();
        PublishedRuleSnapshotHead candidate;
        try {
            candidate = headReader.findActive(new PublishedRuleSnapshotHeadScope(
                            scope.tenantId(),
                            scope.environment(),
                            PayrollReactiveDeterminationRuleSet.RULE_SET_KEY))
                    .orElseThrow(() -> unavailable("Governed payroll RuleSet head is required"));
        } catch (ResponseStatusException unavailable) {
            telemetry.rejected("missing_head", lastAcceptedHeads.size());
            throw unavailable;
        } catch (RuntimeException unavailableStore) {
            PayrollAggregate lkg = eligibleLkg(scope, now);
            if (lkg == null) {
                telemetry.rejected("lkg_unavailable", lastAcceptedHeads.size());
                throw unavailable("Config snapshot head is unavailable and no eligible last-known-good aggregate exists");
            }
            telemetry.resolved("lkg", lastAcceptedHeads.size());
            if (request != null) request.setAttribute(REQUEST_AGGREGATE_ATTRIBUTE, lkg);
            return lkg;
        }
        VerifiedAggregate verified = verifyCandidate(scope, candidate, now);
        CachedAggregate selected = lastAcceptedHeads.compute(scope, (ignored, current) -> {
            if (current == null) return verified.cachedAt(now);
            if (current.aggregate().headEtag().equals(verified.aggregate().headEtag())) {
                if (current.aggregate().activationRevision() != verified.aggregate().activationRevision()
                        || !current.aggregate().snapshotKey().equals(verified.aggregate().snapshotKey())
                        || !current.aggregate().snapshotContentHash().equals(verified.aggregate().snapshotContentHash())) {
                    throw unavailable("Config returned an internally inconsistent payroll head");
                }
                return verified.cachedAt(now);
            }
            if (verified.aggregate().activationRevision() <= current.aggregate().activationRevision()) {
                throw unavailable("Config returned a stale or reordered payroll head");
            }
            return verified.cachedAt(now);
        });
        telemetry.resolved("fresh", lastAcceptedHeads.size());
        if (request != null) {
            request.setAttribute(REQUEST_AGGREGATE_ATTRIBUTE, selected.aggregate());
        }
        return selected.aggregate();
    }

    private PayrollAggregate eligibleLkg(Scope scope, Instant now) {
        CachedAggregate cached = lastAcceptedHeads.get(scope);
        if (cached == null) return null;
        Instant staleAt = cached.lastControlPlaneSuccessAt().plus(maxStaleness);
        if (!now.isBefore(staleAt) || (cached.validUntil() != null && !now.isBefore(cached.validUntil()))) {
            lastAcceptedHeads.remove(scope, cached);
            return null;
        }
        return cached.aggregate();
    }

    private VerifiedAggregate verifyCandidate(Scope scope, PublishedRuleSnapshotHead head, Instant now) {
        try {
            PublishedRuleSnapshot snapshot = Objects.requireNonNull(head.snapshot(), "snapshot is required");
            if (head.activationRevision() < 1
                    || !scope.tenantId().equals(snapshot.tenantId())
                    || !scope.environment().equals(snapshot.environment())
                    || !PayrollReactiveDeterminationRuleSet.OWNER_SERVICE_KEY.equals(snapshot.ownerServiceKey())
                    || !PayrollReactiveDeterminationRuleSet.HOST_CONTRACT_VERSION.equals(
                            snapshot.requiredHostContractVersion())
                    || !PayrollReactiveDeterminationRuleSet.definition(snapshot.ruleSet().ref().version())
                            .equals(snapshot.ruleSet())) {
                throw new IllegalArgumentException("Snapshot identity or aggregate graph is incompatible");
            }
            Instant validFrom = Instant.parse(snapshot.validFromUtc());
            Instant validUntil = snapshot.validUntilUtc() == null
                    ? null
                    : Instant.parse(snapshot.validUntilUtc());
            if (now.isBefore(validFrom) || (validUntil != null && !now.isBefore(validUntil))) {
                throw new IllegalArgumentException("Snapshot is outside its governed validity interval");
            }
            var compiled = new PraxisRuleSnapshotCompiler(RuleBindingExecutorRegistry.empty())
                    .compile(snapshot, PayrollReactiveDeterminationRuleSet.HOST_CONTRACT_VERSION);
            if (!compiled.snapshotContentHash().equals(head.snapshotContentHash())) {
                throw new IllegalArgumentException("Snapshot content hash does not match the compiled aggregate");
            }
            Map<String, RuleSnapshotSource> sources = snapshot.sources().stream()
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(
                            RuleSnapshotSource::definitionKey,
                            source -> source));
            if (!sources.keySet().equals(java.util.Set.of(
                    PAYROLL_DETERMINATION_KEY,
                    PAYROLL_PAYMENT_DATE_DETERMINATION_KEY))) {
                throw new IllegalArgumentException("Snapshot provenance must exactly cover both determinations");
            }
            return new VerifiedAggregate(new PayrollAggregate(
                    scope,
                    snapshot.snapshotKey(),
                    head.snapshotContentHash(),
                    head.headEtag(),
                    head.activationRevision(),
                    compiled.plan(),
                    selection(sources.get(PAYROLL_DETERMINATION_KEY),
                            PayrollReactiveDeterminationRuleSet.NET_SALARY_OPERATION),
                    selection(sources.get(PAYROLL_PAYMENT_DATE_DETERMINATION_KEY),
                            PayrollReactiveDeterminationRuleSet.PAYMENT_DATE_OPERATION)), validUntil);
        } catch (RuntimeException invalid) {
            telemetry.rejected("invalid_head", lastAcceptedHeads.size());
            throw unavailable("Governed payroll RuleSet head is incompatible with this host");
        }
    }

    ReactiveDeterminationLkgStatus lkgStatus() {
        return telemetry.status();
    }

    private static Duration requirePositive(Duration value) {
        Duration duration = Objects.requireNonNull(value, "maxStaleness is required");
        if (duration.isZero() || duration.isNegative()) throw new IllegalArgumentException("maxStaleness must be positive");
        return duration;
    }

    private AppliedSelection selection(RuleSnapshotSource source, String operationId) {
        return new AppliedSelection(
                source.definitionKey(), source.version(), source.sourceHash(), operationId);
    }

    private ResponseStatusException unavailable(String reason) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                reason + ": " + PayrollReactiveDeterminationRuleSet.RULE_SET_KEY);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    public record AppliedSelection(
            String ruleKey,
            Integer ruleVersion,
            String sourceHash,
            String operationId
    ) {}

    public record PayrollSandboxSnapshot(
            RuleDecisionPlan activePlan,
            String snapshotKey,
            String snapshotContentHash,
            long activationRevision
    ) {}

    private record Scope(String tenantId, String environment) {}

    private record PayrollAggregate(
            Scope scope,
            String snapshotKey,
            String snapshotContentHash,
            String headEtag,
            long activationRevision,
            RuleDecisionPlan activePlan,
            AppliedSelection netSalary,
            AppliedSelection paymentDate
    ) {}

    private record CachedAggregate(
            PayrollAggregate aggregate, Instant lastControlPlaneSuccessAt, Instant validUntil) {}

    private record VerifiedAggregate(PayrollAggregate aggregate, Instant validUntil) {
        CachedAggregate cachedAt(Instant instant) {
            return new CachedAggregate(aggregate, instant, validUntil);
        }
    }
}
