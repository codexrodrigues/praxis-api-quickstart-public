package com.example.praxis.apiquickstart.rulelab;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

/** Safe Actuator readiness projection for the explicitly enabled Rule Lab runtime. */
public final class ExtraordinaryGrantRuleSnapshotHealthIndicator implements HealthIndicator {
    private final ExtraordinaryGrantRuleSnapshotRuntime runtime;

    public ExtraordinaryGrantRuleSnapshotHealthIndicator(ExtraordinaryGrantRuleSnapshotRuntime runtime) {
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime is required");
    }

    @Override
    public Health health() {
        ExtraordinaryGrantRuleSnapshotStatus snapshot = runtime.status();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("ready", snapshot.ready());
        put(details, "activeSnapshotKey", snapshot.activeSnapshotKey());
        put(details, "activeContentHash", snapshot.activeContentHash());
        put(details, "activeHeadEtag", snapshot.activeHeadEtag());
        details.put("activationRevision", snapshot.activationRevision());
        put(details, "lastAttemptAtUtc", snapshot.lastAttemptAtUtc());
        put(details, "lastActivatedAtUtc", snapshot.lastActivatedAtUtc());
        put(details, "lastFailureCode", snapshot.lastFailureCode());
        return Health.status(snapshot.ready() ? Status.UP : Status.OUT_OF_SERVICE)
                .withDetails(details)
                .build();
    }

    private void put(Map<String, Object> details, String key, Object value) {
        if (value != null) {
            details.put(key, value);
        }
    }
}
