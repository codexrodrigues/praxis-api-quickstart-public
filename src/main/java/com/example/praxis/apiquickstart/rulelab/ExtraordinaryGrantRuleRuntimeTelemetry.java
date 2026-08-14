package com.example.praxis.apiquickstart.rulelab;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.praxisplatform.rules.contract.RuleDecision;

/** Redacted, low-cardinality operational telemetry for the host-owned RuleSet runtime. */
final class ExtraordinaryGrantRuleRuntimeTelemetry {
    private final MeterRegistry meterRegistry;

    ExtraordinaryGrantRuleRuntimeTelemetry(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry is required");
    }

    long evaluationStarted() {
        return System.nanoTime();
    }

    long evaluationCompleted(long startedAtNanos, RuleDecision decision) {
        return recordEvaluation(startedAtNanos, tag(Objects.requireNonNull(decision, "decision is required")), "completed");
    }

    void evaluationFailed(long startedAtNanos) {
        recordEvaluation(startedAtNanos, "technical_error", "failed");
    }

    void snapshotRefresh(String result) {
        Counter.builder("praxis.rule.runtime.snapshot.refreshes")
                .description("Governed snapshot refresh attempts by bounded result")
                .tag("ruleset", ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY)
                .tag("result", requireResult(result))
                .register(meterRegistry)
                .increment();
    }

    void observationEnqueued(String result) {
        Counter.builder("praxis.rule.runtime.execution.observation.outbox")
                .description("Redacted execution observations offered to the durable operational outbox")
                .tag("ruleset", ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY)
                .tag("result", switch (result) {
                    case "accepted", "unavailable" -> result;
                    default -> throw new IllegalArgumentException("Unsupported observation enqueue result");
                })
                .register(meterRegistry)
                .increment();
    }

    private long recordEvaluation(long startedAtNanos, String outcome, String status) {
        long elapsedNanos = Math.max(0, System.nanoTime() - startedAtNanos);
        Counter.builder("praxis.rule.runtime.evaluations")
                .description("Deterministic RuleSet evaluations without facts or business identifiers")
                .tag("ruleset", ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY)
                .tag("outcome", outcome)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
        Timer.builder("praxis.rule.runtime.evaluation.duration")
                .description("Host-observed duration of deterministic RuleSet evaluations")
                .tag("ruleset", ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY)
                .tag("outcome", outcome)
                .tag("status", status)
                .register(meterRegistry)
                .record(elapsedNanos, TimeUnit.NANOSECONDS);
        return TimeUnit.NANOSECONDS.toMicros(elapsedNanos);
    }

    private String requireResult(String result) {
        return switch (Objects.requireNonNull(result, "result is required")) {
            case "activated", "unchanged", "rejected" -> result;
            default -> throw new IllegalArgumentException("Unsupported snapshot refresh result");
        };
    }

    private String tag(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
