package com.example.praxis.apiquickstart.rulelab;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Internal governed replay SPI. It never exposes an HTTP administration endpoint and never probes
 * the external consumer while holding an outbox transaction.
 */
@Service
public final class ExtraordinaryBenefitStatementReplayService {
    private final ExtraordinaryBenefitStatementReplayStore store;
    private final ObjectProvider<ExtraordinaryBenefitStatementDeliveryProbe> probeProvider;
    private final ExtraordinaryBenefitStatementOutboxTelemetry telemetry;
    private final Clock clock;
    private final Duration quarantine;

    ExtraordinaryBenefitStatementReplayService(
            ExtraordinaryBenefitStatementReplayStore store,
            ObjectProvider<ExtraordinaryBenefitStatementDeliveryProbe> probeProvider,
            ExtraordinaryBenefitStatementOutboxTelemetry telemetry,
            @Qualifier("extraordinaryGrantRuleClock") Clock clock,
            @Value("${praxis.rule-lab.outbox.replay.quarantine-ms:300000}") long quarantineMs) {
        if (quarantineMs < 1_000 || quarantineMs > Duration.ofDays(7).toMillis()) {
            throw new IllegalArgumentException("replay quarantine must be between 1s and 7d");
        }
        this.store = store;
        this.probeProvider = probeProvider;
        this.telemetry = telemetry;
        this.clock = clock;
        this.quarantine = Duration.ofMillis(quarantineMs);
    }

    public ExtraordinaryBenefitStatementReplayResult requestReplay(
            ExtraordinaryBenefitStatementReplayCommand command) {
        Objects.requireNonNull(command, "command is required");
        var nowUtc = clock.instant();
        var candidate = store.candidate(command.messageId()).orElse(null);
        if (candidate == null) {
            return completed(store.record(command, nowUtc, null,
                    ExtraordinaryBenefitStatementReplayOutcome.REJECTED_MESSAGE_NOT_FOUND, null));
        }
        if (candidate.status() != ExtraordinaryBenefitStatementOutboxStatus.DEAD_LETTER) {
            return completed(store.record(command, nowUtc, candidate.failureCode(),
                    ExtraordinaryBenefitStatementReplayOutcome.REJECTED_NOT_DEAD_LETTER, null));
        }
        if (!Objects.equals(candidate.failureCode(), command.expectedFailureCode())) {
            return completed(store.record(command, nowUtc, candidate.failureCode(),
                    ExtraordinaryBenefitStatementReplayOutcome.REJECTED_FAILURE_CHANGED, null));
        }
        if (candidate.failedAtUtc() == null
                || candidate.failedAtUtc().isAfter(nowUtc.minus(quarantine))) {
            return completed(store.record(command, nowUtc, candidate.failureCode(),
                    ExtraordinaryBenefitStatementReplayOutcome.REJECTED_QUARANTINE, null));
        }
        var probe = probeProvider.getIfAvailable();
        if (probe == null) {
            return completed(store.record(command, nowUtc, candidate.failureCode(),
                    ExtraordinaryBenefitStatementReplayOutcome.REJECTED_NO_PROBE, null));
        }
        try {
            var acknowledgement = probe.findAcknowledgement(command.messageId());
            if (acknowledgement.isPresent()) {
                if (!command.messageId().equals(acknowledgement.get().messageId())) {
                    return completed(store.record(command, nowUtc, candidate.failureCode(),
                            ExtraordinaryBenefitStatementReplayOutcome.REJECTED_PROBE_FAILED, null));
                }
                return completed(store.acknowledgeWithoutReplay(
                        command, nowUtc, candidate.failureCode(), acknowledgement.get()));
            }
            return completed(store.scheduleReplay(command, nowUtc, quarantine));
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return completed(store.record(command, nowUtc, candidate.failureCode(),
                    ExtraordinaryBenefitStatementReplayOutcome.REJECTED_PROBE_FAILED, null));
        }
    }

    private ExtraordinaryBenefitStatementReplayResult completed(
            ExtraordinaryBenefitStatementReplayResult result) {
        telemetry.replayCompleted(result.outcome());
        return result;
    }
}
