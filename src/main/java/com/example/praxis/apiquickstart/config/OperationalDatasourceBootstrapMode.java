package com.example.praxis.apiquickstart.config;

import java.util.Locale;

/** Explicit deployment policy for the Quickstart-owned operational datasource. */
enum OperationalDatasourceBootstrapMode {
    EXISTING_SCHEMA,
    HOSTED_PUBLIC_DEMO_FIXTURE;

    static OperationalDatasourceBootstrapMode parse(String value) {
        if (value == null || value.isBlank() || "existing-schema".equalsIgnoreCase(value.trim())) {
            return EXISTING_SCHEMA;
        }
        if ("hosted-public-demo-fixture".equalsIgnoreCase(value.trim())) {
            return HOSTED_PUBLIC_DEMO_FIXTURE;
        }
        throw new IllegalStateException(
                "Unsupported PRAXIS_OPERATIONAL_BOOTSTRAP_MODE: " + value.trim().toLowerCase(Locale.ROOT));
    }
}
