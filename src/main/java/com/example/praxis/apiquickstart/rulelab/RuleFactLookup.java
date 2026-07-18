package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;

/** Escopo interno e imutavel usado pelo host para buscar facts sem aceitar contexto empresarial do cliente HTTP. */
record RuleFactLookup(
        String tenantId,
        String environment,
        String organizationKey,
        String factReference,
        Instant asOf) {

    RuleFactLookup {
        tenantId = requireText(tenantId, "tenantId");
        environment = requireText(environment, "environment");
        organizationKey = requireText(organizationKey, "organizationKey");
        factReference = requireText(factReference, "factReference");
        if (asOf == null) {
            throw new IllegalArgumentException("asOf is required");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 120) {
            throw new IllegalArgumentException(field + " must contain 1 to 120 characters");
        }
        return value;
    }
}
