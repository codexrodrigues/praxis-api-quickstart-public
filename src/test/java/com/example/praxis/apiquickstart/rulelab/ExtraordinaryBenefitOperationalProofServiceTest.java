package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitAuthoritativeEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationCommandResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReevaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReason;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExtraordinaryBenefitOperationalProofServiceTest {
    private static final String REFERENCE = "policy-studio-proof-run-1";
    private static final String EMPTY = "0".repeat(64);
    private static final String SEEDED = "1".repeat(64);
    private static final String UPDATED = "2".repeat(64);
    private static final String LEDGER = "3".repeat(64);

    private final ExtraordinaryBenefitWorkflowService workflow = mock(ExtraordinaryBenefitWorkflowService.class);
    private final ExtraordinaryBenefitOperationalEvidenceProbe probe =
            mock(ExtraordinaryBenefitOperationalEvidenceProbe.class);
    private final ExtraordinaryBenefitOperationalProofService service =
            new ExtraordinaryBenefitOperationalProofService(
                    workflow, probe, new PolicyStudioOperationalEvidenceAdapter());

    @Test
    void orchestratesUpdateThroughTheAuthoritativeWorkflowAndCleansTheSeed() {
        ExtraordinaryBenefitAuthoritativeEvaluationRequest seed = seed();
        ExtraordinaryBenefitReevaluationRequest update = update();
        ExtraordinaryBenefitRequestResponse resource = new ExtraordinaryBenefitRequestResponse(
                42L, null, null, null, null, 0L, null, null, null, null);
        ExtraordinaryBenefitEvaluationCommandResponse seeded =
                new ExtraordinaryBenefitEvaluationCommandResponse(null, resource);
        when(workflow.evaluateAndPersist(seed, Set.of("evaluate"), "proof-agent", "run-1:seed"))
                .thenReturn(seeded);
        when(probe.capture(REFERENCE)).thenReturn(
                state(EMPTY, EMPTY), state(SEEDED, EMPTY), state(UPDATED, LEDGER), state(EMPTY, EMPTY));

        var evidence = service.proveUpdate(
                seed, update, true, 1, Set.of("evaluate"), "proof-agent", "run-1");

        assertThat(evidence.operationMode()).isEqualTo("UPDATE");
        assertThat(evidence.mutationObserved()).isTrue();
        assertThat(evidence.cleanupVerified()).isTrue();
        verify(workflow).reEvaluate(42L, update, Set.of("evaluate"), "proof-agent", "run-1");
        verify(probe, times(2)).cleanup(REFERENCE);
    }

    @Test
    void cleansUpAndRefusesUpdateWhenTheSeedWasNotPersisted() {
        ExtraordinaryBenefitAuthoritativeEvaluationRequest seed = seed();
        ExtraordinaryBenefitReevaluationRequest update = update();
        when(probe.capture(REFERENCE)).thenReturn(state(EMPTY, EMPTY));
        when(workflow.evaluateAndPersist(seed, Set.of(), "proof-agent", "run-2:seed"))
                .thenReturn(new ExtraordinaryBenefitEvaluationCommandResponse(null, null));

        assertThatThrownBy(() -> service.proveUpdate(
                        seed, update, true, 0, Set.of(), "proof-agent", "run-2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ALLOW seed");

        verify(probe).cleanup(REFERENCE);
    }

    private PolicyStudioOperationalEvidenceAdapter.OperationalState state(String state, String ledger) {
        return new PolicyStudioOperationalEvidenceAdapter.OperationalState(state, ledger);
    }

    private ExtraordinaryBenefitAuthoritativeEvaluationRequest seed() {
        return new ExtraordinaryBenefitAuthoritativeEvaluationRequest(
                REFERENCE, ExtraordinaryBenefitReason.EMERGENCY_MEDICAL, LocalDate.of(2026, 8, 14),
                new BigDecimal("100.00"), "FACT-PROOF-1", LocalDate.of(2026, 8, 20), "UTC");
    }

    private ExtraordinaryBenefitReevaluationRequest update() {
        return new ExtraordinaryBenefitReevaluationRequest(
                ExtraordinaryBenefitReason.EMERGENCY_MEDICAL, LocalDate.of(2026, 8, 14),
                new BigDecimal("120.00"), "FACT-PROOF-2", LocalDate.of(2026, 8, 20), "UTC");
    }
}
