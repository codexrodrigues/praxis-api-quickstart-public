package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;
import java.util.UUID;

/** Minimal state used to authorize replay without exposing the outbox payload. */
record ExtraordinaryBenefitStatementReplayCandidate(
        UUID messageId,
        ExtraordinaryBenefitStatementOutboxStatus status,
        String failureCode,
        Instant failedAtUtc) {
}
