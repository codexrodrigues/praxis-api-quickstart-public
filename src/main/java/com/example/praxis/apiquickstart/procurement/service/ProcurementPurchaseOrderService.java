package com.example.praxis.apiquickstart.procurement.service;

import com.example.praxis.apiquickstart.config.DomainRuleBackendValidationPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleBackendValidationPolicyResolver;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementPurchaseOrderDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementPurchaseOrderDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementPurchaseOrderDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementPurchaseOrderFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementPurchaseOrder;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementSupplier;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementPurchaseOrderMapper;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementPurchaseOrderRepository;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementSupplierRepository;
import org.praxisplatform.uischema.service.base.BaseResourceCommandService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProcurementPurchaseOrderService extends AbstractQuickstartCrudService<ProcurementPurchaseOrder, ProcurementPurchaseOrderDTO, Integer, ProcurementPurchaseOrderFilterDTO, CreateProcurementPurchaseOrderDTO, UpdateProcurementPurchaseOrderDTO> {
    private static final String RESOURCE_KEY = "procurement.purchase-orders";
    private static final String SUPPLIER_RESOURCE_KEY = "procurement.suppliers";
    private static final String SUPPLIER_REFERENCE_FIELD = "supplierId";

    private final ProcurementPurchaseOrderMapper mapper;
    private final ProcurementSupplierRepository supplierRepository;
    private final DomainRuleBackendValidationPolicyResolver backendValidationPolicyResolver;

    public ProcurementPurchaseOrderService(
            ProcurementPurchaseOrderRepository repository,
            ProcurementPurchaseOrderMapper mapper,
            ProcurementSupplierRepository supplierRepository,
            DomainRuleBackendValidationPolicyResolver backendValidationPolicyResolver) {
        super(repository, ProcurementPurchaseOrder.class, mapper::toDto, mapper::toEntity, mapper::toEntity, ProcurementPurchaseOrder::getId);
        this.mapper = mapper;
        this.supplierRepository = supplierRepository;
        this.backendValidationPolicyResolver = backendValidationPolicyResolver;
    }

    @Override
    public BaseResourceCommandService.SavedResult<Integer, ProcurementPurchaseOrderDTO> create(CreateProcurementPurchaseOrderDTO dto) {
        validateSupplierSelection(dto.getSupplierId());
        return super.create(dto);
    }

    @Override
    public ProcurementPurchaseOrderDTO update(Integer id, UpdateProcurementPurchaseOrderDTO dto) {
        validateSupplierSelection(dto.getSupplierId());
        return super.update(id, dto);
    }

    @Override
    public ProcurementPurchaseOrder mergeUpdate(ProcurementPurchaseOrder existing, ProcurementPurchaseOrder fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    void validateSupplierSelection(Integer supplierId) {
        if (supplierId == null) {
            return;
        }
        backendValidationPolicyResolver.resolveAppliedPolicy(RESOURCE_KEY)
                .filter(this::targetsSupplierSelection)
                .ifPresent(policy -> enforceSupplierPolicy(supplierId, policy));
    }

    private boolean targetsSupplierSelection(DomainRuleBackendValidationPolicy policy) {
        return SUPPLIER_RESOURCE_KEY.equals(policy.referenceResourceKey())
                && SUPPLIER_REFERENCE_FIELD.equals(policy.referenceField());
    }

    private void enforceSupplierPolicy(Integer supplierId, DomainRuleBackendValidationPolicy policy) {
        ProcurementSupplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "supplierId not found: " + supplierId));
        if (policy.blocksStatus(supplier.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, validationMessage(policy, supplier));
        }
    }

    private String validationMessage(DomainRuleBackendValidationPolicy policy, ProcurementSupplier supplier) {
        if (StringUtils.hasText(policy.validationMessageTemplate())) {
            return policy.validationMessageTemplate()
                    .replace("{status}", nullSafe(supplier.getStatus()))
                    .replace("{supplierId}", String.valueOf(supplier.getId()));
        }
        return "Supplier status is blocked by published backend validation policy";
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
