package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationOutcome;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitShadowObservation;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Orquestra dual-run limitado e publica apenas evidencia operacional sanitizada. */
final class ExtraordinaryBenefitShadowComparisonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtraordinaryBenefitShadowComparisonService.class);

    private final ExtraordinaryBenefitShadowDecisionEvaluator baselineEvaluator;
    private final ExtraordinaryBenefitShadowDecisionEvaluator candidateEvaluator;
    private final ExecutorService executor;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final Duration timeout;

    ExtraordinaryBenefitShadowComparisonService(
            ExtraordinaryBenefitShadowDecisionEvaluator baselineEvaluator,
            ExtraordinaryBenefitShadowDecisionEvaluator candidateEvaluator,
            ExecutorService executor,
            MeterRegistry meterRegistry,
            Clock clock,
            Duration timeout) {
        this.baselineEvaluator = Objects.requireNonNull(baselineEvaluator, "baselineEvaluator is required");
        this.candidateEvaluator = Objects.requireNonNull(candidateEvaluator, "candidateEvaluator is required");
        this.executor = Objects.requireNonNull(executor, "executor is required");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.timeout = Objects.requireNonNull(timeout, "timeout is required");
        if (timeout.isZero() || timeout.isNegative() || timeout.compareTo(Duration.ofSeconds(5)) > 0) {
            throw new IllegalArgumentException("timeout must be between 1 ms and 5 seconds");
        }
    }

    ExtraordinaryBenefitShadowObservation compare(
            ExtraordinaryBenefitEvaluationRequest request,
            Set<String> actorPermissions) {
        Objects.requireNonNull(request, "request is required");
        Instant observedAt = clock.instant();
        ZoneId userTimeZone = ZoneId.of(request.userTimeZone());
        Set<String> permissions = actorPermissions == null ? Set.of() : Set.copyOf(actorPermissions);
        long timeoutNanos = timeout.toNanos();
        long deadlineNanos = System.nanoTime() + timeoutNanos;

        Future<SideExecution> baselineFuture = submit(
                baselineEvaluator, request, permissions, observedAt, userTimeZone);
        Future<SideExecution> candidateFuture = submit(
                candidateEvaluator, request, permissions, observedAt, userTimeZone);

        SideExecution baseline = await(baselineFuture, deadlineNanos, timeoutNanos);
        SideExecution candidate = await(candidateFuture, deadlineNanos, timeoutNanos);
        recordSide("baseline", baseline);
        recordSide("candidate", candidate);

        Comparison comparison = compare(baseline, candidate);
        Counter.builder("praxis.rule.shadow.comparisons")
                .description("Sanitized extraordinary-benefit shadow comparisons")
                .tag("result", tag(comparison.status()))
                .tag("baseline_status", tag(baseline.status()))
                .tag("candidate_status", tag(candidate.status()))
                .register(meterRegistry)
                .increment();

        String observationId = UUID.randomUUID().toString();
        ExtraordinaryBenefitShadowDecision baselineDecision = baseline.decision();
        ExtraordinaryBenefitShadowDecision candidateDecision = candidate.decision();
        LOGGER.info(
                "rule_shadow observationId={} comparison={} baselineStatus={} candidateStatus={} baselineOutcome={} candidateOutcome={} candidateSnapshotKey={}",
                observationId, comparison.status(), baseline.status(), candidate.status(),
                outcome(baselineDecision), outcome(candidateDecision),
                candidateDecision == null ? null : candidateDecision.snapshotKey());

        return new ExtraordinaryBenefitShadowObservation(
                observationId,
                observedAt,
                comparison.status(),
                baseline.status(),
                candidate.status(),
                outcome(baselineDecision),
                outcome(candidateDecision),
                comparison.outcomeMatch(),
                comparison.reasonCodesMatch(),
                comparison.amountMatch(),
                comparison.effectMatch(),
                candidateDecision == null ? null : candidateDecision.snapshotKey(),
                candidateDecision == null ? null : candidateDecision.snapshotContentHash(),
                true,
                false,
                false);
    }

    private Future<SideExecution> submit(
            ExtraordinaryBenefitShadowDecisionEvaluator evaluator,
            ExtraordinaryBenefitEvaluationRequest request,
            Set<String> permissions,
            Instant observedAt,
            ZoneId userTimeZone) {
        try {
            return executor.submit(() -> execute(evaluator, request, permissions, observedAt, userTimeZone));
        } catch (RejectedExecutionException saturated) {
            return CompletableFuture.completedFuture(
                    new SideExecution(ExtraordinaryBenefitShadowSideStatus.ERROR, null, 0));
        }
    }

    private SideExecution execute(
            ExtraordinaryBenefitShadowDecisionEvaluator evaluator,
            ExtraordinaryBenefitEvaluationRequest request,
            Set<String> permissions,
            Instant observedAt,
            ZoneId userTimeZone) {
        long startedAt = System.nanoTime();
        try {
            return new SideExecution(
                    ExtraordinaryBenefitShadowSideStatus.SUCCESS,
                    evaluator.evaluate(request, permissions, observedAt, userTimeZone),
                    System.nanoTime() - startedAt);
        } catch (RuntimeException failure) {
            return new SideExecution(
                    ExtraordinaryBenefitShadowSideStatus.ERROR,
                    null,
                    System.nanoTime() - startedAt);
        }
    }

    private SideExecution await(Future<SideExecution> future, long deadlineNanos, long timeoutNanos) {
        try {
            if (future.isDone()) {
                return future.get();
            }
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                future.cancel(true);
                return SideExecution.timeout(timeoutNanos);
            }
            return future.get(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (TimeoutException timeoutFailure) {
            future.cancel(true);
            return SideExecution.timeout(timeoutNanos);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return new SideExecution(ExtraordinaryBenefitShadowSideStatus.ERROR, null, timeoutNanos);
        } catch (ExecutionException unexpected) {
            return new SideExecution(ExtraordinaryBenefitShadowSideStatus.ERROR, null, timeoutNanos);
        }
    }

    private Comparison compare(SideExecution baseline, SideExecution candidate) {
        ExtraordinaryBenefitShadowDecision left = baseline.decision();
        ExtraordinaryBenefitShadowDecision right = candidate.decision();
        boolean outcomeMatch = left != null && right != null && left.outcome() == right.outcome();
        boolean reasonsMatch = left != null && right != null && left.reasonCodes().equals(right.reasonCodes());
        boolean amountMatch = left != null && right != null
                && equalAmount(left.recommendedAmount(), right.recommendedAmount())
                && Objects.equals(left.currency(), right.currency());
        boolean effectMatch = left != null && right != null
                && Objects.equals(left.plannedEffectIntent(), right.plannedEffectIntent())
                && Objects.equals(left.plannedEffectStatus(), right.plannedEffectStatus());

        ExtraordinaryBenefitShadowComparisonStatus status;
        if (baseline.status() != ExtraordinaryBenefitShadowSideStatus.SUCCESS
                || candidate.status() != ExtraordinaryBenefitShadowSideStatus.SUCCESS
                || technical(left) || technical(right)) {
            status = ExtraordinaryBenefitShadowComparisonStatus.TECHNICAL_ERROR;
        } else if (left.outcome() == ExtraordinaryBenefitEvaluationOutcome.INCONCLUSIVE
                || right.outcome() == ExtraordinaryBenefitEvaluationOutcome.INCONCLUSIVE) {
            status = ExtraordinaryBenefitShadowComparisonStatus.INCONCLUSIVE;
        } else if (outcomeMatch && reasonsMatch && amountMatch && effectMatch) {
            status = ExtraordinaryBenefitShadowComparisonStatus.MATCH;
        } else {
            status = ExtraordinaryBenefitShadowComparisonStatus.MISMATCH;
        }
        return new Comparison(status, outcomeMatch, reasonsMatch, amountMatch, effectMatch);
    }

    private void recordSide(String side, SideExecution execution) {
        Timer.builder("praxis.rule.shadow.side.duration")
                .description("Bounded duration of each extraordinary-benefit shadow evaluator")
                .tag("side", side)
                .tag("status", tag(execution.status()))
                .register(meterRegistry)
                .record(execution.durationNanos(), TimeUnit.NANOSECONDS);
    }

    private boolean technical(ExtraordinaryBenefitShadowDecision decision) {
        return decision != null && decision.outcome() == ExtraordinaryBenefitEvaluationOutcome.TECHNICAL_ERROR;
    }

    private ExtraordinaryBenefitEvaluationOutcome outcome(ExtraordinaryBenefitShadowDecision decision) {
        return decision == null ? null : decision.outcome();
    }

    private boolean equalAmount(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    private String tag(Enum<?> value) {
        return value.name().toLowerCase(java.util.Locale.ROOT);
    }

    private record SideExecution(
            ExtraordinaryBenefitShadowSideStatus status,
            ExtraordinaryBenefitShadowDecision decision,
            long durationNanos) {
        private static SideExecution timeout(long timeoutNanos) {
            return new SideExecution(ExtraordinaryBenefitShadowSideStatus.TIMEOUT, null, timeoutNanos);
        }
    }

    private record Comparison(
            ExtraordinaryBenefitShadowComparisonStatus status,
            boolean outcomeMatch,
            boolean reasonCodesMatch,
            boolean amountMatch,
            boolean effectMatch) {
    }
}
