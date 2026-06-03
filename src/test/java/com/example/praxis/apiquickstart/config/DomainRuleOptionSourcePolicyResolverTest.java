package com.example.praxis.apiquickstart.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.config.dto.DomainRuleMaterializationResponse;
import org.praxisplatform.config.service.DomainRuleService;
import org.praxisplatform.uischema.options.LookupSelectionPolicy;
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
class DomainRuleOptionSourcePolicyResolverTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private ObjectProvider<DomainRuleService> domainRuleServiceProvider;

    @Mock
    private DomainRuleService domainRuleService;

    @Test
    void shouldResolveLatestAppliedOptionSourcePolicy() throws Exception {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(domainRuleService);
        when(domainRuleService.materializations(
                "default",
                "dev",
                null,
                "option_source",
                "resource-option-source",
                "supplier",
                "applied"))
                .thenReturn(List.of(
                        materialization("ACTIVE", "older message", Instant.parse("2026-04-24T10:00:00Z")),
                        materialization("SUSPENDED", "newer message", Instant.parse("2026-04-24T11:00:00Z"))
                ));

        DomainRuleOptionSourcePolicyResolver resolver =
                new DomainRuleOptionSourcePolicyResolver(domainRuleServiceProvider);

        Optional<LookupSelectionPolicy> policy = resolver.resolveAppliedSelectionPolicy("supplier");

        assertTrue(policy.isPresent());
        assertEquals(List.of("SUSPENDED"), policy.orElseThrow().blockedStatuses());
        assertEquals("newer message", policy.orElseThrow().validationMessageTemplate());
    }

    @Test
    void shouldFallbackWhenConfigStarterServiceIsNotAvailable() {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(null);

        DomainRuleOptionSourcePolicyResolver resolver =
                new DomainRuleOptionSourcePolicyResolver(domainRuleServiceProvider);

        assertTrue(resolver.resolveAppliedSelectionPolicy("supplier").isEmpty());
    }

    @Test
    void shouldFallbackWhenMaterializationStoreIsNotAvailable() {
        when(domainRuleServiceProvider.getIfAvailable()).thenReturn(domainRuleService);
        when(domainRuleService.materializations(
                "default",
                "dev",
                null,
                "option_source",
                "resource-option-source",
                "supplier",
                "applied"))
                .thenThrow(new InvalidDataAccessResourceUsageException("domain_rule_materialization not found"));

        DomainRuleOptionSourcePolicyResolver resolver =
                new DomainRuleOptionSourcePolicyResolver(domainRuleServiceProvider);

        assertTrue(resolver.resolveAppliedSelectionPolicy("supplier").isEmpty());
    }

    private static DomainRuleMaterializationResponse materialization(
            String blockedStatus,
            String validationMessage,
            Instant appliedAt) throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree("""
                {
                  "kind": "lookup_selection_policy",
                  "selectionPolicy": {
                    "statusPropertyPath": "status",
                    "blockedStatuses": ["%s"],
                    "allowRetainInvalidExistingValue": true,
                    "validationMessageTemplate": "%s"
                  }
                }
                """.formatted(blockedStatus, validationMessage));
        return materializationResponse(
                UUID.randomUUID(),
                "default",
                "dev",
                UUID.randomUUID(),
                "procurement.suppliers.rule.selection",
                1,
                "procurement.suppliers.rule.selection:option_source:supplier",
                "option_source",
                "resource-option-source",
                "supplier",
                "/selectionPolicy",
                null,
                "selection-policy",
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
