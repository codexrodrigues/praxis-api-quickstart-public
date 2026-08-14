package com.example.praxis.apiquickstart.rulelab;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RuleExecutionObservationOutboxRepository
        extends JpaRepository<RuleExecutionObservationOutboxMessage, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select message from RuleExecutionObservationOutboxMessage message
            where (message.deliveryStatus = com.example.praxis.apiquickstart.rulelab.RuleExecutionObservationOutboxStatus.PENDING
                    and message.nextAttemptAt <= :now)
               or (message.deliveryStatus = com.example.praxis.apiquickstart.rulelab.RuleExecutionObservationOutboxStatus.PROCESSING
                    and message.leaseUntil <= :now)
            order by message.createdAt, message.observationId
            """)
    List<RuleExecutionObservationOutboxMessage> findDispatchable(
            @Param("now") Instant now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select message from RuleExecutionObservationOutboxMessage message where message.observationId = :id")
    Optional<RuleExecutionObservationOutboxMessage> findLocked(@Param("id") UUID id);
}
