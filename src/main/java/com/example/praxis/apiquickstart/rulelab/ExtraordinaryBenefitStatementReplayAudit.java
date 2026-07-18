package com.example.praxis.apiquickstart.rulelab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Append-only, payload-free evidence of one replay authorization decision. */
@Entity
@Immutable
@Table(name = "extraordinary_benefit_statement_replay_audit")
class ExtraordinaryBenefitStatementReplayAudit {
    @Id
    @Column(name = "audit_id", nullable = false)
    private UUID auditId;
    @Column(name = "message_id", nullable = false)
    private UUID messageId;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "actor_subject", nullable = false, length = 255)
    private String actorSubject;
    @Column(name = "justification", nullable = false, length = 1000)
    private String justification;
    @Column(name = "correlation_id", nullable = false, length = 255)
    private String correlationId;
    @Column(name = "expected_failure_code", length = 120)
    private String expectedFailureCode;
    @Column(name = "observed_failure_code", length = 120)
    private String observedFailureCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "replay_outcome", nullable = false, length = 80)
    private ExtraordinaryBenefitStatementReplayOutcome replayOutcome;
    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    protected ExtraordinaryBenefitStatementReplayAudit() {
    }

    ExtraordinaryBenefitStatementReplayAudit(
            UUID auditId,
            ExtraordinaryBenefitStatementReplayCommand command,
            Instant requestedAt,
            String observedFailureCode,
            ExtraordinaryBenefitStatementReplayOutcome replayOutcome,
            Instant acknowledgedAt) {
        this.auditId = auditId;
        this.messageId = command.messageId();
        this.requestedAt = requestedAt;
        this.actorSubject = command.actorSubject();
        this.justification = command.justification();
        this.correlationId = command.correlationId();
        this.expectedFailureCode = command.expectedFailureCode();
        this.observedFailureCode = observedFailureCode;
        this.replayOutcome = replayOutcome;
        this.acknowledgedAt = acknowledgedAt;
    }
}
