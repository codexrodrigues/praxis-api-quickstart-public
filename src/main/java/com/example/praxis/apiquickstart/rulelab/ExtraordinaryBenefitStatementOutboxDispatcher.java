package com.example.praxis.apiquickstart.rulelab;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Explicit at-least-once dispatcher with durable leases, bounded retries and dead-letter state.
 *
 * <p>No scheduler is enabled by this proof. A deployment must supply an idempotent event sink and
 * invoke this worker through its governed job runtime.</p>
 */
@Service
public final class ExtraordinaryBenefitStatementOutboxDispatcher {
    private final ExtraordinaryBenefitStatementOutboxLeaseService leaseService;
    private final ObjectProvider<ExtraordinaryBenefitStatementEventSink> sinkProvider;
    private final Clock clock;
    private final ExtraordinaryBenefitStatementOutboxTelemetry telemetry;
    private final int maximumAttempts;
    private final Duration leaseDuration;
    private final Duration retryBaseDelay;

    ExtraordinaryBenefitStatementOutboxDispatcher(
            ExtraordinaryBenefitStatementOutboxLeaseService leaseService,
            ObjectProvider<ExtraordinaryBenefitStatementEventSink> sinkProvider,
            @Qualifier("extraordinaryGrantRuleClock") Clock clock,
            ExtraordinaryBenefitStatementOutboxTelemetry telemetry,
            @Value("${praxis.rule-lab.outbox.maximum-attempts:5}") int maximumAttempts,
            @Value("${praxis.rule-lab.outbox.lease-ms:30000}") long leaseMs,
            @Value("${praxis.rule-lab.outbox.retry-base-ms:1000}") long retryBaseMs) {
        if (maximumAttempts < 1 || maximumAttempts > 100
                || leaseMs < 1 || leaseMs > Duration.ofHours(1).toMillis()
                || retryBaseMs < 1 || retryBaseMs > Duration.ofHours(1).toMillis()) {
            throw new IllegalArgumentException(
                    "Outbox attempts must be 1..100 and lease/retry delay must be 1ms..1h");
        }
        this.leaseService = leaseService;
        this.sinkProvider = sinkProvider;
        this.clock = clock;
        this.telemetry = telemetry;
        this.maximumAttempts = maximumAttempts;
        this.leaseDuration = Duration.ofMillis(leaseMs);
        this.retryBaseDelay = Duration.ofMillis(retryBaseMs);
    }

    public ExtraordinaryBenefitStatementDispatchResult dispatchNext() {
        var sample = telemetry.start();
        ExtraordinaryBenefitStatementEventSink sink = sinkProvider.getIfAvailable();
        if (sink == null) {
            return result(sample, ExtraordinaryBenefitStatementDispatchOutcome.NO_SINK, null, 0);
        }
        var delivery = leaseService.claimNext(clock.instant(), leaseDuration);
        if (delivery.isEmpty()) {
            return result(sample, ExtraordinaryBenefitStatementDispatchOutcome.EMPTY, null, 0);
        }
        var claim = delivery.get();
        var claimed = claim.delivery();
        try {
            sink.deliver(claimed);
            leaseService.markDelivered(claimed.messageId(), claim.leaseToken(), clock.instant());
            return result(sample, ExtraordinaryBenefitStatementDispatchOutcome.DELIVERED,
                    claimed.messageId(), claimed.attempt());
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            var classifiedFailure = failure instanceof ExtraordinaryBenefitStatementDeliveryFailure deliveryFailure
                    ? deliveryFailure
                    : null;
            var status = leaseService.markFailed(
                    claimed.messageId(), claim.leaseToken(), clock.instant(), maximumAttempts,
                    retryDelay(claimed.attempt()),
                    classifiedFailure != null && classifiedFailure.permanent(),
                    classifiedFailure == null ? "EVENT_SINK_FAILURE" : classifiedFailure.failureCode(),
                    safeMessage(failure));
            var outcome = status == ExtraordinaryBenefitStatementOutboxStatus.DEAD_LETTER
                    ? ExtraordinaryBenefitStatementDispatchOutcome.DEAD_LETTERED
                    : ExtraordinaryBenefitStatementDispatchOutcome.RETRY_SCHEDULED;
            return result(sample, outcome, claimed.messageId(), claimed.attempt());
        }
    }

    private ExtraordinaryBenefitStatementDispatchResult result(
            io.micrometer.core.instrument.Timer.Sample sample,
            ExtraordinaryBenefitStatementDispatchOutcome outcome,
            java.util.UUID messageId,
            int attempt) {
        telemetry.dispatchCompleted(sample, outcome);
        return new ExtraordinaryBenefitStatementDispatchResult(outcome, messageId, attempt);
    }

    private Duration retryDelay(int attempt) {
        int exponent = Math.min(Math.max(attempt - 1, 0), 10);
        long multiplier = 1L << exponent;
        long maximumMillis = Duration.ofHours(1).toMillis();
        long baseMillis = retryBaseDelay.toMillis();
        long millis = baseMillis > maximumMillis / multiplier
                ? maximumMillis
                : Math.min(baseMillis * multiplier, maximumMillis);
        return Duration.ofMillis(millis);
    }

    private String safeMessage(Exception failure) {
        return failure.getClass().getSimpleName();
    }
}
