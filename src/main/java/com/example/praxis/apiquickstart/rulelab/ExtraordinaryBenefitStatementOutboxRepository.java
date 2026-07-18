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
import org.springframework.data.jpa.repository.Modifying;

interface ExtraordinaryBenefitStatementOutboxRepository
        extends JpaRepository<ExtraordinaryBenefitStatementOutboxMessage, UUID> {

    long countByDeliveryStatus(ExtraordinaryBenefitStatementOutboxStatus status);

    @Query("""
            select min(message.createdAt)
            from ExtraordinaryBenefitStatementOutboxMessage message
            where message.deliveryStatus <> com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitStatementOutboxStatus.DELIVERED
            """)
    Instant findOldestUndeliveredCreatedAt();

    @Query("""
            select message.messageId
            from ExtraordinaryBenefitStatementOutboxMessage message
            where message.deliveryStatus = com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitStatementOutboxStatus.DELIVERED
              and message.deliveredAt < :cutoffUtc
            order by message.deliveredAt, message.messageId
            """)
    List<UUID> findDeliveredBefore(@Param("cutoffUtc") Instant cutoffUtc, Pageable pageable);

    @Modifying
    @Query("""
            delete from ExtraordinaryBenefitStatementOutboxMessage message
            where message.messageId in :messageIds
              and message.deliveryStatus = com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitStatementOutboxStatus.DELIVERED
              and message.deliveredAt < :cutoffUtc
            """)
    int deleteDeliveredBeforeByMessageId(
            @Param("messageIds") List<UUID> messageIds,
            @Param("cutoffUtc") Instant cutoffUtc);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select message
            from ExtraordinaryBenefitStatementOutboxMessage message
            where (message.deliveryStatus = com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitStatementOutboxStatus.PENDING
                    and message.nextAttemptAt <= :nowUtc)
               or (message.deliveryStatus = com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitStatementOutboxStatus.PROCESSING
                    and message.leaseUntil <= :nowUtc)
            order by message.createdAt, message.messageId
            """)
    List<ExtraordinaryBenefitStatementOutboxMessage> findDispatchable(
            @Param("nowUtc") Instant nowUtc,
            Pageable pageable);

    @Query("""
            select message.messageId
            from ExtraordinaryBenefitStatementOutboxMessage message
            where (message.deliveryStatus in (
                        com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitStatementOutboxStatus.PENDING,
                        com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitStatementOutboxStatus.DEAD_LETTER)
                    or (message.deliveryStatus = com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitStatementOutboxStatus.PROCESSING
                        and message.leaseUntil <= :nowUtc))
              and message.nextReconciliationAt <= :nowUtc
            order by message.createdAt, message.messageId
            """)
    List<UUID> findReconciliationCandidates(@Param("nowUtc") Instant nowUtc, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select message
            from ExtraordinaryBenefitStatementOutboxMessage message
            where message.messageId = :messageId
            """)
    Optional<ExtraordinaryBenefitStatementOutboxMessage> findLockedById(
            @Param("messageId") UUID messageId);
}
