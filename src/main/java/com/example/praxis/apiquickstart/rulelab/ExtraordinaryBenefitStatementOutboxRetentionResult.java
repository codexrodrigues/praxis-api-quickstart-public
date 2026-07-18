package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;

/** Result of one bounded retention pass; dead-letter rows are never deleted by this operation. */
public record ExtraordinaryBenefitStatementOutboxRetentionResult(
        Instant cutoffExclusiveUtc,
        int deletedDeliveredRows,
        boolean batchLimitReached) {
}
