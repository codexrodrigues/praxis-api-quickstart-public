package com.example.praxis.apiquickstart.rulelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationOutcome;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReason;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExtraordinaryBenefitShadowComparisonServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-13T15:00:00Z");
    private ExecutorService executor;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
        meterRegistry = new SimpleMeterRegistry();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        meterRegistry.close();
    }

    @Test
    void classifiesEquivalentAllowAsMatchAndEmitsOnlyLowCardinalityMetrics() {
        var baseline = new ExtraordinaryBenefitSyntheticBaseline();
        var service = service(baseline, baseline, Duration.ofMillis(100));

        var observation = service.compare(eligibleRequest(), Set.of("benefit:request"));

        assertEquals(ExtraordinaryBenefitShadowComparisonStatus.MATCH, observation.comparisonStatus());
        assertTrue(observation.outcomeMatch());
        assertTrue(observation.reasonCodesMatch());
        assertTrue(observation.recommendedAmountMatch());
        assertTrue(observation.plannedEffectMatch());
        assertTrue(observation.sanitized());
        assertFalse(observation.persisted());
        assertFalse(observation.effectExecuted());
        assertEquals(1.0, meterRegistry.get("praxis.rule.shadow.comparisons")
                .tag("result", "match").counter().count());
    }

    @Test
    void detectsOutcomeReasonAmountAndEffectDivergenceWithoutPublishingValues() {
        ExtraordinaryBenefitShadowDecisionEvaluator baseline = new ExtraordinaryBenefitSyntheticBaseline();
        ExtraordinaryBenefitShadowDecisionEvaluator candidate = (request, permissions, now, zone) ->
                ExtraordinaryBenefitShadowDecision.baseline(
                        ExtraordinaryBenefitEvaluationOutcome.DENY,
                        List.of("CANDIDATE_DRIFT"), new BigDecimal("10.00"), "BRL", null, null);

        var observation = service(baseline, candidate, Duration.ofMillis(100))
                .compare(eligibleRequest(), Set.of("benefit:request"));

        assertEquals(ExtraordinaryBenefitShadowComparisonStatus.MISMATCH, observation.comparisonStatus());
        assertFalse(observation.outcomeMatch());
        assertFalse(observation.reasonCodesMatch());
        assertFalse(observation.recommendedAmountMatch());
        assertFalse(observation.plannedEffectMatch());
        assertNull(observation.candidateSnapshotKey());
    }

    @Test
    void preservesInconclusiveAsItsOwnOperationalClass() {
        ExtraordinaryBenefitEvaluationRequest incomplete = withCustomerEligibility(eligibleRequest(), null);
        var baseline = new ExtraordinaryBenefitSyntheticBaseline();

        var observation = service(baseline, baseline, Duration.ofMillis(100))
                .compare(incomplete, Set.of("benefit:request"));

        assertEquals(ExtraordinaryBenefitShadowComparisonStatus.INCONCLUSIVE, observation.comparisonStatus());
        assertEquals(ExtraordinaryBenefitEvaluationOutcome.INCONCLUSIVE, observation.baselineOutcome());
        assertTrue(observation.outcomeMatch());
    }

    @Test
    void isolatesTimeoutAndCandidateFailureAsTechnicalError() {
        ExtraordinaryBenefitShadowDecisionEvaluator blocking = (request, permissions, now, zone) -> {
            while (!Thread.currentThread().isInterrupted()) {
                LockSupport.parkNanos(Duration.ofSeconds(1).toNanos());
            }
            return null;
        };
        ExtraordinaryBenefitShadowDecisionEvaluator failing = (request, permissions, now, zone) -> {
            throw new IllegalStateException("sensitive failure must not escape");
        };

        var observation = service(blocking, failing, Duration.ofMillis(20))
                .compare(eligibleRequest(), Set.of("benefit:request"));

        assertEquals(ExtraordinaryBenefitShadowComparisonStatus.TECHNICAL_ERROR, observation.comparisonStatus());
        assertEquals(ExtraordinaryBenefitShadowSideStatus.TIMEOUT, observation.baselineStatus());
        assertEquals(ExtraordinaryBenefitShadowSideStatus.ERROR, observation.candidateStatus());
        assertNull(observation.baselineOutcome());
        assertNull(observation.candidateOutcome());
    }

    @Test
    void convertsExecutorSaturationIntoSanitizedTechnicalError() {
        var baseline = new ExtraordinaryBenefitSyntheticBaseline();
        var service = service(baseline, baseline, Duration.ofMillis(100));
        executor.shutdownNow();

        var observation = service.compare(eligibleRequest(), Set.of("benefit:request"));

        assertEquals(ExtraordinaryBenefitShadowComparisonStatus.TECHNICAL_ERROR, observation.comparisonStatus());
        assertEquals(ExtraordinaryBenefitShadowSideStatus.ERROR, observation.baselineStatus());
        assertEquals(ExtraordinaryBenefitShadowSideStatus.ERROR, observation.candidateStatus());
        assertTrue(observation.sanitized());
    }

    private ExtraordinaryBenefitShadowComparisonService service(
            ExtraordinaryBenefitShadowDecisionEvaluator baseline,
            ExtraordinaryBenefitShadowDecisionEvaluator candidate,
            Duration timeout) {
        return new ExtraordinaryBenefitShadowComparisonService(
                baseline, candidate, executor, meterRegistry,
                Clock.fixed(NOW, ZoneOffset.UTC), timeout);
    }

    private ExtraordinaryBenefitEvaluationRequest eligibleRequest() {
        return new ExtraordinaryBenefitEvaluationRequest(
                "BEN-PII-NEVER-OBSERVED",
                ExtraordinaryBenefitReason.FAMILY_HARDSHIP,
                LocalDate.parse("2026-07-13"),
                new BigDecimal("2500.00"),
                "ACTIVE", false, true, new BigDecimal("5000.00"), true,
                LocalDate.parse("2026-07-20"),
                List.of(LocalDate.parse("2026-07-20"), LocalDate.parse("2026-08-05")),
                new BigDecimal("100000.00"),
                "America/Sao_Paulo");
    }

    private ExtraordinaryBenefitEvaluationRequest withCustomerEligibility(
            ExtraordinaryBenefitEvaluationRequest source,
            Boolean eligibility) {
        return new ExtraordinaryBenefitEvaluationRequest(
                source.requestReference(), source.reasonCode(), source.eventDate(), source.requestedAmount(),
                source.workerStatus(), source.duplicateGrant(), source.programActive(),
                source.programMaximumAmount(), eligibility, source.requestedPaymentDate(),
                source.allowedPaymentDates(), source.availableBudgetAmount(), source.userTimeZone());
    }
}
