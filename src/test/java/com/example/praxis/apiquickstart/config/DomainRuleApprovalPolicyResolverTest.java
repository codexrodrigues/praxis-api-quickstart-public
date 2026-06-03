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
class DomainRuleApprovalPolicyResolverTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private ObjectProvider<DomainRuleService> domainRuleServiceProvider;

    @Mock
    private DomainRuleService domainRuleService;

    @Test
    void shouldResolveLatestAppliedApprovalPolicy() throws Exception {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(domainRuleService);
        when(domainRuleService.materializations(
                "default",
                "dev",
                null,
                "approval_policy",
                "resource-action-approval",
                "human-resources.eventos-folha:bulk-approve",
                "applied"))
                .thenReturn(List.of(
                        materialization("older message", Instant.parse("2026-04-24T10:00:00Z")),
                        materialization("newer message", Instant.parse("2026-04-24T11:00:00Z"))
                ));

        DomainRuleApprovalPolicyResolver resolver =
                new DomainRuleApprovalPolicyResolver(domainRuleServiceProvider);

        Optional<DomainRuleApprovalPolicy> policy =
                resolver.resolveAppliedPolicy("human-resources.eventos-folha:bulk-approve");

        assertTrue(policy.isPresent());
        assertEquals("human-resources.eventos-folha", policy.orElseThrow().resourceKey());
        assertEquals("bulk-approve", policy.orElseThrow().actionId());
        assertEquals(List.of("payroll-manager"), policy.orElseThrow().requiredApprovals());
        assertEquals(List.of("hr-payroll"), policy.orElseThrow().approvalGroups());
        assertEquals("payroll-events", policy.orElseThrow().approverContext());
        assertEquals("newer message", policy.orElseThrow().message());
    }

    @Test
    void shouldFallbackWhenConfigStarterServiceIsNotAvailable() {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(null);

        DomainRuleApprovalPolicyResolver resolver =
                new DomainRuleApprovalPolicyResolver(domainRuleServiceProvider);

        assertTrue(resolver.resolveAppliedPolicy("human-resources.eventos-folha:bulk-approve").isEmpty());
    }

    @Test
    void shouldFallbackWhenMaterializationStoreIsNotAvailable() {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(domainRuleService);
        when(domainRuleService.materializations(
                "default",
                "dev",
                null,
                "approval_policy",
                "resource-action-approval",
                "human-resources.eventos-folha:bulk-approve",
                "applied"))
                .thenThrow(new InvalidDataAccessResourceUsageException("domain_rule_materialization not found"));

        DomainRuleApprovalPolicyResolver resolver =
                new DomainRuleApprovalPolicyResolver(domainRuleServiceProvider);

        assertTrue(resolver.resolveAppliedPolicy("human-resources.eventos-folha:bulk-approve").isEmpty());
    }

    private static DomainRuleMaterializationResponse materialization(String message, Instant appliedAt) throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree("""
                {
                  "kind": "approval_policy",
                  "resourceAction": {
                    "resourceKey": "human-resources.eventos-folha",
                    "actionId": "bulk-approve"
                  },
                  "approvalPolicy": {
                    "requiredApprovals": ["payroll-manager"],
                    "approvalGroups": ["hr-payroll"],
                    "approverContext": "payroll-events",
                    "message": "%s"
                  }
                }
                """.formatted(message));
        return materializationResponse(
                UUID.randomUUID(),
                "default",
                "dev",
                UUID.randomUUID(),
                "human-resources.eventos-folha.rule.bulk-approve-approval",
                1,
                "human-resources.eventos-folha.rule.bulk-approve-approval:approval_policy:human-resources.eventos-folha:bulk-approve",
                "approval_policy",
                "resource-action-approval",
                "human-resources.eventos-folha:bulk-approve",
                "/approvalPolicy",
                null,
                "approval-policy",
                "applied",
                payload,
                "source",
                null,
                null,
                "human",
                "payroll-manager",
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
