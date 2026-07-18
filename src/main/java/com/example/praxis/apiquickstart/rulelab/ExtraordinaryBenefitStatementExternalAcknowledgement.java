package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Consumer-owned evidence that a statement message is durably present in its idempotent inbox. */
public record ExtraordinaryBenefitStatementExternalAcknowledgement(
        UUID messageId,
        Instant acknowledgedAtUtc) {

    public ExtraordinaryBenefitStatementExternalAcknowledgement {
        Objects.requireNonNull(messageId, "messageId is required");
        Objects.requireNonNull(acknowledgedAtUtc, "acknowledgedAtUtc is required");
    }
}
