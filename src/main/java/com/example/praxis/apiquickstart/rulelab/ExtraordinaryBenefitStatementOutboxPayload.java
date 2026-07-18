package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Minimal external notification; consumers reload authorized business data by item id. */
record ExtraordinaryBenefitStatementOutboxPayload(
        UUID operationId,
        List<Long> itemIds,
        int itemCount,
        String snapshotKey,
        String snapshotContentHash,
        long snapshotActivationRevision,
        Instant evaluatedAtUtc) {

    ExtraordinaryBenefitStatementOutboxPayload {
        itemIds = List.copyOf(itemIds);
    }
}
