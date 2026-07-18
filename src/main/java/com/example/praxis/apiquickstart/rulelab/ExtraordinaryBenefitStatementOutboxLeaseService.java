package com.example.praxis.apiquickstart.rulelab;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Owns short, isolated claim/ack/retry transactions around external I/O. */
@Service
class ExtraordinaryBenefitStatementOutboxLeaseService {
    private final ExtraordinaryBenefitStatementOutboxRepository repository;

    ExtraordinaryBenefitStatementOutboxLeaseService(
            ExtraordinaryBenefitStatementOutboxRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ExtraordinaryBenefitStatementOutboxClaim> claimNext(
            Instant nowUtc,
            Duration leaseDuration) {
        return repository.findDispatchable(nowUtc, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(message -> message.claim(nowUtc, leaseDuration));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDelivered(
            UUID messageId,
            UUID leaseToken,
            Instant deliveredAtUtc) {
        var message = repository.findLockedById(messageId)
                .orElseThrow(() -> new IllegalStateException("Outbox message no longer exists"));
        message.markDelivered(leaseToken, deliveredAtUtc);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExtraordinaryBenefitStatementOutboxStatus markFailed(
            UUID messageId,
            UUID leaseToken,
            Instant failedAtUtc,
            int maximumAttempts,
            Duration retryDelay,
            boolean terminalFailure,
            String failureCode,
            String failureMessage) {
        var message = repository.findLockedById(messageId)
                .orElseThrow(() -> new IllegalStateException("Outbox message no longer exists"));
        return message.markFailed(
                leaseToken, failedAtUtc, maximumAttempts, retryDelay, terminalFailure,
                failureCode, failureMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<UUID> findReconciliationCandidates(Instant nowUtc, int scanLimit) {
        return List.copyOf(repository.findReconciliationCandidates(nowUtc, PageRequest.of(0, scanLimit)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markExternallyAcknowledged(UUID messageId, Instant reconciledAtUtc) {
        var message = repository.findLockedById(messageId)
                .orElseThrow(() -> new IllegalStateException("Outbox message no longer exists"));
        return message.markExternallyAcknowledged(reconciledAtUtc);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleReconciliation(UUID messageId, Instant nextReconciliationAtUtc) {
        var message = repository.findLockedById(messageId)
                .orElseThrow(() -> new IllegalStateException("Outbox message no longer exists"));
        message.scheduleReconciliation(nextReconciliationAtUtc);
    }
}
