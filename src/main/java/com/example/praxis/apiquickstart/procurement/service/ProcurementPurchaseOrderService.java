package com.example.praxis.apiquickstart.procurement.service;

import com.example.praxis.apiquickstart.config.DomainRuleBackendValidationPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleBackendValidationPolicyResolver;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementPurchaseOrderDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementPurchaseOrderDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementPurchaseOrderDTO;
import com.example.praxis.apiquickstart.procurement.dto.actions.ProcurementPurchaseOrderWorkflowRequestDTO;
import com.example.praxis.apiquickstart.procurement.dto.actions.ProcurementPurchaseOrderWorkflowResultDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementPurchaseOrderFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementPurchaseOrder;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementSupplier;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementPurchaseOrderMapper;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementPurchaseOrderRepository;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementSupplierRepository;
import org.praxisplatform.uischema.service.base.BaseResourceCommandService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProcurementPurchaseOrderService extends AbstractQuickstartCrudService<ProcurementPurchaseOrder, ProcurementPurchaseOrderDTO, Integer, ProcurementPurchaseOrderFilterDTO, CreateProcurementPurchaseOrderDTO, UpdateProcurementPurchaseOrderDTO> {
    private static final String RESOURCE_KEY = "procurement.purchase-orders";
    private static final String WORKFLOW_POLICY_TARGET_PREFIX = RESOURCE_KEY + ":";
    private static final String SUPPLIER_RESOURCE_KEY = "procurement.suppliers";
    private static final String SUPPLIER_REFERENCE_FIELD = "supplierId";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_RECEIVED = "RECEIVED";
    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("companyId", "companyId", Set.of(StatsMetric.COUNT))
            .groupByBucket("supplierId", "supplierId", Set.of(StatsMetric.COUNT))
            .groupByBucket("contractId", "contractId", Set.of(StatsMetric.COUNT))
            .groupByBucket("productId", "productId", Set.of(StatsMetric.COUNT))
            .groupByBucket("currency", "currency", Set.of(StatsMetric.COUNT))
            .groupByBucket("status", "status", Set.of(StatsMetric.COUNT))
            .temporalTimeSeriesField("orderDate", "orderDate")
            .temporalTimeSeriesField("approvedAt", "approvedAt")
            .temporalTimeSeriesField("cancelledAt", "cancelledAt")
            .temporalTimeSeriesField("receivedAt", "receivedAt")
            .numericHistogramMeasureField("quantity", "quantity")
            .build();

    private final ProcurementPurchaseOrderMapper mapper;
    private final ProcurementPurchaseOrderRepository repository;
    private final ProcurementSupplierRepository supplierRepository;
    private final DomainRuleBackendValidationPolicyResolver backendValidationPolicyResolver;
    private final DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    public ProcurementPurchaseOrderService(
            ProcurementPurchaseOrderRepository repository,
            ProcurementPurchaseOrderMapper mapper,
            ProcurementSupplierRepository supplierRepository,
            DomainRuleBackendValidationPolicyResolver backendValidationPolicyResolver,
            DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver) {
        super(repository, ProcurementPurchaseOrder.class, mapper::toDto, mapper::toEntity, mapper::toEntity, ProcurementPurchaseOrder::getId);
        this.mapper = mapper;
        this.repository = repository;
        this.supplierRepository = supplierRepository;
        this.backendValidationPolicyResolver = backendValidationPolicyResolver;
        this.workflowActionPolicyResolver = workflowActionPolicyResolver;
    }

    @Override
    public BaseResourceCommandService.SavedResult<Integer, ProcurementPurchaseOrderDTO> create(CreateProcurementPurchaseOrderDTO dto) {
        validateSupplierSelection(dto.getSupplierId());
        dto.setStatus(STATUS_DRAFT);
        dto.setDisabledReason(null);
        dto.setApprovedAt(null);
        dto.setCancelledAt(null);
        dto.setReceivedAt(null);
        return super.create(dto);
    }

    @Override
    public ProcurementPurchaseOrderDTO update(Integer id, UpdateProcurementPurchaseOrderDTO dto) {
        validateSupplierSelection(dto.getSupplierId());
        return super.update(id, dto);
    }

    @Override
    public ProcurementPurchaseOrder mergeUpdate(ProcurementPurchaseOrder existing, ProcurementPurchaseOrder fromPayload) {
        String status = existing.getStatus();
        String disabledReason = existing.getDisabledReason();
        LocalDate approvedAt = existing.getApprovedAt();
        LocalDate cancelledAt = existing.getCancelledAt();
        LocalDate receivedAt = existing.getReceivedAt();
        mapper.updateEntity(fromPayload, existing);
        existing.setStatus(status);
        existing.setDisabledReason(disabledReason);
        existing.setApprovedAt(approvedAt);
        existing.setCancelledAt(cancelledAt);
        existing.setReceivedAt(receivedAt);
        return existing;
    }

    @Transactional(readOnly = true)
    public List<ProcurementPurchaseOrderDTO> findBySupplierIdForSupplierSurface(Integer supplierId) {
        return repository.findBySupplierId(supplierId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProcurementPurchaseOrderDTO> findByContractIdForContractSurface(Integer contractId) {
        return repository.findByContractId(contractId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public StatsSupportMode getGroupByStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsSupportMode getTimeSeriesStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsSupportMode getDistributionStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsFieldRegistry getStatsFieldRegistry() {
        return STATS_FIELDS;
    }

    @Transactional
    public ProcurementPurchaseOrderWorkflowResultDTO approve(Integer id, ProcurementPurchaseOrderWorkflowRequestDTO dto) {
        return transitionStatus(
                id,
                "approve",
                List.of(STATUS_DRAFT),
                STATUS_APPROVED,
                null,
                dto,
                "Pedido aprovado para execucao governada"
        );
    }

    @Transactional
    public ProcurementPurchaseOrderWorkflowResultDTO cancel(Integer id, ProcurementPurchaseOrderWorkflowRequestDTO dto) {
        return transitionStatus(
                id,
                "cancel",
                List.of(STATUS_DRAFT, STATUS_APPROVED),
                STATUS_CANCELLED,
                requiredReason(dto, "Cancelar um pedido exige justificativa de negocio ou compliance."),
                dto,
                "Pedido cancelado e removido do fluxo de compra"
        );
    }

    @Transactional
    public ProcurementPurchaseOrderWorkflowResultDTO receive(Integer id, ProcurementPurchaseOrderWorkflowRequestDTO dto) {
        return transitionStatus(
                id,
                "receive",
                List.of(STATUS_APPROVED),
                STATUS_RECEIVED,
                null,
                dto,
                "Pedido recebido e fechado no fluxo de suprimentos"
        );
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

    private ProcurementPurchaseOrderWorkflowResultDTO transitionStatus(
            Integer id,
            String actionId,
            List<String> allowedStates,
            String targetStatus,
            String targetDisabledReason,
            ProcurementPurchaseOrderWorkflowRequestDTO dto,
            String message
    ) {
        ProcurementPurchaseOrder purchaseOrder = getRepository().findById(id).orElseThrow(this::getNotFoundException);
        String previousStatus = normalized(purchaseOrder.getStatus());
        enforceWorkflowActionPolicy(actionId, previousStatus);
        if (!allowedStates.contains(previousStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "State not allowed: " + previousStatus);
        }

        LocalDate today = LocalDate.now();
        purchaseOrder.setStatus(targetStatus);
        purchaseOrder.setDisabledReason(targetDisabledReason);
        if (STATUS_APPROVED.equals(targetStatus)) {
            purchaseOrder.setApprovedAt(today);
        } else if (STATUS_CANCELLED.equals(targetStatus)) {
            purchaseOrder.setCancelledAt(today);
        } else if (STATUS_RECEIVED.equals(targetStatus)) {
            purchaseOrder.setReceivedAt(today);
        }

        ProcurementPurchaseOrder saved = getRepository().save(purchaseOrder);
        ProcurementPurchaseOrderWorkflowResultDTO result = new ProcurementPurchaseOrderWorkflowResultDTO();
        result.setId(saved.getId());
        result.setSupplierId(saved.getSupplierId());
        result.setContractId(saved.getContractId());
        result.setProductId(saved.getProductId());
        result.setStatusAnterior(previousStatus);
        result.setStatusAtual(saved.getStatus());
        result.setMotivo(dto == null ? null : dto.getMotivo());
        result.setApprovedAt(saved.getApprovedAt());
        result.setCancelledAt(saved.getCancelledAt());
        result.setReceivedAt(saved.getReceivedAt());
        result.setMensagem(message);
        return result;
    }

    private void enforceWorkflowActionPolicy(String actionId, String currentStatus) {
        workflowActionPolicyResolver.resolveAppliedPolicy(WORKFLOW_POLICY_TARGET_PREFIX + actionId)
                .filter(policy -> policy.appliesToState(currentStatus))
                .map(DomainRuleWorkflowActionPolicy::message)
                .filter(StringUtils::hasText)
                .ifPresent(message -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, message);
                });
    }

    private static String requiredReason(ProcurementPurchaseOrderWorkflowRequestDTO dto, String fallbackMessage) {
        if (dto == null || !StringUtils.hasText(dto.getMotivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, fallbackMessage);
        }
        return dto.getMotivo().trim();
    }

    private static String normalized(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : "";
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
