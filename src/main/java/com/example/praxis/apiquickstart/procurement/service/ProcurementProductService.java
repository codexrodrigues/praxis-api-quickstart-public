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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProcurementProductService extends AbstractQuickstartCrudService<ProcurementProduct, ProcurementProductDTO, Integer, ProcurementProductFilterDTO, CreateProcurementProductDTO, UpdateProcurementProductDTO> {
    private static final Map<String, String> DEPENDENCIES = Map.of(
            "companyId", "companyId",
            "contractId", "contractId"
    );

    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(ProcurementProduct.class, new OptionSourceDescriptor(
                    "product",
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
                            "product",
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

    public ProcurementProductService(ProcurementProductRepository repository, ProcurementProductMapper mapper) {
        super(repository, ProcurementProduct.class, mapper::toDto, mapper::toEntity, mapper::toEntity, ProcurementProduct::getId);
        this.mapper = mapper;
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

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label");
    }
}
