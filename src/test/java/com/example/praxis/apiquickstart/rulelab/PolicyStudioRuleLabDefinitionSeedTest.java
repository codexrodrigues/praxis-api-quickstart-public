package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.praxisplatform.config.dto.DomainRuleDefinitionRequest;
import org.praxisplatform.config.dto.DomainRuleDefinitionResponse;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleService;

class PolicyStudioRuleLabDefinitionSeedTest {

    @Test
    void seedsOnlyEditableJsonLogicBindingsThroughTheGovernedService() {
        DomainRuleService service = mock(DomainRuleService.class);
        when(service.definitions(anyString(), anyString(), any(), any(), any(), anyString()))
                .thenReturn(List.of());
        var seed = new PolicyStudioRuleLabDefinitionSeed();

        seed.seed(service, new ObjectMapper(), "tenant-a", "uat");

        ArgumentCaptor<DomainRuleDefinitionRequest> requests =
                ArgumentCaptor.forClass(DomainRuleDefinitionRequest.class);
        ArgumentCaptor<DomainRuleGovernancePrincipal> principals =
                ArgumentCaptor.forClass(DomainRuleGovernancePrincipal.class);
        verify(service, times(7)).createDefinition(requests.capture(), principals.capture());
        assertThat(principals.getAllValues()).allSatisfy(principal -> {
            assertThat(principal.tenantId()).isEqualTo("tenant-a");
            assertThat(principal.environment()).isEqualTo("uat");
            assertThat(principal.actorRef()).isEqualTo("policy-studio-quickstart-seed");
        });
        assertThat(requests.getAllValues()).extracting(DomainRuleDefinitionRequest::ruleKey)
                .containsExactly(
                        "request.authorization-integrity",
                        "worker.legal-eligibility",
                        "grant.duplicate-conflict",
                        "program.applicability",
                        "payment.calendar-policy",
                        "grant.amount-parameters",
                        "budget.availability");
        assertThat(requests.getAllValues()).allSatisfy(request -> {
            assertThat(request.ruleType()).isEqualTo("selection_eligibility");
            assertThat(request.status()).isEqualTo("draft");
            assertThat(request.condition()).isNotNull();
            assertThat(request.parameters().path("hostContractVersion").asText()).isEqualTo("quickstart/1.0");
            assertThat(request.governance().path("requiredApprovals").get(0).asText())
                    .isEqualTo("policy-owner");
            assertThat(request.governance().path("authorizedApprovers").get(0).asText())
                    .isEqualTo("policy-owner");
            assertThat(request.governance().path("authorityChangeAllowed").asBoolean()).isFalse();
        });
    }

    @Test
    void doesNotCreateAnotherVersionWhenTheGovernedRuleKeyAlreadyExists() {
        DomainRuleService service = mock(DomainRuleService.class);
        when(service.definitions(anyString(), anyString(), any(), any(), any(), anyString()))
                .thenReturn(List.of(existingDefinition()));

        new PolicyStudioRuleLabDefinitionSeed().seed(service, new ObjectMapper(), "desenv", "local");

        verify(service, times(0)).createDefinition(
                any(DomainRuleDefinitionRequest.class), any(DomainRuleGovernancePrincipal.class));
    }

    @Test
    void rejectsAnUnscopedDeploymentInsteadOfCreatingWildcardDefinitions() {
        DomainRuleService service = mock(DomainRuleService.class);

        assertThatThrownBy(() ->
                        new PolicyStudioRuleLabDefinitionSeed()
                                .seed(service, new ObjectMapper(), " ", "local"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenant-id");

        verify(service, times(0)).createDefinition(
                any(DomainRuleDefinitionRequest.class), any(DomainRuleGovernancePrincipal.class));
    }

    private static DomainRuleDefinitionResponse existingDefinition() {
        return new DomainRuleDefinitionResponse(
                null, "desenv", "local", "request.authorization-integrity", 1,
                "JSON_LOGIC", "draft", null, null, null, null, null,
                null, null, null, null, null, null, null,
                "SYSTEM", "policy-studio-quickstart-seed", null,
                null, null, null, null);
    }
}
