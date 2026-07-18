package com.example.praxis.apiquickstart.rulelab;

/** Durable delivery states for the statement outbox. */
enum ExtraordinaryBenefitStatementOutboxStatus {
    PENDING,
    PROCESSING,
    DELIVERED,
    DEAD_LETTER
}
