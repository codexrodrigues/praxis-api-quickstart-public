package com.example.praxis.apiquickstart.rulelab;

import java.util.UUID;

/** Server-coordinated identity of one immutable candidate to preload for a rollout. */
public record RuleCandidatePreloadCommand(
        UUID rolloutId, String candidateSnapshotKey, String candidateContentHash) {}
