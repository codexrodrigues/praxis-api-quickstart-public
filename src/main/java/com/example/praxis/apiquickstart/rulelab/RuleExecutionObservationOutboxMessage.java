package com.example.praxis.apiquickstart.rulelab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Minimal durable evidence waiting for delivery to the canonical Config control plane. */
@Entity
@Table(name = "rule_execution_observation_outbox")
class RuleExecutionObservationOutboxMessage {
    @Id @Column(name = "observation_id", nullable = false) private UUID observationId;
    @Column(name = "tenant_id", nullable = false, length = 128) private String tenantId;
    @Column(name = "environment", nullable = false, length = 128) private String environment;
    @Column(name = "snapshot_key", nullable = false, length = 128) private String snapshotKey;
    @Column(name = "snapshot_content_hash", nullable = false, length = 64) private String snapshotContentHash;
    @Column(name = "activation_revision", nullable = false) private long activationRevision;
    @Column(name = "outcome", nullable = false, length = 32) private String outcome;
    @Column(name = "duration_micros", nullable = false) private long durationMicros;
    @Column(name = "observed_at", nullable = false) private Instant observedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 32)
    private RuleExecutionObservationOutboxStatus deliveryStatus;
    @Column(name = "delivery_attempts", nullable = false) private int deliveryAttempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "lease_token") private UUID leaseToken;
    @Column(name = "lease_until") private Instant leaseUntil;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "delivered_at") private Instant deliveredAt;
    @Column(name = "last_failure_code", length = 120) private String lastFailureCode;

    protected RuleExecutionObservationOutboxMessage() {}

    RuleExecutionObservationOutboxMessage(
            UUID observationId, String tenantId, String environment, String snapshotKey,
            String snapshotContentHash, long activationRevision, String outcome,
            long durationMicros, Instant observedAt) {
        this.observationId = Objects.requireNonNull(observationId);
        this.tenantId = requireText(tenantId);
        this.environment = requireText(environment);
        this.snapshotKey = requireText(snapshotKey);
        this.snapshotContentHash = requireText(snapshotContentHash);
        if (!this.snapshotContentHash.matches("[A-F0-9]{64}")) {
            throw new IllegalArgumentException("snapshotContentHash must be an uppercase SHA-256 digest");
        }
        if (activationRevision < 1 || durationMicros < 0 || durationMicros > 300_000_000L) {
            throw new IllegalArgumentException("revision or duration is outside the governed limit");
        }
        this.activationRevision = activationRevision;
        this.outcome = requireText(outcome);
        this.durationMicros = durationMicros;
        this.observedAt = Objects.requireNonNull(observedAt);
        this.createdAt = observedAt;
        this.nextAttemptAt = observedAt;
        this.deliveryStatus = RuleExecutionObservationOutboxStatus.PENDING;
    }

    RuleExecutionObservationDelivery claim(Instant now, Duration lease) {
        boolean ready = deliveryStatus == RuleExecutionObservationOutboxStatus.PENDING
                && !nextAttemptAt.isAfter(now);
        boolean expired = deliveryStatus == RuleExecutionObservationOutboxStatus.PROCESSING
                && leaseUntil != null && !leaseUntil.isAfter(now);
        if (!ready && !expired) throw new IllegalStateException("Observation is not dispatchable");
        deliveryStatus = RuleExecutionObservationOutboxStatus.PROCESSING;
        deliveryAttempts++;
        leaseToken = UUID.randomUUID();
        leaseUntil = now.plus(lease);
        return new RuleExecutionObservationDelivery(
                observationId, tenantId, environment, snapshotKey, snapshotContentHash,
                activationRevision, outcome, durationMicros, observedAt, leaseToken, deliveryAttempts);
    }

    void delivered(UUID expectedLease, Instant now) {
        verifyLease(expectedLease);
        deliveryStatus = RuleExecutionObservationOutboxStatus.DELIVERED;
        deliveredAt = now;
        leaseToken = null;
        leaseUntil = null;
        lastFailureCode = null;
    }

    void failed(UUID expectedLease, Instant now, int maximumAttempts, Duration retryDelay, String code) {
        verifyLease(expectedLease);
        lastFailureCode = requireText(code);
        leaseToken = null;
        leaseUntil = null;
        if (deliveryAttempts >= maximumAttempts) {
            deliveryStatus = RuleExecutionObservationOutboxStatus.DEAD_LETTER;
        } else {
            deliveryStatus = RuleExecutionObservationOutboxStatus.PENDING;
            nextAttemptAt = now.plus(retryDelay);
        }
    }

    private void verifyLease(UUID expected) {
        if (deliveryStatus != RuleExecutionObservationOutboxStatus.PROCESSING
                || !Objects.equals(leaseToken, expected)) {
            throw new IllegalStateException("Observation lease is no longer owned");
        }
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("text is required");
        return value.trim();
    }
}
