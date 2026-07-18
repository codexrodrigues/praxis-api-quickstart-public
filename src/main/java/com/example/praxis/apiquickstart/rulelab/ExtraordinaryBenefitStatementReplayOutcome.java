package com.example.praxis.apiquickstart.rulelab;

/** Closed operational outcomes for a governed dead-letter replay request. */
public enum ExtraordinaryBenefitStatementReplayOutcome {
    REPLAY_SCHEDULED,
    ACKNOWLEDGED_NO_REPLAY,
    REJECTED_MESSAGE_NOT_FOUND,
    REJECTED_NOT_DEAD_LETTER,
    REJECTED_QUARANTINE,
    REJECTED_FAILURE_CHANGED,
    REJECTED_NO_PROBE,
    REJECTED_PROBE_FAILED
}
