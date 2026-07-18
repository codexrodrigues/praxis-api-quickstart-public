package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Transactional outbox row containing only the minimum statement delivery envelope. */
@Entity
@Table(name = "extraordinary_benefit_statement_outbox")
class ExtraordinaryBenefitStatementOutboxMessage {
    @Id
    @Column(name = "message_id", nullable = false)
    private UUID messageId;
    @Column(name = "operation_id", nullable = false, unique = true)
    private UUID operationId;
    @Column(name = "event_type", nullable = false, length = 160)
    private String eventType;
    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;
    @Column(name = "environment", nullable = false, length = 80)
    private String environment;
    @Column(name = "correlation_id", nullable = false, length = 255)
    private String correlationId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 32)
    private ExtraordinaryBenefitStatementOutboxStatus deliveryStatus;
    @Column(name = "delivery_attempts", nullable = false)
    private int deliveryAttempts;
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;
    @Column(name = "next_reconciliation_at", nullable = false)
    private Instant nextReconciliationAt;
    @Column(name = "lease_token")
    private UUID leaseToken;
    @Column(name = "lease_until")
    private Instant leaseUntil;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    @Column(name = "last_failure_code", length = 120)
    private String lastFailureCode;
    @Column(name = "last_failure_message", length = 1000)
    private String lastFailureMessage;
    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    protected ExtraordinaryBenefitStatementOutboxMessage() {
    }

    ExtraordinaryBenefitStatementOutboxMessage(
            UUID messageId,
            UUID operationId,
            String eventType,
            String tenantId,
            String environment,
            String correlationId,
            JsonNode payload,
            Instant createdAt) {
        this.messageId = Objects.requireNonNull(messageId, "messageId is required");
        this.operationId = Objects.requireNonNull(operationId, "operationId is required");
        this.eventType = requireText(eventType, "eventType");
        this.tenantId = requireText(tenantId, "tenantId");
        this.environment = requireText(environment, "environment");
        this.correlationId = requireText(correlationId, "correlationId");
        this.payload = Objects.requireNonNull(payload, "payload is required").deepCopy();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.nextAttemptAt = createdAt;
        this.nextReconciliationAt = createdAt;
        this.deliveryStatus = ExtraordinaryBenefitStatementOutboxStatus.PENDING;
    }

    ExtraordinaryBenefitStatementOutboxClaim claim(Instant nowUtc, Duration leaseDuration) {
        Objects.requireNonNull(nowUtc, "nowUtc is required");
        Objects.requireNonNull(leaseDuration, "leaseDuration is required");
        boolean pending = deliveryStatus == ExtraordinaryBenefitStatementOutboxStatus.PENDING
                && !nextAttemptAt.isAfter(nowUtc);
        boolean expiredLease = deliveryStatus == ExtraordinaryBenefitStatementOutboxStatus.PROCESSING
                && leaseUntil != null && !leaseUntil.isAfter(nowUtc);
        if (!pending && !expiredLease) {
            throw new IllegalStateException("Outbox message is not dispatchable");
        }
        deliveryStatus = ExtraordinaryBenefitStatementOutboxStatus.PROCESSING;
        deliveryAttempts++;
        leaseToken = UUID.randomUUID();
        leaseUntil = nowUtc.plus(leaseDuration);
        return new ExtraordinaryBenefitStatementOutboxClaim(
                new ExtraordinaryBenefitStatementOutboxDelivery(
                        messageId, operationId, eventType, tenantId, environment, correlationId,
                        payload.deepCopy(), deliveryAttempts),
                leaseToken);
    }

    void markDelivered(UUID expectedLeaseToken, Instant deliveredAtUtc) {
        verifyLease(expectedLeaseToken);
        deliveryStatus = ExtraordinaryBenefitStatementOutboxStatus.DELIVERED;
        deliveredAt = Objects.requireNonNull(deliveredAtUtc, "deliveredAtUtc is required");
        leaseToken = null;
        leaseUntil = null;
        lastFailureCode = null;
        lastFailureMessage = null;
        lastFailureAt = null;
    }

    ExtraordinaryBenefitStatementOutboxStatus markFailed(
            UUID expectedLeaseToken,
            Instant failedAtUtc,
            int maximumAttempts,
            Duration retryDelay,
            boolean terminalFailure,
            String failureCode,
            String failureMessage) {
        verifyLease(expectedLeaseToken);
        Instant failureInstant = Objects.requireNonNull(failedAtUtc, "failedAtUtc is required");
        if (maximumAttempts < 1) {
            throw new IllegalArgumentException("maximumAttempts must be positive");
        }
        lastFailureCode = truncate(requireText(failureCode, "failureCode"), 120);
        lastFailureMessage = truncate(
                failureMessage == null || failureMessage.isBlank() ? "External delivery failed" : failureMessage,
                1000);
        lastFailureAt = failureInstant;
        leaseToken = null;
        leaseUntil = null;
        if (terminalFailure || deliveryAttempts >= maximumAttempts) {
            deliveryStatus = ExtraordinaryBenefitStatementOutboxStatus.DEAD_LETTER;
        } else {
            deliveryStatus = ExtraordinaryBenefitStatementOutboxStatus.PENDING;
            nextAttemptAt = failureInstant.plus(Objects.requireNonNull(retryDelay, "retryDelay is required"));
        }
        return deliveryStatus;
    }

    boolean markExternallyAcknowledged(Instant reconciledAtUtc) {
        Instant nowUtc = Objects.requireNonNull(reconciledAtUtc, "reconciledAtUtc is required");
        if (deliveryStatus == ExtraordinaryBenefitStatementOutboxStatus.DELIVERED) {
            return false;
        }
        if (deliveryStatus == ExtraordinaryBenefitStatementOutboxStatus.PROCESSING
                && leaseUntil != null && leaseUntil.isAfter(nowUtc)) {
            return false;
        }
        deliveryStatus = ExtraordinaryBenefitStatementOutboxStatus.DELIVERED;
        deliveredAt = nowUtc;
        leaseToken = null;
        leaseUntil = null;
        lastFailureCode = null;
        lastFailureMessage = null;
        lastFailureAt = null;
        return true;
    }

    void scheduleReconciliation(Instant nextReconciliationAtUtc) {
        if (deliveryStatus != ExtraordinaryBenefitStatementOutboxStatus.DELIVERED) {
            nextReconciliationAt = Objects.requireNonNull(
                    nextReconciliationAtUtc, "nextReconciliationAtUtc is required");
        }
    }

    UUID messageId() {
        return messageId;
    }

    ExtraordinaryBenefitStatementReplayCandidate replayCandidate() {
        return new ExtraordinaryBenefitStatementReplayCandidate(
                messageId, deliveryStatus, lastFailureCode, lastFailureAt);
    }

    void scheduleGovernedReplay(String expectedFailureCode, Instant replayAtUtc) {
        if (deliveryStatus != ExtraordinaryBenefitStatementOutboxStatus.DEAD_LETTER) {
            throw new IllegalStateException("Only a dead-letter message can be replayed");
        }
        if (!Objects.equals(lastFailureCode, expectedFailureCode)) {
            throw new IllegalStateException("Outbox failure changed while replay was being authorized");
        }
        deliveryStatus = ExtraordinaryBenefitStatementOutboxStatus.PENDING;
        deliveryAttempts = 0;
        nextAttemptAt = Objects.requireNonNull(replayAtUtc, "replayAtUtc is required");
        nextReconciliationAt = replayAtUtc;
        leaseToken = null;
        leaseUntil = null;
        deliveredAt = null;
        lastFailureCode = null;
        lastFailureMessage = null;
        lastFailureAt = null;
    }

    private void verifyLease(UUID expectedLeaseToken) {
        if (deliveryStatus != ExtraordinaryBenefitStatementOutboxStatus.PROCESSING
                || leaseToken == null
                || !leaseToken.equals(expectedLeaseToken)) {
            throw new IllegalStateException("Outbox delivery lease is no longer owned by this dispatcher");
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String truncate(String value, int maximumLength) {
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }
}
