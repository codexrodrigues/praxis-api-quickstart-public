package com.example.praxis.apiquickstart.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/** Low-cardinality telemetry for the bounded payroll aggregate cache. */
@Component
final class ReactiveDeterminationLkgTelemetry {
    private final MeterRegistry registry;
    private final AtomicReference<ReactiveDeterminationLkgStatus> status = new AtomicReference<>(
            new ReactiveDeterminationLkgStatus(true, "not_observed", 0, null, null));

    ReactiveDeterminationLkgTelemetry(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry is required");
    }

    void resolved(String mode, int cachedScopes) {
        record(mode);
        status.set(new ReactiveDeterminationLkgStatus(true, mode, cachedScopes, Instant.now(), null));
    }

    void rejected(String code, int cachedScopes) {
        record("rejected");
        status.set(new ReactiveDeterminationLkgStatus(false, "unavailable", cachedScopes, Instant.now(), code));
    }

    ReactiveDeterminationLkgStatus status() { return status.get(); }

    private void record(String result) {
        Counter.builder("praxis.reactive.determination.snapshot.resolutions")
                .description("Payroll aggregate snapshot resolutions by bounded result")
                .tag("ruleset", PayrollReactiveDeterminationRuleSet.RULE_SET_KEY)
                .tag("result", switch (result) {
                    case "fresh", "lkg", "rejected" -> result;
                    default -> throw new IllegalArgumentException("Unsupported result");
                })
                .register(registry).increment();
    }
}
