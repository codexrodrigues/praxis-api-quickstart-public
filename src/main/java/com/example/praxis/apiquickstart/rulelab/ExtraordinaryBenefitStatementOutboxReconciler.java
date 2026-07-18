package com.example.praxis.apiquickstart.rulelab;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Reconciles ambiguous delivery results without holding a database transaction during HTTP I/O. */
@Service
public final class ExtraordinaryBenefitStatementOutboxReconciler {
    private final ExtraordinaryBenefitStatementOutboxLeaseService leaseService;
    private final ObjectProvider<ExtraordinaryBenefitStatementDeliveryProbe> probeProvider;
    private final Clock clock;
    private final ExtraordinaryBenefitStatementOutboxTelemetry telemetry;
    private final int scanLimit;
    private final Duration retryDelay;

    ExtraordinaryBenefitStatementOutboxReconciler(
            ExtraordinaryBenefitStatementOutboxLeaseService leaseService,
            ObjectProvider<ExtraordinaryBenefitStatementDeliveryProbe> probeProvider,
            @Qualifier("extraordinaryGrantRuleClock") Clock clock,
            ExtraordinaryBenefitStatementOutboxTelemetry telemetry,
            @Value("${praxis.rule-lab.outbox.reconciliation-scan-limit:20}") int scanLimit,
            @Value("${praxis.rule-lab.outbox.reconciliation-retry-ms:30000}") long retryMs) {
        if (scanLimit < 1 || scanLimit > 1000
                || retryMs < 1 || retryMs > Duration.ofHours(24).toMillis()) {
            throw new IllegalArgumentException(
                    "reconciliation-scan-limit must be 1..1000 and retry must be 1ms..24h");
        }
        this.leaseService = leaseService;
        this.probeProvider = probeProvider;
        this.clock = clock;
        this.telemetry = telemetry;
        this.scanLimit = scanLimit;
        this.retryDelay = Duration.ofMillis(retryMs);
    }

    public ExtraordinaryBenefitStatementReconciliationResult reconcileNext() {
        var sample = telemetry.start();
        ExtraordinaryBenefitStatementDeliveryProbe probe = probeProvider.getIfAvailable();
        if (probe == null) {
            return result(sample, ExtraordinaryBenefitStatementReconciliationOutcome.NO_PROBE, null);
        }
        var candidates = leaseService.findReconciliationCandidates(clock.instant(), scanLimit);
        if (candidates.isEmpty()) {
            return result(sample, ExtraordinaryBenefitStatementReconciliationOutcome.EMPTY, null);
        }
        boolean probeFailed = false;
        for (var messageId : candidates) {
            try {
                var acknowledgement = probe.findAcknowledgement(messageId);
                if (acknowledgement.isPresent()
                        && leaseService.markExternallyAcknowledged(messageId, clock.instant())) {
                    return result(sample, ExtraordinaryBenefitStatementReconciliationOutcome.RECONCILED, messageId);
                }
                leaseService.scheduleReconciliation(messageId, clock.instant().plus(retryDelay));
            } catch (Exception failure) {
                boolean interrupted = failure instanceof InterruptedException;
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                probeFailed = true;
                leaseService.scheduleReconciliation(messageId, clock.instant().plus(retryDelay));
                if (interrupted) {
                    return result(sample, ExtraordinaryBenefitStatementReconciliationOutcome.PROBE_FAILED, messageId);
                }
            }
        }
        return result(sample, probeFailed
                ? ExtraordinaryBenefitStatementReconciliationOutcome.PROBE_FAILED
                : ExtraordinaryBenefitStatementReconciliationOutcome.NOT_ACKNOWLEDGED,
                null);
    }

    private ExtraordinaryBenefitStatementReconciliationResult result(
            io.micrometer.core.instrument.Timer.Sample sample,
            ExtraordinaryBenefitStatementReconciliationOutcome outcome,
            java.util.UUID messageId) {
        telemetry.reconciliationCompleted(sample, outcome);
        return new ExtraordinaryBenefitStatementReconciliationResult(outcome, messageId);
    }
}
