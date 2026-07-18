package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;

/**
 * Safe, payload-free snapshot for worker health checks and SLO evaluation.
 * {@code oldestUndeliveredCreatedAtUtc} is {@code null} when there is no backlog.
 */
public record ExtraordinaryBenefitStatementOutboxOperationalSnapshot(
        Instant observedAtUtc,
        long pending,
        long processing,
        long delivered,
        long deadLetter,
        Instant oldestUndeliveredCreatedAtUtc) {
}
