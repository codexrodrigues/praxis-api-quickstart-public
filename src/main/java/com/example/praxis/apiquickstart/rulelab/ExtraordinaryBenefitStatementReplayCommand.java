package com.example.praxis.apiquickstart.rulelab;

import java.util.Objects;
import java.util.UUID;

/** Governed operator intent to retry one dead-letter message after external verification. */
public record ExtraordinaryBenefitStatementReplayCommand(
        UUID messageId,
        String expectedFailureCode,
        String actorSubject,
        String justification,
        String correlationId) {

    public ExtraordinaryBenefitStatementReplayCommand {
        Objects.requireNonNull(messageId, "messageId is required");
        expectedFailureCode = requireText(expectedFailureCode, "expectedFailureCode", 120, 1);
        actorSubject = requireText(actorSubject, "actorSubject", 255, 1);
        justification = requireText(justification, "justification", 1000, 10);
        correlationId = requireText(correlationId, "correlationId", 255, 1);
    }

    private static String requireText(String value, String field, int maximum, int minimum) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() < minimum || normalized.length() > maximum) {
            throw new IllegalArgumentException(field + " length must be between " + minimum + " and " + maximum);
        }
        return normalized;
    }
}
