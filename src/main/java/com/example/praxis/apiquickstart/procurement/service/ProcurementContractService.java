package com.example.praxis.apiquickstart.procurement.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementContractDTO;
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProcurementContractService extends AbstractQuickstartCrudService<ProcurementContract, ProcurementContractDTO, Integer, ProcurementContractFilterDTO, CreateProcurementContractDTO, UpdateProcurementContractDTO> {
    private static final Map<String, String> DEPENDENCIES = Map.of(
            "companyId", "companyId",
            "supplierId", "supplierId"
    );

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
                            new LookupSelectionPolicy(null, "status", List.of("ACTIVE", "SIGNED"), List.of("EXPIRED", "CANCELLED"), true, null, null),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Procurement.CONTRACTS + "/{id}", "/procurement/contracts/{id}", "route")
                    )
            ))
            .build();

    private final ProcurementContractMapper mapper;

    public ProcurementContractService(ProcurementContractRepository repository, ProcurementContractMapper mapper) {
        super(repository, ProcurementContract.class, mapper::toDto, mapper::toEntity, mapper::toEntity, ProcurementContract::getId);
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
    public ProcurementContract mergeUpdate(ProcurementContract existing, ProcurementContract fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label");
    }
}
