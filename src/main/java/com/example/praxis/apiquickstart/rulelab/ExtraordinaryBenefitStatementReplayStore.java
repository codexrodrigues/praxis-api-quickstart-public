package com.example.praxis.apiquickstart.rulelab;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Short database transactions used around the replay service's external acknowledgement probe. */
@Service
class ExtraordinaryBenefitStatementReplayStore {
    private final ExtraordinaryBenefitStatementOutboxRepository outboxRepository;
    @PersistenceContext(unitName = "api")
    private EntityManager entityManager;

    ExtraordinaryBenefitStatementReplayStore(ExtraordinaryBenefitStatementOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<ExtraordinaryBenefitStatementReplayCandidate> candidate(UUID messageId) {
        return outboxRepository.findById(messageId)
                .map(ExtraordinaryBenefitStatementOutboxMessage::replayCandidate);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExtraordinaryBenefitStatementReplayResult record(
            ExtraordinaryBenefitStatementReplayCommand command,
            Instant decidedAtUtc,
            String observedFailureCode,
            ExtraordinaryBenefitStatementReplayOutcome outcome,
            Instant acknowledgedAtUtc) {
        UUID auditId = UUID.randomUUID();
        appendAudit(new ExtraordinaryBenefitStatementReplayAudit(
                auditId, command, decidedAtUtc, observedFailureCode, outcome, acknowledgedAtUtc));
        return new ExtraordinaryBenefitStatementReplayResult(
                auditId, command.messageId(), outcome, decidedAtUtc);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExtraordinaryBenefitStatementReplayResult acknowledgeWithoutReplay(
            ExtraordinaryBenefitStatementReplayCommand command,
            Instant decidedAtUtc,
            String observedFailureCode,
            ExtraordinaryBenefitStatementExternalAcknowledgement acknowledgement) {
        var message = outboxRepository.findLockedById(command.messageId()).orElse(null);
        if (message == null) {
            return recordInCurrentTransaction(command, decidedAtUtc, null,
                    ExtraordinaryBenefitStatementReplayOutcome.REJECTED_MESSAGE_NOT_FOUND, null);
        }
        boolean alreadyDelivered = message.replayCandidate().status()
                == ExtraordinaryBenefitStatementOutboxStatus.DELIVERED;
        boolean acknowledged = message.markExternallyAcknowledged(decidedAtUtc);
        return recordInCurrentTransaction(
                command,
                decidedAtUtc,
                observedFailureCode,
                acknowledged || alreadyDelivered
                        ? ExtraordinaryBenefitStatementReplayOutcome.ACKNOWLEDGED_NO_REPLAY
                        : ExtraordinaryBenefitStatementReplayOutcome.REJECTED_NOT_DEAD_LETTER,
                acknowledgement.acknowledgedAtUtc());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExtraordinaryBenefitStatementReplayResult scheduleReplay(
            ExtraordinaryBenefitStatementReplayCommand command,
            Instant decidedAtUtc,
            Duration quarantine) {
        var message = outboxRepository.findLockedById(command.messageId()).orElse(null);
        if (message == null) {
            return recordInCurrentTransaction(command, decidedAtUtc, null,
                    ExtraordinaryBenefitStatementReplayOutcome.REJECTED_MESSAGE_NOT_FOUND, null);
        }
        var current = message.replayCandidate();
        if (current.status() != ExtraordinaryBenefitStatementOutboxStatus.DEAD_LETTER) {
            return recordInCurrentTransaction(command, decidedAtUtc, current.failureCode(),
                    ExtraordinaryBenefitStatementReplayOutcome.REJECTED_NOT_DEAD_LETTER, null);
        }
        if (!Objects.equals(current.failureCode(), command.expectedFailureCode())) {
            return recordInCurrentTransaction(command, decidedAtUtc, current.failureCode(),
                    ExtraordinaryBenefitStatementReplayOutcome.REJECTED_FAILURE_CHANGED, null);
        }
        if (current.failedAtUtc() == null
                || current.failedAtUtc().isAfter(decidedAtUtc.minus(quarantine))) {
            return recordInCurrentTransaction(command, decidedAtUtc, current.failureCode(),
                    ExtraordinaryBenefitStatementReplayOutcome.REJECTED_QUARANTINE, null);
        }
        message.scheduleGovernedReplay(current.failureCode(), decidedAtUtc);
        return recordInCurrentTransaction(command, decidedAtUtc, current.failureCode(),
                ExtraordinaryBenefitStatementReplayOutcome.REPLAY_SCHEDULED, null);
    }

    private ExtraordinaryBenefitStatementReplayResult recordInCurrentTransaction(
            ExtraordinaryBenefitStatementReplayCommand command,
            Instant decidedAtUtc,
            String observedFailureCode,
            ExtraordinaryBenefitStatementReplayOutcome outcome,
            Instant acknowledgedAtUtc) {
        UUID auditId = UUID.randomUUID();
        appendAudit(new ExtraordinaryBenefitStatementReplayAudit(
                auditId, command, decidedAtUtc, observedFailureCode, outcome, acknowledgedAtUtc));
        return new ExtraordinaryBenefitStatementReplayResult(
                auditId, command.messageId(), outcome, decidedAtUtc);
    }

    private void appendAudit(ExtraordinaryBenefitStatementReplayAudit audit) {
        // Persist models the ledger's insert-only contract; repository save could select merge for
        // assigned identifiers and would obscure the database-enforced immutability boundary.
        entityManager.persist(audit);
    }
}
