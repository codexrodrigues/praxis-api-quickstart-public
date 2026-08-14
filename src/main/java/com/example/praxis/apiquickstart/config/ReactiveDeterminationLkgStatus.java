package com.example.praxis.apiquickstart.config;

import java.time.Instant;

record ReactiveDeterminationLkgStatus(
        boolean ready, String mode, int cachedScopeCount, Instant lastResolutionAtUtc, String lastFailureCode) {}
