package com.example.praxis.apiquickstart.rulelab;

/** Host-side transaction cardinality; it is deliberately distinct from RuleSet slot cardinality. */
enum RuleLabOperationCardinality {
    SINGLE_ITEM,
    ITEM_INDEPENDENT,
    STATEMENT_ATOMIC
}
