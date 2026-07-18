package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;

/** Builds fact lookups exclusively from host-owned scope configuration. */
final class ExtraordinaryBenefitFactLookupFactory {
    private final String tenantId;
    private final String environment;
    private final String organizationKey;

    ExtraordinaryBenefitFactLookupFactory(String tenantId, String environment, String organizationKey) {
        this.tenantId = tenantId;
        this.environment = environment;
        this.organizationKey = organizationKey;
    }

    RuleFactLookup create(String factReference, Instant asOf) {
        return new RuleFactLookup(tenantId, environment, organizationKey, factReference, asOf);
    }
}
