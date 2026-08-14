package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.praxisplatform.rules.contract.RuleDecision;

class ExtraordinaryGrantRuleRuntimeTelemetryTest {

    @Test
    void recordsOnlyBoundedRuleSetOutcomeStatusAndRefreshDimensions() {
        var registry = new SimpleMeterRegistry();
        var telemetry = new ExtraordinaryGrantRuleRuntimeTelemetry(registry);

        telemetry.evaluationCompleted(telemetry.evaluationStarted(), RuleDecision.DENY);
        telemetry.evaluationFailed(telemetry.evaluationStarted());
        telemetry.snapshotRefresh("activated");
        telemetry.snapshotRefresh("unchanged");
        telemetry.snapshotRefresh("rejected");

        assertThat(registry.get("praxis.rule.runtime.evaluations")
                .tags("ruleset", "extraordinary-grant-eligibility", "outcome", "deny", "status", "completed")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get("praxis.rule.runtime.evaluations")
                .tags("ruleset", "extraordinary-grant-eligibility", "outcome", "technical_error", "status", "failed")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get("praxis.rule.runtime.evaluation.duration").timers()).hasSize(2);
        assertThat(registry.get("praxis.rule.runtime.snapshot.refreshes").counters()).hasSize(3);
        assertThat(registry.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags())
                .extracting(tag -> tag.getKey())
                .doesNotContain("snapshot", "snapshot_hash", "facts", "actor", "request"));
    }
}
