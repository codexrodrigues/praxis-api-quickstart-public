package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;

/** Proveniencia allowlisted: identifica fonte e versao sem expor tenant, organizacao ou referencia consultada. */
record RuleFactProvenance(
        String providerKey,
        String sourceSystem,
        String sourceRecordDigest,
        long sourceVersion,
        Instant sourceRecordedAt,
        Instant asOf,
        String scopeDigest) {

    RuleFactProvenance {
        if (providerKey == null || providerKey.isBlank()) {
            throw new IllegalArgumentException("providerKey is required");
        }
        if (sourceSystem == null || sourceSystem.isBlank()) {
            throw new IllegalArgumentException("sourceSystem is required");
        }
        if (sourceRecordDigest == null || !sourceRecordDigest.matches("[0-9A-F]{64}")) {
            throw new IllegalArgumentException("sourceRecordDigest must be an uppercase SHA-256 digest");
        }
        if (sourceVersion < 1) {
            throw new IllegalArgumentException("sourceVersion must be positive");
        }
        if (sourceRecordedAt == null || asOf == null) {
            throw new IllegalArgumentException("sourceRecordedAt and asOf are required");
        }
        if (scopeDigest == null || !scopeDigest.matches("[0-9A-F]{64}")) {
            throw new IllegalArgumentException("scopeDigest must be an uppercase SHA-256 digest");
        }
    }
}
