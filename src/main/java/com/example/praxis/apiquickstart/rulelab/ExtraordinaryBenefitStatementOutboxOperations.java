package com.example.praxis.apiquickstart.rulelab;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal operations SPI for a governed worker or job runtime.
 *
 * <p>It exposes no HTTP administration surface and never removes pending, processing or dead-letter
 * messages. A deployment must invoke retention from its owned scheduler and alert on the snapshot.</p>
 */
@Service
public class ExtraordinaryBenefitStatementOutboxOperations {
    private final ExtraordinaryBenefitStatementOutboxRepository repository;
    private final ExtraordinaryBenefitStatementOutboxTelemetry telemetry;
    private final Clock clock;
    private final Duration deliveredRetention;
    private final int retentionBatchSize;

    ExtraordinaryBenefitStatementOutboxOperations(
            ExtraordinaryBenefitStatementOutboxRepository repository,
            ExtraordinaryBenefitStatementOutboxTelemetry telemetry,
            @Qualifier("extraordinaryGrantRuleClock") Clock clock,
            @Value("${praxis.rule-lab.outbox.retention.delivered-days:30}") int deliveredDays,
            @Value("${praxis.rule-lab.outbox.retention.batch-size:500}") int retentionBatchSize) {
        if (deliveredDays < 1 || deliveredDays > 3650) {
            throw new IllegalArgumentException("delivered retention must be between 1 and 3650 days");
        }
        if (retentionBatchSize < 1 || retentionBatchSize > 10_000) {
            throw new IllegalArgumentException("retention batch size must be between 1 and 10000");
        }
        this.repository = repository;
        this.telemetry = telemetry;
        this.clock = clock;
        this.deliveredRetention = Duration.ofDays(deliveredDays);
        this.retentionBatchSize = retentionBatchSize;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.REPEATABLE_READ,
            readOnly = true)
    public ExtraordinaryBenefitStatementOutboxOperationalSnapshot snapshot() {
        Instant nowUtc = clock.instant();
        return new ExtraordinaryBenefitStatementOutboxOperationalSnapshot(
                nowUtc,
                repository.countByDeliveryStatus(ExtraordinaryBenefitStatementOutboxStatus.PENDING),
                repository.countByDeliveryStatus(ExtraordinaryBenefitStatementOutboxStatus.PROCESSING),
                repository.countByDeliveryStatus(ExtraordinaryBenefitStatementOutboxStatus.DELIVERED),
                repository.countByDeliveryStatus(ExtraordinaryBenefitStatementOutboxStatus.DEAD_LETTER),
                repository.findOldestUndeliveredCreatedAt());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExtraordinaryBenefitStatementOutboxRetentionResult purgeDelivered() {
        Instant cutoff = clock.instant().minus(deliveredRetention);
        List<java.util.UUID> candidates = repository.findDeliveredBefore(
                cutoff, PageRequest.of(0, retentionBatchSize));
        int deleted = candidates.isEmpty()
                ? 0
                : repository.deleteDeliveredBeforeByMessageId(candidates, cutoff);
        telemetry.deliveredRowsPurged(deleted);
        return new ExtraordinaryBenefitStatementOutboxRetentionResult(
                cutoff, deleted, candidates.size() == retentionBatchSize);
    }
}
