package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitAuthoritativeEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReason;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReevaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioSandboxRunRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.dto.DomainRuleOperationalTestEvidence;
import org.praxisplatform.config.dto.DomainRuleTestRunRecordRequest;
import org.praxisplatform.config.dto.DomainRuleTestRunResultRequest;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;

class PolicyStudioOperationalTestRunExecutorTest {
    private static final String DIGEST = "A".repeat(64);
    private final ExtraordinaryBenefitOperationalProofService proof =
            mock(ExtraordinaryBenefitOperationalProofService.class);
    private final PolicyStudioOperationalTestRunRecorder recorder =
            mock(PolicyStudioOperationalTestRunRecorder.class);
    private final PolicyStudioSandboxService sandbox = mock(PolicyStudioSandboxService.class);
    private final PolicyStudioOperationalTestRunExecutor executor =
            new PolicyStudioOperationalTestRunExecutor(proof, recorder, sandbox);

    @Test
    void evaluatesThenProvesEveryExplicitBindingBeforeDelegatingTheRecord() {
        UUID workspaceId = UUID.randomUUID();
        UUID createId = UUID.randomUUID();
        UUID updateId = UUID.randomUUID();
        DomainRuleTestRunRecordRequest evaluated = run(createId, updateId);
        DomainRuleGovernancePrincipal principal = principal();
        var createEvidence = evidence("CREATE", true);
        var updateEvidence = evidence("UPDATE", false);
        when(proof.proveCreate(
                seed("policy-studio-proof-create"), true, 1, Set.of("benefit:request"),
                "proof-agent", "run:scenario:" + createId)).thenReturn(createEvidence);
        when(proof.proveUpdate(
                seed("policy-studio-proof-update"), update(), false, 1, Set.of("benefit:request"),
                "proof-agent", "run:scenario:" + updateId)).thenReturn(updateEvidence);

        executor.execute(
                workspaceId,
                () -> evaluated,
                List.of(
                        new ExtraordinaryBenefitOperationalScenarioBinding(
                                createId, PolicyStudioOperationalEvidenceAdapter.OperationMode.CREATE,
                                seed("policy-studio-proof-create"), null, true, 1),
                        new ExtraordinaryBenefitOperationalScenarioBinding(
                                updateId, PolicyStudioOperationalEvidenceAdapter.OperationMode.UPDATE,
                                seed("policy-studio-proof-update"), update(), false, 1)),
                Set.of("benefit:request"), "proof-agent", "run", principal);

        verify(recorder).record(eq(workspaceId), eq(evaluated),
                eq(java.util.Map.of(createId, createEvidence, updateId, updateEvidence)), eq(principal));
    }

    @Test
    void refusesPartialBindingsBeforeExecutingAnyOperationalCommand() {
        UUID scenarioId = UUID.randomUUID();
        DomainRuleTestRunRecordRequest evaluated = run(scenarioId);

        assertThatThrownBy(() -> executor.execute(
                        UUID.randomUUID(), () -> evaluated, List.of(), Set.of(), "proof-agent", "run", principal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly every");

        verify(proof, never()).proveCreate(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anySet(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(recorder, never()).record(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyMap(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reusesTheUnpersistedSandboxPreparationForTheOperationalRun() {
        UUID workspaceId = UUID.randomUUID();
        UUID scenarioId = UUID.randomUUID();
        DomainRuleTestRunRecordRequest evaluated = run(scenarioId);
        PolicyStudioSandboxRunRequest request =
                new PolicyStudioSandboxRunRequest(workspaceId, List.of(scenarioId), "UTC");
        when(sandbox.prepare(request, principal())).thenReturn(
                new PolicyStudioSandboxService.PolicyStudioSandboxPreparedRun(
                        workspaceId, evaluated, List.of()));
        DomainRuleOperationalTestEvidence evidence = evidence("CREATE", true);
        when(proof.proveCreate(
                seed("policy-studio-proof-sandbox"), true, 1, Set.of("benefit:request"),
                "proof-agent", "run:scenario:" + scenarioId)).thenReturn(evidence);

        executor.executeSandbox(
                request,
                List.of(new ExtraordinaryBenefitOperationalScenarioBinding(
                        scenarioId, PolicyStudioOperationalEvidenceAdapter.OperationMode.CREATE,
                        seed("policy-studio-proof-sandbox"), null, true, 1)),
                Set.of("benefit:request"), "proof-agent", "run", principal());

        verify(sandbox).prepare(request, principal());
        verify(recorder).record(eq(workspaceId), eq(evaluated),
                eq(java.util.Map.of(scenarioId, evidence)), eq(principal()));
    }

    private DomainRuleOperationalTestEvidence evidence(String mode, boolean mutation) {
        return new DomainRuleOperationalTestEvidence(
                mode, DIGEST, DIGEST, mutation, !mutation, true, DIGEST, 1);
    }

    private DomainRuleTestRunRecordRequest run(UUID... scenarioIds) {
        return new DomainRuleTestRunRecordRequest(
                1L, DIGEST, Instant.parse("2026-08-14T12:00:00Z"), "UTC",
                "snapshot", DIGEST, 2L, null,
                java.util.Arrays.stream(scenarioIds).map(this::result).toList());
    }

    private DomainRuleTestRunResultRequest result(UUID scenarioId) {
        return new DomainRuleTestRunResultRequest(
                scenarioId, "scenario-" + scenarioId, "ALLOW", "ALLOW", null, null,
                List.of(), List.of(), List.of(), List.of(), DIGEST, DIGEST, DIGEST);
    }

    private ExtraordinaryBenefitAuthoritativeEvaluationRequest seed(String reference) {
        return new ExtraordinaryBenefitAuthoritativeEvaluationRequest(
                reference, ExtraordinaryBenefitReason.FAMILY_HARDSHIP, LocalDate.of(2026, 8, 14),
                new BigDecimal("100.00"), "FACT-1", LocalDate.of(2026, 8, 20), "UTC");
    }

    private ExtraordinaryBenefitReevaluationRequest update() {
        return new ExtraordinaryBenefitReevaluationRequest(
                ExtraordinaryBenefitReason.FAMILY_HARDSHIP, LocalDate.of(2026, 8, 14),
                new BigDecimal("120.00"), "FACT-2", LocalDate.of(2026, 8, 20), "UTC");
    }

    private DomainRuleGovernancePrincipal principal() {
        return new DomainRuleGovernancePrincipal("desenv", "proof-agent", "local");
    }
}
