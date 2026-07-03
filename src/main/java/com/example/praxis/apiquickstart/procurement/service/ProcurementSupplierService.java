package com.example.praxis.apiquickstart.procurement.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.config.DomainRuleOptionSourcePolicyResolver;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.actions.ProcurementSupplierWorkflowRequestDTO;
import com.example.praxis.apiquickstart.procurement.dto.actions.ProcurementSupplierWorkflowResultDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementSupplierFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementSupplier;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementSupplierMapper;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementSupplierRepository;
import org.praxisplatform.uischema.options.EntityLookupDescriptor;
import org.praxisplatform.uischema.options.GovernedOptionSourceCatalog;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class ProcurementSupplierService extends AbstractQuickstartCrudService<ProcurementSupplier, ProcurementSupplierDTO, Integer, ProcurementSupplierFilterDTO, CreateProcurementSupplierDTO, UpdateProcurementSupplierDTO> {
    private static final String RESOURCE_KEY = "procurement.suppliers";
    private static final String WORKFLOW_POLICY_TARGET_PREFIX = RESOURCE_KEY + ":";
    private static final Map<String, String> DEPENDENCIES = Map.of("companyId", "companyId");
    private static final LookupSelectionPolicy STATIC_SUPPLIER_SELECTION_POLICY =
            new LookupSelectionPolicy(null, "status", List.of("ACTIVE", "APPROVED"), List.of("INACTIVE", "BLOCKED"), true, null, null);
    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("companyId", "companyId", Set.of(StatsMetric.COUNT))
            .groupByBucket("homologationStatus", "homologationStatus", Set.of(StatsMetric.COUNT))
            .groupByBucket("riskLevel", "riskLevel", Set.of(StatsMetric.COUNT))
            .groupByBucket("status", "status", Set.of(StatsMetric.COUNT))
            .build();

    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(ProcurementSupplier.class, new OptionSourceDescriptor(
                    ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Procurement.SUPPLIERS,
                    "supplierId",
                    "id",
                    "legalName",
                    "id",
                    List.of("companyId"),
                    DEPENDENCIES,
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_SOURCE,
                            "code",
                            List.of("documentNumber", "homologationStatus", "riskLevel"),
                            "status",
                            null,
                            "disabledReason",
                            List.of("code", "legalName", "documentNumber"),
                            DEPENDENCIES,
                            STATIC_SUPPLIER_SELECTION_POLICY,
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Procurement.SUPPLIERS + "/{id}", "/procurement/suppliers/{id}", "route")
                    )
            ))
            .add(ProcurementSupplier.class, paymentTermsDescriptor())
            .add(ProcurementSupplier.class, externalLookupDescriptor())
            .build();

    private final ProcurementSupplierMapper mapper;
    private final DomainRuleOptionSourcePolicyResolver optionSourcePolicyResolver;
    private final DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    public ProcurementSupplierService(
            ProcurementSupplierRepository repository,
            ProcurementSupplierMapper mapper,
            DomainRuleOptionSourcePolicyResolver optionSourcePolicyResolver,
            DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver) {
        super(repository, ProcurementSupplier.class, mapper::toDto, mapper::toEntity, mapper::toEntity, ProcurementSupplier::getId);
        this.mapper = mapper;
        this.optionSourcePolicyResolver = optionSourcePolicyResolver;
        this.workflowActionPolicyResolver = workflowActionPolicyResolver;
    }

    public static OptionSourceRegistry optionSources() {
        return OPTION_SOURCES;
    }

    @Override
    public OptionSourceRegistry getOptionSourceRegistry() {
        return optionSourcePolicyResolver.resolveAppliedSelectionPolicy(ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_SOURCE)
                .map(ProcurementSupplierService::optionSourcesWithSelectionPolicy)
                .orElse(OPTION_SOURCES);
    }

    @Override
    public OptionSourceDescriptor resolveOptionSource(String sourceKey) {
        if (ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_SOURCE.equals(sourceKey)
                || ApiPaths.Procurement.SUPPLIERS_EXTERNAL_LOOKUP_SOURCE.equals(sourceKey)) {
            return OPTION_SOURCES.resolve(ProcurementSupplier.class, sourceKey).orElseThrow();
        }
        return super.resolveOptionSource(sourceKey);
    }

    @Override
    public Optional<String> getOptionSourceDatasetVersion(String sourceKey) {
        if (ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_SOURCE.equals(sourceKey)) {
            return Optional.of("ProcurementPaymentTerms:v1");
        }
        if (ApiPaths.Procurement.SUPPLIERS_EXTERNAL_LOOKUP_SOURCE.equals(sourceKey)) {
            return Optional.of("externalLookup:v1");
        }
        return super.getOptionSourceDatasetVersion(sourceKey);
    }

    @Override
    public ProcurementSupplier mergeUpdate(ProcurementSupplier existing, ProcurementSupplier fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Override
    public StatsSupportMode getGroupByStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsFieldRegistry getStatsFieldRegistry() {
        return STATS_FIELDS;
    }

    @Transactional
    public ProcurementSupplierWorkflowResultDTO block(Integer id, ProcurementSupplierWorkflowRequestDTO dto) {
        return transitionEligibility(
                id,
                "block",
                List.of("ACTIVE", "APPROVED"),
                "BLOCKED",
                requiredReason(dto, "Blocking a supplier requires a compliance reason."),
                dto,
                "Supplier blocked for governed procurement"
        );
    }

    @Transactional
    public ProcurementSupplierWorkflowResultDTO reinstate(Integer id, ProcurementSupplierWorkflowRequestDTO dto) {
        return transitionEligibility(
                id,
                "reinstate",
                List.of("BLOCKED", "INACTIVE"),
                "ACTIVE",
                null,
                dto,
                "Supplier reinstated for governed procurement"
        );
    }

    private ProcurementSupplierWorkflowResultDTO transitionEligibility(
            Integer id,
            String actionId,
            List<String> allowedStates,
            String targetStatus,
            String targetDisabledReason,
            ProcurementSupplierWorkflowRequestDTO dto,
            String message
    ) {
        ProcurementSupplier supplier = getRepository().findById(id).orElseThrow(this::getNotFoundException);
        String previousStatus = normalized(supplier.getStatus());
        enforceWorkflowActionPolicy(actionId, previousStatus);
        if (!allowedStates.contains(previousStatus)) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + previousStatus);
        }

        supplier.setStatus(targetStatus);
        supplier.setDisabledReason(targetDisabledReason);
        ProcurementSupplier saved = getRepository().save(supplier);

        ProcurementSupplierWorkflowResultDTO result = new ProcurementSupplierWorkflowResultDTO();
        result.setId(saved.getId());
        result.setSupplierCode(saved.getCode());
        result.setLegalName(saved.getLegalName());
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
                    throw new ResponseStatusException(CONFLICT, message);
                });
    }

    private static String requiredReason(ProcurementSupplierWorkflowRequestDTO dto, String fallbackMessage) {
        if (dto == null || !StringUtils.hasText(dto.getMotivo())) {
            throw new ResponseStatusException(CONFLICT, fallbackMessage);
        }
        return dto.getMotivo().trim();
    }

    private static String normalized(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase(java.util.Locale.ROOT) : "";
    }

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label");
    }

    private static OptionSourceRegistry optionSourcesWithSelectionPolicy(LookupSelectionPolicy selectionPolicy) {
        return OptionSourceRegistry.builder()
                .add(ProcurementSupplier.class, new OptionSourceDescriptor(
                        ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_SOURCE,
                        OptionSourceType.RESOURCE_ENTITY,
                        ApiPaths.Procurement.SUPPLIERS,
                        "supplierId",
                        "id",
                        "legalName",
                        "id",
                        List.of("companyId"),
                        DEPENDENCIES,
                        lookupPolicy(),
                        new EntityLookupDescriptor(
                                ApiPaths.Procurement.SUPPLIERS_SUPPLIER_LOOKUP_SOURCE,
                                "code",
                                List.of("documentNumber", "homologationStatus", "riskLevel"),
                                "status",
                                null,
                                "disabledReason",
                                List.of("code", "legalName", "documentNumber"),
                                DEPENDENCIES,
                                selectionPolicy,
                                new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                                new LookupDetailDescriptor(ApiPaths.Procurement.SUPPLIERS + "/{id}", "/procurement/suppliers/{id}", "route")
                        )
                ))
                .add(ProcurementSupplier.class, paymentTermsDescriptor())
                .add(ProcurementSupplier.class, externalLookupDescriptor())
                .build();
    }

    private static OptionSourceDescriptor paymentTermsDescriptor() {
        return GovernedOptionSourceCatalog.providerBackedLookup(
                ApiPaths.Procurement.SUPPLIERS_PAYMENT_TERMS_LOOKUP_SOURCE,
                ApiPaths.Procurement.SUPPLIERS,
                null,
                "label",
                "id",
                List.of("companyId"),
                DEPENDENCIES,
                new OptionSourcePolicy(false, true, "contains", 3, 10, 20, false, false, "label")
        );
    }

    private static OptionSourceDescriptor externalLookupDescriptor() {
        return GovernedOptionSourceCatalog.providerBackedLookup(
                ApiPaths.Procurement.SUPPLIERS_EXTERNAL_LOOKUP_SOURCE,
                ApiPaths.Procurement.SUPPLIERS,
                null,
                "label",
                "id",
                List.of("companyId"),
                DEPENDENCIES,
                new OptionSourcePolicy(false, true, "contains", 0, 10, 20, false, false, "label")
        );
    }
}
