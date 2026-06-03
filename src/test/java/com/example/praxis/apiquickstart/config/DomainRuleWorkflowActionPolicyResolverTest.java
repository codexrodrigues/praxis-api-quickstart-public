package com.example.praxis.apiquickstart.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.config.dto.DomainRuleMaterializationResponse;
import org.praxisplatform.config.service.DomainRuleService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainRuleWorkflowActionPolicyResolverTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private ObjectProvider<DomainRuleService> domainRuleServiceProvider;

    @Mock
    private DomainRuleService domainRuleService;

    @Test
    void shouldResolveLatestAppliedWorkflowActionPolicy() throws Exception {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(domainRuleService);
        when(domainRuleService.materializations(
                "default",
                "dev",
                null,
                "workflow_action",
                "resource-workflow-action",
                "human-resources.folhas-pagamento:mark-paid",
                "applied"))
                .thenReturn(List.of(
                        materialization("older message", Instant.parse("2026-04-24T10:00:00Z")),
                        materialization("newer message", Instant.parse("2026-04-24T11:00:00Z"))
                ));

        DomainRuleWorkflowActionPolicyResolver resolver =
                new DomainRuleWorkflowActionPolicyResolver(domainRuleServiceProvider);

        Optional<DomainRuleWorkflowActionPolicy> policy =
                resolver.resolveAppliedPolicy("human-resources.folhas-pagamento:mark-paid");

        assertTrue(policy.isPresent());
        assertEquals("human-resources.folhas-pagamento", policy.orElseThrow().resourceKey());
        assertEquals("mark-paid", policy.orElseThrow().actionId());
        assertEquals(List.of("PROGRAMADA"), policy.orElseThrow().requiredStates());
        assertEquals("newer message", policy.orElseThrow().message());
    }

    @Test
    void shouldFallbackWhenConfigStarterServiceIsNotAvailable() {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(null);

        DomainRuleWorkflowActionPolicyResolver resolver =
                new DomainRuleWorkflowActionPolicyResolver(domainRuleServiceProvider);

        assertTrue(resolver.resolveAppliedPolicy("human-resources.folhas-pagamento:mark-paid").isEmpty());
    }

    @Test
    void shouldFallbackWhenMaterializationStoreIsNotAvailable() {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(domainRuleService);
        when(domainRuleService.materializations(
                "default",
                "dev",
                null,
                "workflow_action",
                "resource-workflow-action",
                "human-resources.folhas-pagamento:mark-paid",
                "applied"))
                .thenThrow(new InvalidDataAccessResourceUsageException("domain_rule_materialization not found"));

        DomainRuleWorkflowActionPolicyResolver resolver =
                new DomainRuleWorkflowActionPolicyResolver(domainRuleServiceProvider);

        assertTrue(resolver.resolveAppliedPolicy("human-resources.folhas-pagamento:mark-paid").isEmpty());
    }

    private static DomainRuleMaterializationResponse materialization(String message, Instant appliedAt) throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree("""
                {
                  "kind": "workflow_action_policy",
                  "workflowAction": {
                    "resourceKey": "human-resources.folhas-pagamento",
                    "actionId": "mark-paid"
                  },
                  "availabilityPolicy": {
                    "requiredStates": ["PROGRAMADA"],
                    "message": "%s"
                  }
                }
                """.formatted(message));
        return materializationResponse(
                UUID.randomUUID(),
                "default",
                "dev",
                UUID.randomUUID(),
                "human-resources.folhas-pagamento.rule.mark-paid-compliance",
                1,
                "human-resources.folhas-pagamento.rule.mark-paid-compliance:workflow_action:human-resources.folhas-pagamento:mark-paid",
                "workflow_action",
                "resource-workflow-action",
                "human-resources.folhas-pagamento:mark-paid",
                "/availabilityPolicy",
                null,
                "workflow-action-policy",
                "applied",
                payload,
                "source",
                null,
                null,
                "human",
                "finance-owner",
                appliedAt,
                appliedAt,
                appliedAt);
    }

    private static DomainRuleMaterializationResponse materializationResponse(Object... args) {
        try {
            for (var constructor : DomainRuleMaterializationResponse.class.getDeclaredConstructors()) {
                if (constructor.getParameterCount() == args.length) {
                    return (DomainRuleMaterializationResponse) constructor.newInstance(args);
                }
                if (constructor.getParameterCount() == args.length - 1) {
                    Object[] legacyArgs = new Object[args.length - 1];
                    System.arraycopy(args, 0, legacyArgs, 0, 17);
                    System.arraycopy(args, 18, legacyArgs, 17, args.length - 18);
                    return (DomainRuleMaterializationResponse) constructor.newInstance(legacyArgs);
                }
            }
            throw new IllegalStateException("Unsupported DomainRuleMaterializationResponse constructor shape.");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not create DomainRuleMaterializationResponse test fixture.", ex);
        }
    }
}
