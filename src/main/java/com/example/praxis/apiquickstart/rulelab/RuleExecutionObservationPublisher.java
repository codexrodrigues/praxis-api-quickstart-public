package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.praxisplatform.rules.contract.RuleDecision;

/** Best-effort producer for a durable local outbox; evaluation availability always wins. */
final class RuleExecutionObservationPublisher {
    private final RuleExecutionObservationOutbox outbox;
    private final ExtraordinaryGrantRuleRuntimeTelemetry telemetry;

    RuleExecutionObservationPublisher(
            RuleExecutionObservationOutbox outbox,
            ExtraordinaryGrantRuleRuntimeTelemetry telemetry) {
        this.outbox = outbox;
        this.telemetry = telemetry;
    }

    void publish(
            String tenantId,
            String environment,
            String snapshotKey,
            String snapshotContentHash,
            long activationRevision,
            RuleDecision decision,
            long durationMicros,
            Instant observedAt) {
        var evidence = new RuleExecutionObservationDelivery(
                UUID.randomUUID(), tenantId, environment, snapshotKey,
                snapshotContentHash, activationRevision, decision.name().toUpperCase(Locale.ROOT),
                durationMicros, observedAt, null, 0);
        try {
            outbox.append(evidence);
            telemetry.observationEnqueued("accepted");
        } catch (RuntimeException unavailable) {
            // The control/operational evidence plane must never change the business decision outcome.
            telemetry.observationEnqueued("unavailable");
        }
    }
}
