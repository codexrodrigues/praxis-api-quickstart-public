package com.example.praxis.apiquickstart.procurement.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.dto.actions.ProcurementContractWorkflowRequestDTO;
import com.example.praxis.apiquickstart.procurement.dto.actions.ProcurementContractWorkflowResultDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementContractFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementContract;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementContractMapper;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementContractRepository;
import org.praxisplatform.uischema.options.EntityLookupDescriptor;
import org.praxisplatform.uischema.options.LookupCapabilities;
import org.praxisplatform.uischema.options.LookupDetailDescriptor;
import org.praxisplatform.uischema.options.LookupSelectionPolicy;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProcurementContractService extends AbstractQuickstartCrudService<ProcurementContract, ProcurementContractDTO, Integer, ProcurementContractFilterDTO, CreateProcurementContractDTO, UpdateProcurementContractDTO> {
    private static final String RESOURCE_KEY = "procurement.contracts";
    private static final String WORKFLOW_POLICY_TARGET_PREFIX = RESOURCE_KEY + ":";
    private static final Map<String, String> DEPENDENCIES = Map.of(
            "companyId", "companyId",
            "supplierId", "supplierId"
    );
    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("companyId", "companyId", Set.of(StatsMetric.COUNT))
            .groupByBucket("supplierId", "supplierId", Set.of(StatsMetric.COUNT))
            .groupByBucket("supplierName", "supplierName", Set.of(StatsMetric.COUNT))
            .groupByBucket("currency", "currency", Set.of(StatsMetric.COUNT))
            .groupByBucket("status", "status", Set.of(StatsMetric.COUNT))
            .temporalTimeSeriesField("validUntil", "validUntil")
            .build();

    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(ProcurementContract.class, new OptionSourceDescriptor(
                    ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Procurement.CONTRACTS,
                    "contractId",
                    "id",
                    "number",
                    "id",
                    List.of("companyId", "supplierId"),
                    DEPENDENCIES,
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Procurement.CONTRACTS_CONTRACT_LOOKUP_SOURCE,
                            null,
                            List.of("supplierName", "validUntil", "currency"),
                            "status",
                            null,
                            "disabledReason",
                            List.of("number", "supplierName", "currency"),
                            DEPENDENCIES,
                            new LookupSelectionPolicy(null, "status", List.of("ACTIVE", "SIGNED"), List.of("EXPIRED", "CANCELLED", "SUSPENDED"), true, null, null),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Procurement.CONTRACTS + "/{id}", "/procurement/contracts/{id}", "route")
                    )
            ))
            .build();

    private final ProcurementContractMapper mapper;
    private final ProcurementContractRepository repository;
    private final DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    public ProcurementContractService(
            ProcurementContractRepository repository,
            ProcurementContractMapper mapper,
            DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver) {
        super(repository, ProcurementContract.class, mapper::toDto, mapper::toEntity, mapper::toEntity, ProcurementContract::getId);
        this.mapper = mapper;
        this.repository = repository;
        this.workflowActionPolicyResolver = workflowActionPolicyResolver;
    }

    public static OptionSourceRegistry optionSources() {
        return OPTION_SOURCES;
    }

    @Override
    public OptionSourceRegistry getOptionSourceRegistry() {
        return OPTION_SOURCES;
    }

    @Override
    public ProcurementContract mergeUpdate(ProcurementContract existing, ProcurementContract fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Transactional(readOnly = true)
    public List<ProcurementContractDTO> findBySupplierIdForSupplierSurface(Integer supplierId) {
        return repository.findBySupplierId(supplierId).stream()
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
    public StatsFieldRegistry getStatsFieldRegistry() {
        return STATS_FIELDS;
    }

    @Transactional
    public ProcurementContractWorkflowResultDTO sign(Integer id, ProcurementContractWorkflowRequestDTO dto) {
        return transitionStatus(
                id,
                "sign",
                List.of("DRAFT", "ACTIVE"),
                "SIGNED",
                null,
                dto,
                "Contract signed for governed procurement"
        );
    }

    @Transactional
    public ProcurementContractWorkflowResultDTO suspend(Integer id, ProcurementContractWorkflowRequestDTO dto) {
        return transitionStatus(
                id,
                "suspend",
                List.of("SIGNED", "ACTIVE"),
                "SUSPENDED",
                requiredReason(dto, "Suspending a contract requires a business or compliance reason."),
                dto,
                "Contract suspended for governed procurement"
        );
    }

    @Transactional
    public ProcurementContractWorkflowResultDTO reactivate(Integer id, ProcurementContractWorkflowRequestDTO dto) {
        return transitionStatus(
                id,
                "reactivate",
                List.of("SUSPENDED"),
                "SIGNED",
                null,
                dto,
                "Contract reactivated for governed procurement"
        );
    }

    private ProcurementContractWorkflowResultDTO transitionStatus(
            Integer id,
            String actionId,
            List<String> allowedStates,
            String targetStatus,
            String targetDisabledReason,
            ProcurementContractWorkflowRequestDTO dto,
            String message
    ) {
        ProcurementContract contract = getRepository().findById(id).orElseThrow(this::getNotFoundException);
        String previousStatus = normalized(contract.getStatus());
        enforceWorkflowActionPolicy(actionId, previousStatus);
        if (!allowedStates.contains(previousStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "State not allowed: " + previousStatus);
        }

        contract.setStatus(targetStatus);
        contract.setDisabledReason(targetDisabledReason);
        ProcurementContract saved = getRepository().save(contract);

        ProcurementContractWorkflowResultDTO result = new ProcurementContractWorkflowResultDTO();
        result.setId(saved.getId());
        result.setContractNumber(saved.getNumber());
        result.setSupplierName(saved.getSupplierName());
        result.setStatusAnterior(previousStatus);
        result.setStatusAtual(saved.getStatus());
        result.setMotivo(dto == null ? null : dto.getMotivo());
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

    private static String requiredReason(ProcurementContractWorkflowRequestDTO dto, String fallbackMessage) {
        if (dto == null || !StringUtils.hasText(dto.getMotivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, fallbackMessage);
        }
        return dto.getMotivo().trim();
    }

    private static String normalized(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : "";
    }

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label");
    }
}
