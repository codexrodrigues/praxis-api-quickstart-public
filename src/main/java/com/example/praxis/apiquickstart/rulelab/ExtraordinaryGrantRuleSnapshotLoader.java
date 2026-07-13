package com.example.praxis.apiquickstart.rulelab;

import java.time.Instant;
import java.time.Clock;
import java.util.Objects;
import org.praxisplatform.config.service.DomainRuleSnapshotReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Polls the Config Starter read boundary and delegates atomic activation to the host runtime. */
public final class ExtraordinaryGrantRuleSnapshotLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ExtraordinaryGrantRuleSnapshotLoader.class);

    private final DomainRuleSnapshotReader reader;
    private final ExtraordinaryGrantRuleSnapshotRuntime runtime;
    private final String tenantId;
    private final String environment;
    private final Clock clock;

    public ExtraordinaryGrantRuleSnapshotLoader(
            DomainRuleSnapshotReader reader,
            ExtraordinaryGrantRuleSnapshotRuntime runtime,
            String tenantId,
            String environment,
            Clock clock) {
        this.reader = Objects.requireNonNull(reader, "reader is required");
        this.runtime = Objects.requireNonNull(runtime, "runtime is required");
        this.tenantId = requireText(tenantId, "tenantId");
        this.environment = requireText(environment, "environment");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    /** Explicit refresh seam used by startup, operations and deterministic tests. */
    public ExtraordinaryGrantRuleSnapshotStatus refreshNow() {
        Instant attemptedAt = clock.instant();
        try {
            return reader.findActive(
                            tenantId,
                            environment,
                            ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY)
                    .map(candidate -> runtime.activate(candidate, tenantId, environment, attemptedAt))
                    .orElseGet(() -> runtime.reject(
                            "HEAD_NOT_FOUND",
                            "No governed extraordinary-grant RuleSet head exists in this scope",
                            attemptedAt));
        } catch (RuntimeException exception) {
            LOG.warn("Rule Lab snapshot refresh failed; retaining last-known-good plan: {}", exception.getMessage());
            return runtime.reject(
                    "CONTROL_PLANE_UNAVAILABLE",
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(),
                    attemptedAt);
        }
    }

    /** Periodic hot-reload poll; candidate failures never clear the active reference. */
    @Scheduled(
            initialDelayString = "${praxis.rule-lab.snapshot.initial-delay-ms:5000}",
            fixedDelayString = "${praxis.rule-lab.snapshot.refresh-delay-ms:30000}")
    public void scheduledRefresh() {
        refreshNow();
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
