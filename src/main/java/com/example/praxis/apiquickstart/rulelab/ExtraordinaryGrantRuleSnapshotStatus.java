package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;

/** Safe operational status of the Quickstart Rule Lab snapshot cache. */
public record ExtraordinaryGrantRuleSnapshotStatus(
        boolean ready,
        String activeSnapshotKey,
        String activeContentHash,
        String activeHeadEtag,
        long activationRevision,
        Instant lastAttemptAtUtc,
        Instant lastActivatedAtUtc,
        String lastFailureCode,
        String lastFailureMessage) {
}
