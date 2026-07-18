package com.example.praxis.apiquickstart.rulelab;

import java.util.UUID;

/** Safe dispatcher observation without facts or outbox payload. */
public record ExtraordinaryBenefitStatementDispatchResult(
        ExtraordinaryBenefitStatementDispatchOutcome outcome,
        UUID messageId,
        int attempt) {
}
