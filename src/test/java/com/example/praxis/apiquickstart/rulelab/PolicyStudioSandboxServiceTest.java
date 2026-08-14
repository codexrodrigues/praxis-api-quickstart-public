package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioSandboxRunRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.praxisplatform.config.dto.DomainRuleChangeWorkspaceResponse;
import org.praxisplatform.config.dto.DomainRuleTestScenarioResponse;
import org.praxisplatform.config.service.DomainRuleChangeWorkspaceService;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleTestRunService;
import org.praxisplatform.config.dto.DomainRuleTestRunResponse;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.springframework.web.server.ResponseStatusException;

class PolicyStudioSandboxServiceTest {
    private static final UUID WORKSPACE_ID = UUID.randomUUID();
    private static final UUID SCENARIO_ID = UUID.randomUUID();
    private static final DomainRuleGovernancePrincipal PRINCIPAL =
            new DomainRuleGovernancePrincipal("desenv", "policy-author", "local");
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
    private final ObjectMapper json = new ObjectMapper();
    private DomainRuleChangeWorkspaceService workspaceService;
    private PolicyStudioSandboxService service;
    private DomainRuleTestRunService testRunService;

    @BeforeEach
    void setUp() {
        workspaceService = mock(DomainRuleChangeWorkspaceService.class);
        testRunService = mock(DomainRuleTestRunService.class);
        when(testRunService.record(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            var request = (org.praxisplatform.config.dto.DomainRuleTestRunRecordRequest) invocation.getArgument(1);
            return new DomainRuleTestRunResponse(
                    UUID.randomUUID(), WORKSPACE_ID, request.workspaceRevision(), request.baseDefinitionHash(),
                    request.evaluatedAtUtc(), request.userTimeZone(), request.activeSnapshotKey(),
                    request.activeSnapshotContentHash(), request.activeActivationRevision(), List.of(),
                    "policy-author", NOW);
        });
        RuleBindingExecutorRegistry registry =
                new ExtraordinaryGrantRuleLabConfiguration().extraordinaryGrantRuleExecutorRegistry();
        ExtraordinaryGrantRuleSnapshotRuntime runtime = new ExtraordinaryGrantRuleSnapshotRuntime(
                registry,
                new ExtraordinaryGrantRuleRuntimeTelemetry(
                        new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        service = new PolicyStudioSandboxService(
                workspaceService,
                new ExtraordinaryGrantRuleLabService(runtime),
                runtime,
                testRunService,
                registry,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void evaluatesCandidateButKeepsMissingActiveSnapshotAsTechnicalError() throws Exception {
        stub("grant.amount-parameters", json.readTree(
                "{\"<=\":[{\"var\":\"request.requestedAmount\"},{\"var\":\"program.maxAmount\"}]}"),
                "ALLOW", completeFacts());

        var response = service.run(
                new PolicyStudioSandboxRunRequest(WORKSPACE_ID, List.of(), "America/Sao_Paulo"), PRINCIPAL);

        assertThat(response.evaluatedAtUtc()).isEqualTo(NOW);
        assertThat(response.activeSnapshotKey()).isNull();
        assertThat(response.results()).singleElement().satisfies(result -> {
            assertThat(result.candidateDecision()).isEqualTo("ALLOW");
            assertThat(result.activeDecision()).isEqualTo("TECHNICAL_ERROR");
            assertThat(result.comparison()).isEqualTo("TECHNICAL_ERROR");
            assertThat(result.candidateMatchesExpected()).isTrue();
            assertThat(result.activeMatchesExpected()).isFalse();
        });
        var request = ArgumentCaptor.forClass(
                org.praxisplatform.config.dto.DomainRuleTestRunRecordRequest.class);
        verify(testRunService).record(org.mockito.ArgumentMatchers.eq(WORKSPACE_ID), request.capture(),
                org.mockito.ArgumentMatchers.eq(PRINCIPAL));
        assertThat(request.getValue().baselineEvidence()).satisfies(evidence -> {
            assertThat(evidence.authorityType()).isEqualTo("SYNTHETIC_EXPECTED");
            assertThat(evidence.artifactRef()).contains(WORKSPACE_ID.toString());
            assertThat(evidence.artifactDigest()).matches("[A-F0-9]{64}");
            assertThat(evidence.eligibility()).isEqualTo("ELIGIBLE");
        });
    }

    @Test
    void preparesTheIdenticalCandidateActiveRecordWithoutPersistingIt() throws Exception {
        stub("grant.amount-parameters", json.readTree(
                "{\"<=\":[{\"var\":\"request.requestedAmount\"},{\"var\":\"program.maxAmount\"}]}"),
                "ALLOW", completeFacts());

        var prepared = service.prepare(
                new PolicyStudioSandboxRunRequest(WORKSPACE_ID, List.of(SCENARIO_ID), "UTC"), PRINCIPAL);

        assertThat(prepared.workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(prepared.recordRequest().results()).singleElement().satisfies(result -> {
            assertThat(result.scenarioId()).isEqualTo(SCENARIO_ID);
            assertThat(result.candidateDecision()).isEqualTo("ALLOW");
            assertThat(result.activeDecision()).isEqualTo("TECHNICAL_ERROR");
        });
        verify(testRunService, never()).record(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void replacesOnlyTheGovernedBindingAndProducesBusinessDenial() throws Exception {
        stub("grant.amount-parameters", json.readTree("{\"===\":[true,false]}"),
                "DENY", completeFacts());

        var result = service.run(
                new PolicyStudioSandboxRunRequest(WORKSPACE_ID, List.of(SCENARIO_ID), "UTC"), PRINCIPAL)
                .results().get(0);

        assertThat(result.candidateDecision()).isEqualTo("DENY");
        assertThat(result.candidateReasonCodes()).contains("REQUESTED_AMOUNT_EXCEEDS_PROGRAM_LIMIT");
        assertThat(result.candidateMatchesExpected()).isTrue();
    }

    @Test
    void provesOutputReasonsAndPlannedEffectsWithoutExecutingAnEffect() throws Exception {
        var expectedOutput = json.readTree("""
                {"grant.effect-plan":{"intentType":"REGISTER_EXTRAORDINARY_GRANT",
                "operationKey":"evaluate-extraordinary-grant","status":"PLANNED_NOT_EXECUTED"}}
                """);
        stub("grant.amount-parameters", json.readTree(
                "{\"<=\":[{\"var\":\"request.requestedAmount\"},{\"var\":\"program.maxAmount\"}]}"),
                "ALLOW", completeFacts(), expectedOutput, List.of(), List.of("REGISTER_EXTRAORDINARY_GRANT"));

        var result = service.run(
                new PolicyStudioSandboxRunRequest(WORKSPACE_ID, List.of(), "UTC"), PRINCIPAL)
                .results().getFirst();

        assertThat(result.candidateOutputMatchesExpected()).isTrue();
        assertThat(result.candidateReasonCodesMatchExpected()).isTrue();
        assertThat(result.candidateEffectsMatchExpected()).isTrue();
        assertThat(result.candidateEffectIntents()).containsExactly("REGISTER_EXTRAORDINARY_GRANT");
    }

    @Test
    void rejectsWorkspaceRuleThatDoesNotBelongToTheHostRuleSet() throws Exception {
        stub("ergon.only.rule", json.readTree("{\"===\":[true,true]}"), "ALLOW", completeFacts());

        assertThatThrownBy(() -> service.run(
                new PolicyStudioSandboxRunRequest(WORKSPACE_ID, List.of(), "UTC"), PRINCIPAL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not a replaceable binding");
    }

    private void stub(String ruleKey, com.fasterxml.jackson.databind.JsonNode condition,
                      String expected, com.fasterxml.jackson.databind.JsonNode facts) {
        stub(ruleKey, condition, expected, facts, null, List.of(), List.of());
    }

    private void stub(String ruleKey, com.fasterxml.jackson.databind.JsonNode condition,
                      String expected, com.fasterxml.jackson.databind.JsonNode facts,
                      com.fasterxml.jackson.databind.JsonNode expectedOutput,
                      List<String> expectedReasonCodes, List<String> expectedEffectIntents) {
        var workspace = new DomainRuleChangeWorkspaceResponse(
                WORKSPACE_ID, ruleKey, UUID.randomUUID(), 1, "A".repeat(64), null, "Candidate", "OPEN",
                condition, json.createObjectNode(), "test", 2L, UUID.randomUUID().toString(),
                "author", "author", NOW, NOW);
        var scenario = new DomainRuleTestScenarioResponse(
                SCENARIO_ID, WORKSPACE_ID, "happy-path", "Happy path", facts, expected, expectedOutput,
                expectedReasonCodes, expectedEffectIntents,
                "ACTIVE", 1L, UUID.randomUUID().toString(), "author", "author", NOW, NOW);
        when(workspaceService.get(WORKSPACE_ID, PRINCIPAL)).thenReturn(workspace);
        when(workspaceService.scenarios(WORKSPACE_ID, PRINCIPAL)).thenReturn(List.of(scenario));
    }

    private com.fasterxml.jackson.databind.JsonNode completeFacts() throws Exception {
        return json.readTree("""
                {
                  "actor":{"permissions":["benefit:request"]},
                  "worker":{"status":"ACTIVE"},
                  "grant":{"hasDuplicate":false},
                  "program":{"active":true,"maxAmount":1000},
                  "customer":{"additionalEligible":true},
                  "payment":{"requestedDate":"2026-08-20","allowedDates":["2026-08-20"]},
                  "request":{"requestedAmount":500},
                  "budget":{"availableAmount":2000}
                }
                """);
    }
}
