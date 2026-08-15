package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.core.entity.ResourceActionExecution;
import com.example.praxis.apiquickstart.core.service.ResourceActionExecutionService;
import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioOperationalRunRequest;
import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioOperationalScenarioSelection;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.praxisplatform.config.contract.DomainRuleOperationalTestEvidence;
import org.praxisplatform.config.contract.DomainRuleTestRunResponse;
import org.praxisplatform.config.contract.DomainRuleTestRunResultResponse;
import org.praxisplatform.config.dto.DomainRuleChangeWorkspaceResponse;
import org.praxisplatform.config.service.DomainRuleChangeWorkspaceService;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PolicyStudioOperationalTestRunCommandServiceTest {
    private static final UUID WORKSPACE_ID = UUID.randomUUID();
    private static final UUID SCENARIO_ID = UUID.randomUUID();
    private static final String ETAG = UUID.randomUUID().toString();
    private static final DomainRuleGovernancePrincipal PRINCIPAL =
            new DomainRuleGovernancePrincipal("desenv", "proof-operator", "local");
    private final DomainRuleChangeWorkspaceService workspaces =
            mock(DomainRuleChangeWorkspaceService.class);
    private final PolicyStudioOperationalTestRunPort executor =
            mock(PolicyStudioOperationalTestRunPort.class);
    private final ExtraordinaryBenefitOperationalScenarioBindingFactory bindings =
            mock(ExtraordinaryBenefitOperationalScenarioBindingFactory.class);
    private final ResourceActionExecutionService executions =
            mock(ResourceActionExecutionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final PolicyStudioOperationalTestRunCommandService service =
            new PolicyStudioOperationalTestRunCommandService(
                    workspaces, executor, bindings, executions, objectMapper);

    @Test
    void requiresStrongWorkspaceEtagBeforeCallingTheOperationalExecutor() {
        when(workspaces.get(WORKSPACE_ID, PRINCIPAL)).thenReturn(workspace());

        assertThatThrownBy(() -> service.execute(
                        request(), null, "run-1", Set.of(), "proof-operator", "correlation", PRINCIPAL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(failure -> assertThat(((ResponseStatusException) failure).getStatusCode())
                        .isEqualTo(HttpStatus.PRECONDITION_REQUIRED));
        assertThatThrownBy(() -> service.execute(
                        request(), "\"stale\"", "run-1", Set.of(), "proof-operator", "correlation", PRINCIPAL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(failure -> assertThat(((ResponseStatusException) failure).getStatusCode())
                        .isEqualTo(HttpStatus.PRECONDITION_FAILED));

        verify(executor, never()).executeSandbox(any(), any(), anySet(), any(), any(), any());
    }

    @Test
    void delegatesOnlyScenarioIdentityAndOperationWhileAddingTheBoundedPilotPermission() {
        when(workspaces.get(WORKSPACE_ID, PRINCIPAL)).thenReturn(workspace());
        DomainRuleTestRunResponse receipt = new DomainRuleTestRunResponse(
                UUID.randomUUID(), WORKSPACE_ID, "run-1", "A".repeat(64), 1L, "A".repeat(64),
                Instant.parse("2026-08-14T12:00:00Z"), "UTC", null, null, 0L,
                null, List.of(receiptResult("CREATE")), "proof-operator", Instant.parse("2026-08-14T12:00:01Z"));
        when(executor.existingSandbox(any(), any())).thenReturn(Optional.empty());
        ResourceActionExecution execution = mock(ResourceActionExecution.class);
        when(executions.reserve(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(execution));
        when(executor.executeSandbox(any(), any(), anySet(), any(), any(), any())).thenReturn(receipt);

        var result = service.execute(
                request(), "\"" + ETAG + "\"", "run-1", Set.of("ROLE_RULE_OPERATIONAL_TEST_OPERATOR"),
                "proof-operator", "correlation", PRINCIPAL);

        assertThat(result.run()).isSameAs(receipt);
        assertThat(result.workspaceEtag()).isEqualTo(ETAG);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> permissions = ArgumentCaptor.forClass(Set.class);
        verify(executor).executeSandbox(
                any(), any(), permissions.capture(),
                org.mockito.ArgumentMatchers.eq("proof-operator"),
                org.mockito.ArgumentMatchers.eq("correlation"),
                org.mockito.ArgumentMatchers.eq(PRINCIPAL));
        assertThat(permissions.getValue())
                .containsExactlyInAnyOrder("ROLE_RULE_OPERATIONAL_TEST_OPERATOR", "benefit:request");
        verify(executions).complete(execution, receipt);
    }

    @Test
    void rejectsAReplayReceiptRecordedWithDifferentOperationalModes() {
        when(workspaces.get(WORKSPACE_ID, PRINCIPAL)).thenReturn(workspace());
        DomainRuleTestRunResponse receipt = new DomainRuleTestRunResponse(
                UUID.randomUUID(), WORKSPACE_ID, "run-1", "A".repeat(64), 1L, "A".repeat(64),
                Instant.parse("2026-08-14T12:00:00Z"), "UTC", null, null, 0L,
                null, List.of(receiptResult("UPDATE")), "proof-operator", Instant.parse("2026-08-14T12:00:01Z"));
        when(executor.existingSandbox(any(), any())).thenReturn(Optional.of(receipt));

        assertThatThrownBy(() -> service.execute(
                        request(), "\"" + ETAG + "\"", "run-1", Set.of(),
                        "proof-operator", "correlation", PRINCIPAL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(failure -> assertThat(((ResponseStatusException) failure).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(executions, never()).reserve(any(), any(), any(), any(), any(), any(), any(), any());
    }

    private PolicyStudioOperationalRunRequest request() {
        return new PolicyStudioOperationalRunRequest(
                WORKSPACE_ID,
                List.of(new PolicyStudioOperationalScenarioSelection(SCENARIO_ID, "CREATE")),
                "UTC",
                Instant.parse("2026-08-14T12:00:00Z"));
    }

    private DomainRuleTestRunResultResponse receiptResult(String operationMode) {
        String digest = "A".repeat(64);
        return new DomainRuleTestRunResultResponse(
                SCENARIO_ID, "scenario", "ALLOW", "ALLOW", "ALLOW", "MATCH", true, true,
                null, null, null, true, true,
                List.of(), List.of(), List.of(), true, true,
                List.of(), List.of(), List.of(), true, true,
                digest, digest, digest, null, null, true, true, true, true,
                new DomainRuleOperationalTestEvidence(
                        operationMode,
                        "UPDATE".equals(operationMode) ? digest : null,
                        digest, true, false, true, digest, 0));
    }

    private DomainRuleChangeWorkspaceResponse workspace() {
        return new DomainRuleChangeWorkspaceResponse(
                WORKSPACE_ID, "grant.amount-parameters", UUID.randomUUID(), 1,
                "A".repeat(64), null, null, "Workspace", "OPEN", null, null, null,
                1L, ETAG, "author", "author",
                Instant.parse("2026-08-14T12:00:00Z"), Instant.parse("2026-08-14T12:00:00Z"));
    }
}
