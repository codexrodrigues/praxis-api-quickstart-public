package com.example.praxis.apiquickstart.rulelab;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Isolated operational transactions for append, lease and acknowledgement. */
class RuleExecutionObservationOutbox {
    private final RuleExecutionObservationOutboxRepository repository;

    RuleExecutionObservationOutbox(RuleExecutionObservationOutboxRepository repository) {
        this.repository = repository;
    }

    @Transactional(transactionManager = "apiTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void append(RuleExecutionObservationDelivery evidence) {
        repository.saveAndFlush(new RuleExecutionObservationOutboxMessage(
                evidence.observationId(), evidence.tenantId(), evidence.environment(), evidence.snapshotKey(),
                evidence.snapshotContentHash(), evidence.activationRevision(), evidence.outcome(),
                evidence.durationMicros(), evidence.observedAt()));
    }

    @Transactional(transactionManager = "apiTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public Optional<RuleExecutionObservationDelivery> claim(Instant now, Duration lease) {
        return repository.findDispatchable(now, PageRequest.of(0, 1)).stream().findFirst()
                .map(message -> message.claim(now, lease));
    }

    @Transactional(transactionManager = "apiTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void delivered(UUID id, UUID lease, Instant now) {
        repository.findLocked(id).orElseThrow().delivered(lease, now);
    }

    @Transactional(transactionManager = "apiTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void failed(UUID id, UUID lease, Instant now, int attempts, Duration delay, String code) {
        repository.findLocked(id).orElseThrow().failed(lease, now, attempts, delay, code);
    }
}
