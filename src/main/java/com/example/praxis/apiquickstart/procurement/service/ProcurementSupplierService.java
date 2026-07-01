package com.example.praxis.apiquickstart.procurement.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.config.DomainRuleOptionSourcePolicyResolver;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementSupplierDTO;
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProcurementSupplierService extends AbstractQuickstartCrudService<ProcurementSupplier, ProcurementSupplierDTO, Integer, ProcurementSupplierFilterDTO, CreateProcurementSupplierDTO, UpdateProcurementSupplierDTO> {
    private static final Map<String, String> DEPENDENCIES = Map.of("companyId", "companyId");
    private static final LookupSelectionPolicy STATIC_SUPPLIER_SELECTION_POLICY =
            new LookupSelectionPolicy(null, "status", List.of("ACTIVE", "APPROVED"), List.of("INACTIVE", "BLOCKED"), true, null, null);

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

    public ProcurementSupplierService(
            ProcurementSupplierRepository repository,
            ProcurementSupplierMapper mapper,
            DomainRuleOptionSourcePolicyResolver optionSourcePolicyResolver) {
        super(repository, ProcurementSupplier.class, mapper::toDto, mapper::toEntity, mapper::toEntity, ProcurementSupplier::getId);
        this.mapper = mapper;
        this.optionSourcePolicyResolver = optionSourcePolicyResolver;
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
