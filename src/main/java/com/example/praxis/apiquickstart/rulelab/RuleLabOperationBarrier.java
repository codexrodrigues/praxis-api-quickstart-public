package com.example.praxis.apiquickstart.rulelab;

/** Visibility barriers from P2F-ADR-10; only local commit is proven by this host slice. */
enum RuleLabOperationBarrier {
    EVALUATED,
    LOCAL_FLUSHED,
    LOCAL_COMMITTED,
    EXTERNAL_DELIVERED
}
