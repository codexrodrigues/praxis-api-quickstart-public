package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;
import java.util.UUID;

record RuleExecutionObservationDelivery(
        UUID observationId,
        String tenantId,
        String environment,
        String snapshotKey,
        String snapshotContentHash,
        long activationRevision,
        String outcome,
        long durationMicros,
        Instant observedAt,
        UUID leaseToken,
        int attempt) {}
