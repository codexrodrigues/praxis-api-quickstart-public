package com.example.praxis.apiquickstart.procurement.service;

import com.example.praxis.apiquickstart.config.DomainRuleOptionSourcePolicyResolver;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementSupplier;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementSupplierMapper;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementSupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.options.LookupSelectionPolicy;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcurementSupplierServiceTest {

    @Mock
    private ProcurementSupplierRepository repository;

    @Mock
    private ProcurementSupplierMapper mapper;

    @Mock
    private DomainRuleOptionSourcePolicyResolver optionSourcePolicyResolver;

    @Mock
    private DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    @Test
    void shouldUsePublishedSelectionPolicyWhenAvailable() {
        when(optionSourcePolicyResolver.resolveAppliedSelectionPolicy("supplier"))
                .thenReturn(Optional.of(new LookupSelectionPolicy(
                        null,
                        "status",
                        List.of("ACTIVE"),
                        List.of("INACTIVE", "BLOCKED", "SUSPENDED"),
                        true,
                        null,
                        "Fornecedor bloqueado por decisao publicada"
                )));

        ProcurementSupplierService service =
                new ProcurementSupplierService(repository, mapper, optionSourcePolicyResolver, workflowActionPolicyResolver);

        LookupSelectionPolicy policy = service.getOptionSourceRegistry()
                .resolve(ProcurementSupplier.class, "supplier")
                .orElseThrow()
                .entityLookup()
                .selectionPolicy();

        assertEquals(List.of("INACTIVE", "BLOCKED", "SUSPENDED"), policy.blockedStatuses());
        assertEquals("Fornecedor bloqueado por decisao publicada", policy.validationMessageTemplate());
    }

    @Test
    void shouldFallbackToStaticPolicyWhenNoPublishedPolicyExists() {
        when(optionSourcePolicyResolver.resolveAppliedSelectionPolicy("supplier")).thenReturn(Optional.empty());

        ProcurementSupplierService service =
                new ProcurementSupplierService(repository, mapper, optionSourcePolicyResolver, workflowActionPolicyResolver);

        LookupSelectionPolicy policy = service.getOptionSourceRegistry()
                .resolve(ProcurementSupplier.class, "supplier")
                .orElseThrow()
                .entityLookup()
                .selectionPolicy();

        assertEquals(List.of("ACTIVE", "APPROVED"), policy.allowedStatuses());
        assertEquals(List.of("INACTIVE", "BLOCKED"), policy.blockedStatuses());
    }
}
