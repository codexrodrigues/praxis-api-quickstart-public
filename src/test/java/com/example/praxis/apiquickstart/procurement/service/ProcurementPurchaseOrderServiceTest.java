package com.example.praxis.apiquickstart.procurement.service;

import com.example.praxis.apiquickstart.config.DomainRuleBackendValidationPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleBackendValidationPolicyResolver;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementSupplier;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementPurchaseOrderMapper;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementPurchaseOrderRepository;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementSupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcurementPurchaseOrderServiceTest {
    @Mock
    private ProcurementPurchaseOrderRepository repository;

    @Mock
    private ProcurementPurchaseOrderMapper mapper;

    @Mock
    private ProcurementSupplierRepository supplierRepository;

    @Mock
    private DomainRuleBackendValidationPolicyResolver backendValidationPolicyResolver;

    @Test
    void shouldBlockSupplierWhenPublishedBackendValidationPolicyBlocksStatus() {
        when(backendValidationPolicyResolver.resolveAppliedPolicy("procurement.purchase-orders"))
                .thenReturn(Optional.of(policy("Fornecedor bloqueado por decisao publicada")));
        when(supplierRepository.findById(10)).thenReturn(Optional.of(supplier(10, "BLOCKED")));

        ProcurementPurchaseOrderService service = service();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.validateSupplierSelection(10));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Fornecedor bloqueado por decisao publicada", exception.getReason());
    }

    @Test
    void shouldAllowSupplierWhenNoPublishedBackendValidationPolicyExists() {
        when(backendValidationPolicyResolver.resolveAppliedPolicy("procurement.purchase-orders"))
                .thenReturn(Optional.empty());

        ProcurementPurchaseOrderService service = service();

        assertDoesNotThrow(() -> service.validateSupplierSelection(10));
        verify(supplierRepository, never()).findById(10);
    }

    @Test
    void shouldAllowSupplierWhenStatusIsNotBlockedByPublishedPolicy() {
        when(backendValidationPolicyResolver.resolveAppliedPolicy("procurement.purchase-orders"))
                .thenReturn(Optional.of(policy("Fornecedor bloqueado por decisao publicada")));
        when(supplierRepository.findById(10)).thenReturn(Optional.of(supplier(10, "ACTIVE")));

        ProcurementPurchaseOrderService service = service();

        assertDoesNotThrow(() -> service.validateSupplierSelection(10));
    }

    @Test
    void shouldIgnorePolicyTargetingDifferentReferenceField() {
        when(backendValidationPolicyResolver.resolveAppliedPolicy("procurement.purchase-orders"))
                .thenReturn(Optional.of(new DomainRuleBackendValidationPolicy(
                        "procurement.purchase-orders",
                        "procurement.suppliers",
                        "contractId",
                        "status",
                        List.of("BLOCKED"),
                        "Contrato invalido")));

        ProcurementPurchaseOrderService service = service();

        assertDoesNotThrow(() -> service.validateSupplierSelection(10));
        verify(supplierRepository, never()).findById(10);
    }

    private ProcurementPurchaseOrderService service() {
        return new ProcurementPurchaseOrderService(
                repository,
                mapper,
                supplierRepository,
                backendValidationPolicyResolver);
    }

    private static DomainRuleBackendValidationPolicy policy(String message) {
        return new DomainRuleBackendValidationPolicy(
                "procurement.purchase-orders",
                "procurement.suppliers",
                "supplierId",
                "status",
                List.of("BLOCKED", "INACTIVE"),
                message);
    }

    private static ProcurementSupplier supplier(Integer id, String status) {
        ProcurementSupplier supplier = new ProcurementSupplier();
        supplier.setId(id);
        supplier.setStatus(status);
        return supplier;
    }
}
