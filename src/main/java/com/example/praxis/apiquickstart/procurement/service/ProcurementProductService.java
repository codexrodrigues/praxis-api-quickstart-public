package com.example.praxis.apiquickstart.procurement.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementProductDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementProductDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementProductDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementProductFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementProduct;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementProductMapper;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementProductRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProcurementProductService extends AbstractQuickstartCrudService<ProcurementProduct, ProcurementProductDTO, Integer, ProcurementProductFilterDTO, CreateProcurementProductDTO, UpdateProcurementProductDTO> {
    private static final Map<String, String> DEPENDENCIES = Map.of(
            "companyId", "companyId",
            "contractId", "contractId"
    );
    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("companyId", "companyId", Set.of(StatsMetric.COUNT))
            .groupByBucket("contractId", "contractId", Set.of(StatsMetric.COUNT))
            .groupByBucket("categoryName", "categoryName", Set.of(StatsMetric.COUNT))
            .groupByBucket("unitOfMeasure", "unitOfMeasure", Set.of(StatsMetric.COUNT))
            .groupByBucket("status", "status", Set.of(StatsMetric.COUNT))
            .numericHistogramMeasureField("stockAvailable", "stockAvailable")
            .build();

    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(ProcurementProduct.class, new OptionSourceDescriptor(
                    ApiPaths.Procurement.PRODUCTS_PRODUCT_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Procurement.PRODUCTS,
                    "productId",
                    "id",
                    "name",
                    "id",
                    List.of("companyId", "contractId"),
                    DEPENDENCIES,
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Procurement.PRODUCTS_PRODUCT_LOOKUP_SOURCE,
                            "sku",
                            List.of("categoryName", "stockAvailable", "unitOfMeasure"),
                            "status",
                            null,
                            "disabledReason",
                            List.of("sku", "name", "categoryName"),
                            DEPENDENCIES,
                            new LookupSelectionPolicy(null, "status", List.of("ACTIVE", "AVAILABLE"), List.of("INACTIVE", "BLOCKED"), true, null, null),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Procurement.PRODUCTS + "/{id}", "/procurement/products/{id}", "route")
                    )
            ))
            .build();

    private final ProcurementProductMapper mapper;
    private final ProcurementProductRepository repository;

    public ProcurementProductService(ProcurementProductRepository repository, ProcurementProductMapper mapper) {
        super(repository, ProcurementProduct.class, mapper::toDto, mapper::toEntity, mapper::toEntity, ProcurementProduct::getId);
        this.mapper = mapper;
        this.repository = repository;
    }

    public static OptionSourceRegistry optionSources() {
        return OPTION_SOURCES;
    }

    @Override
    public OptionSourceRegistry getOptionSourceRegistry() {
        return OPTION_SOURCES;
    }

    @Override
    public ProcurementProduct mergeUpdate(ProcurementProduct existing, ProcurementProduct fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Transactional(readOnly = true)
    public List<ProcurementProductDTO> findByContractIdForContractSurface(Integer contractId) {
        return repository.findByContractId(contractId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public StatsSupportMode getGroupByStatsSupportMode() {
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

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label");
    }
}
