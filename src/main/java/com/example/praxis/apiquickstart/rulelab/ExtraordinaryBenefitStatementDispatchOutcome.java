package com.example.praxis.apiquickstart.rulelab;

/** Observable result of one explicit dispatcher iteration. */
public enum ExtraordinaryBenefitStatementDispatchOutcome {
    NO_SINK,
    EMPTY,
    DELIVERED,
    RETRY_SCHEDULED,
    DEAD_LETTERED
}
