package com.example.praxis.apiquickstart.rulelab;

import java.util.UUID;

public record ExtraordinaryBenefitStatementReconciliationResult(
        ExtraordinaryBenefitStatementReconciliationOutcome outcome,
        UUID messageId) {
}
