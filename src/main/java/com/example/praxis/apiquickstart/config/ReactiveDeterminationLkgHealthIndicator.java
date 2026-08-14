package com.example.praxis.apiquickstart.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Redacted readiness for the host-owned payroll aggregate; never exposes tenant or snapshot identity. */
@Component("payrollReactiveDetermination")
final class ReactiveDeterminationLkgHealthIndicator implements HealthIndicator {
    private final AppliedReactiveDeterminationResolver resolver;

    ReactiveDeterminationLkgHealthIndicator(AppliedReactiveDeterminationResolver resolver) {
        this.resolver = resolver;
    }

    @Override public Health health() {
        ReactiveDeterminationLkgStatus status = resolver.lkgStatus();
        Health.Builder health = status.ready() ? Health.up() : Health.outOfService();
        health.withDetail("mode", status.mode())
                .withDetail("cachedScopeCount", status.cachedScopeCount());
        if (status.lastResolutionAtUtc() != null) health.withDetail("lastResolutionAtUtc", status.lastResolutionAtUtc());
        if (status.lastFailureCode() != null) health.withDetail("lastFailureCode", status.lastFailureCode());
        return health.build();
    }
}
