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
class DomainRuleBackendValidationPolicyResolverTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private ObjectProvider<DomainRuleService> domainRuleServiceProvider;

    @Mock
    private DomainRuleService domainRuleService;

    @Test
    void shouldResolveLatestAppliedBackendValidationPolicy() throws Exception {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(domainRuleService);
        when(domainRuleService.materializations(
                "default",
                "dev",
                null,
                "backend_validation",
                "resource-validation",
                "procurement.purchase-orders",
                "applied"))
                .thenReturn(List.of(
                        materialization("INACTIVE", "older message", Instant.parse("2026-04-24T10:00:00Z")),
                        materialization("BLOCKED", "newer message", Instant.parse("2026-04-24T11:00:00Z"))
                ));

        DomainRuleBackendValidationPolicyResolver resolver =
                new DomainRuleBackendValidationPolicyResolver(domainRuleServiceProvider);

        Optional<DomainRuleBackendValidationPolicy> policy =
                resolver.resolveAppliedPolicy("procurement.purchase-orders");

        assertTrue(policy.isPresent());
        assertEquals("supplierId", policy.orElseThrow().referenceField());
        assertEquals("procurement.suppliers", policy.orElseThrow().referenceResourceKey());
        assertEquals(List.of("BLOCKED"), policy.orElseThrow().blockedStatuses());
        assertEquals("newer message", policy.orElseThrow().validationMessageTemplate());
    }

    @Test
    void shouldFallbackToConditionStatusesWhenParametersDoNotCarryBlockedStatuses() throws Exception {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(domainRuleService);
        when(domainRuleService.materializations(
                "default",
                "dev",
                null,
                "backend_validation",
                "resource-validation",
                "procurement.purchase-orders",
                "applied"))
                .thenReturn(List.of(materializationWithCondition("SUSPENDED", Instant.parse("2026-04-24T10:00:00Z"))));

        DomainRuleBackendValidationPolicyResolver resolver =
                new DomainRuleBackendValidationPolicyResolver(domainRuleServiceProvider);

        Optional<DomainRuleBackendValidationPolicy> policy =
                resolver.resolveAppliedPolicy("procurement.purchase-orders");

        assertTrue(policy.isPresent());
        assertEquals(List.of("SUSPENDED"), policy.orElseThrow().blockedStatuses());
    }

    @Test
    void shouldFallbackWhenConfigStarterServiceIsNotAvailable() {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(null);

        DomainRuleBackendValidationPolicyResolver resolver =
                new DomainRuleBackendValidationPolicyResolver(domainRuleServiceProvider);

        assertTrue(resolver.resolveAppliedPolicy("procurement.purchase-orders").isEmpty());
    }

    @Test
    void shouldFallbackWhenMaterializationStoreIsNotAvailable() {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(domainRuleService);
        when(domainRuleService.materializations(
                "default",
                "dev",
                null,
                "backend_validation",
                "resource-validation",
                "procurement.purchase-orders",
                "applied"))
                .thenThrow(new InvalidDataAccessResourceUsageException("domain_rule_materialization not found"));

        DomainRuleBackendValidationPolicyResolver resolver =
                new DomainRuleBackendValidationPolicyResolver(domainRuleServiceProvider);

        assertTrue(resolver.resolveAppliedPolicy("procurement.purchase-orders").isEmpty());
    }

    private static DomainRuleMaterializationResponse materialization(
            String blockedStatus,
            String validationMessage,
            Instant appliedAt) throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree("""
                {
                  "kind": "resource_validation_policy",
                  "validationPolicy": {
                    "validationMessageTemplate": "%s",
                    "parameters": {
                      "referenceResourceKey": "procurement.suppliers",
                      "referenceField": "supplierId",
                      "statusPropertyPath": "status",
                      "blockedStatuses": ["%s"]
                    }
                  }
                }
                """.formatted(validationMessage, blockedStatus));
        return materialization(payload, appliedAt);
    }

    private static DomainRuleMaterializationResponse materializationWithCondition(
            String blockedStatus,
            Instant appliedAt) throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree("""
                {
                  "kind": "resource_validation_policy",
                  "validationPolicy": {
                    "condition": {
                      "in": ["supplier.status", ["%s"]]
                    },
                    "parameters": {
                      "referenceResourceKey": "procurement.suppliers",
                      "referenceField": "supplierId",
                      "statusPropertyPath": "status"
                    }
                  }
                }
                """.formatted(blockedStatus));
        return materialization(payload, appliedAt);
    }

    private static DomainRuleMaterializationResponse materialization(JsonNode payload, Instant appliedAt) {
        return materializationResponse(
                UUID.randomUUID(),
                "default",
                "dev",
                UUID.randomUUID(),
                "procurement.purchase-orders.rule.supplier-validation",
                1,
                "procurement.purchase-orders.rule.supplier-validation:backend_validation:procurement.purchase-orders",
                "backend_validation",
                "resource-validation",
                "procurement.purchase-orders",
                "/validationPolicy",
                null,
                "validation-policy",
                "applied",
                payload,
                "source",
                null,
                null,
                "human",
                "procurement-owner",
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
