package com.example.praxis.apiquickstart.rulelab;

/** Lexical stages used by the host while a statement transaction is active. */
enum RuleLabOperationStage {
    OPENED,
    EVALUATED,
    LOCAL_FLUSHED,
    AGGREGATE_VERIFIED,
    OUTBOX_APPENDED,
    IDEMPOTENCY_COMPLETED
}
