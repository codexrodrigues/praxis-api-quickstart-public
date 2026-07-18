package com.example.praxis.apiquickstart.rulelab;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Low-cardinality operational telemetry for the host-owned outbox workers. */
@Component
final class ExtraordinaryBenefitStatementOutboxTelemetry {
    private final MeterRegistry meterRegistry;

    ExtraordinaryBenefitStatementOutboxTelemetry(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry is required");
    }

    Timer.Sample start() {
        return Timer.start(meterRegistry);
    }

    void dispatchCompleted(Timer.Sample sample, ExtraordinaryBenefitStatementDispatchOutcome outcome) {
        String tag = outcome.name();
        Counter.builder("praxis.rule.lab.outbox.dispatches")
                .description("Completed statement outbox dispatch attempts by bounded outcome")
                .tag("outcome", tag)
                .register(meterRegistry)
                .increment();
        sample.stop(Timer.builder("praxis.rule.lab.outbox.dispatch.duration")
                .description("Statement outbox dispatch duration")
                .tag("outcome", tag)
                .register(meterRegistry));
    }

    void reconciliationCompleted(
            Timer.Sample sample,
            ExtraordinaryBenefitStatementReconciliationOutcome outcome) {
        String tag = outcome.name();
        Counter.builder("praxis.rule.lab.outbox.reconciliations")
                .description("Completed statement outbox reconciliation scans by bounded outcome")
                .tag("outcome", tag)
                .register(meterRegistry)
                .increment();
        sample.stop(Timer.builder("praxis.rule.lab.outbox.reconciliation.duration")
                .description("Statement outbox reconciliation duration")
                .tag("outcome", tag)
                .register(meterRegistry));
    }

    void deliveredRowsPurged(int deletedRows) {
        Counter.builder("praxis.rule.lab.outbox.retention.deleted")
                .description("Delivered statement outbox rows removed by the governed retention job")
                .register(meterRegistry)
                .increment(deletedRows);
    }

    void replayCompleted(ExtraordinaryBenefitStatementReplayOutcome outcome) {
        Counter.builder("praxis.rule.lab.outbox.replays")
                .description("Governed statement dead-letter replay decisions by bounded outcome")
                .tag("outcome", outcome.name())
                .register(meterRegistry)
                .increment();
    }
}
