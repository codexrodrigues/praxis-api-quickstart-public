package com.example.praxis.apiquickstart.rulelab;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.praxisplatform.config.dto.DomainRuleExecutionObservationBatchRequest;
import org.praxisplatform.config.dto.DomainRuleExecutionObservationRequest;
import org.praxisplatform.config.service.DomainRuleExecutionObservationService;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;

/** Delivers one leased observation to Config; safe to invoke repeatedly from a governed job. */
public final class RuleExecutionObservationDispatcher {
    private final RuleExecutionObservationOutbox outbox;
    private final DomainRuleExecutionObservationService service;
    private final Clock clock;
    private final String hostActorRef;
    private final int maximumAttempts;
    private final Duration lease;
    private final Duration retryDelay;

    RuleExecutionObservationDispatcher(
            RuleExecutionObservationOutbox outbox,
            DomainRuleExecutionObservationService service,
            Clock clock,
            String hostActorRef,
            int maximumAttempts,
            Duration lease,
            Duration retryDelay) {
        this.outbox = outbox;
        this.service = service;
        this.clock = clock;
        this.hostActorRef = requireText(hostActorRef);
        if (maximumAttempts < 1 || maximumAttempts > 100
                || lease == null || lease.isZero() || lease.isNegative() || lease.compareTo(Duration.ofHours(1)) > 0
                || retryDelay == null || retryDelay.isZero() || retryDelay.isNegative()
                || retryDelay.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("attempts must be 1..100 and lease/retry must be 1ms..1h");
        }
        this.maximumAttempts = maximumAttempts;
        this.lease = lease;
        this.retryDelay = retryDelay;
    }

    public boolean dispatchNext() {
        var claimed = outbox.claim(clock.instant(), lease);
        if (claimed.isEmpty()) return false;
        var item = claimed.get();
        try {
            service.ingest(
                    new DomainRuleExecutionObservationBatchRequest(List.of(
                            new DomainRuleExecutionObservationRequest(
                                    item.observationId(), item.snapshotKey(), item.snapshotContentHash(),
                                    item.activationRevision(), item.outcome(), item.durationMicros(), item.observedAt()))),
                    new DomainRuleGovernancePrincipal(item.tenantId(), hostActorRef, item.environment()));
            outbox.delivered(item.observationId(), item.leaseToken(), clock.instant());
        } catch (RuntimeException failure) {
            outbox.failed(item.observationId(), item.leaseToken(), clock.instant(), maximumAttempts,
                    retryDelay.multipliedBy(Math.min(item.attempt(), 10)), failure.getClass().getSimpleName());
        }
        return true;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("hostActorRef is required");
        return value.trim();
    }
}
