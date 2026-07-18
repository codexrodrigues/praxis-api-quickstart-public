package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable host operation identity passed explicitly across statement stages.
 *
 * <p>This context is not part of the engine contract and contains no business facts or effect
 * payload. The host captures one snapshot, instant and timezone for the complete transaction.</p>
 */
record RuleLabOperationContext(
        UUID operationId,
        String correlationId,
        String tenantId,
        String environment,
        String actorSubject,
        Instant nowUtc,
        ZoneId userTimeZone,
        String snapshotKey,
        String snapshotContentHash,
        long snapshotActivationRevision,
        RuleLabOperationCardinality cardinality,
        int itemCount) {

    RuleLabOperationContext {
        operationId = Objects.requireNonNull(operationId, "operationId is required");
        correlationId = requireText(correlationId, "correlationId");
        tenantId = requireText(tenantId, "tenantId");
        environment = requireText(environment, "environment");
        actorSubject = requireText(actorSubject, "actorSubject");
        nowUtc = Objects.requireNonNull(nowUtc, "nowUtc is required");
        userTimeZone = Objects.requireNonNull(userTimeZone, "userTimeZone is required");
        snapshotKey = requireText(snapshotKey, "snapshotKey");
        snapshotContentHash = requireText(snapshotContentHash, "snapshotContentHash");
        if (snapshotActivationRevision < 1) {
            throw new IllegalArgumentException("snapshotActivationRevision must be positive");
        }
        cardinality = Objects.requireNonNull(cardinality, "cardinality is required");
        if (itemCount < 1 || itemCount > 50) {
            throw new IllegalArgumentException("itemCount must be between 1 and 50");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
