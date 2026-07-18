package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Payload-free evidence returned to the governed job or administrative console. */
public record ExtraordinaryBenefitStatementReplayResult(
        UUID auditId,
        UUID messageId,
        ExtraordinaryBenefitStatementReplayOutcome outcome,
        Instant decidedAtUtc) {

    public ExtraordinaryBenefitStatementReplayResult {
        Objects.requireNonNull(auditId, "auditId is required");
        Objects.requireNonNull(messageId, "messageId is required");
        Objects.requireNonNull(outcome, "outcome is required");
        Objects.requireNonNull(decidedAtUtc, "decidedAtUtc is required");
    }
}
