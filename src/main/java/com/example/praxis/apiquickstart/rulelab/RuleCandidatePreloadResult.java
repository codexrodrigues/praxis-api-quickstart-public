package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;
import java.util.UUID;

/** Safe local result of isolated compilation and candidate-probe delivery. */
public record RuleCandidatePreloadResult(
        UUID rolloutId, boolean preloadReady, boolean probeUpdated,
        String failureCode, Instant observedAtUtc) {}
