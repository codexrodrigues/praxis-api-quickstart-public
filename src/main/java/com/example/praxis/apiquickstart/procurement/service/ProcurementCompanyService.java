package com.example.praxis.apiquickstart.procurement.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementCompanyDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementCompanyDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementCompanyDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementCompanyFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementCompany;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementCompanyMapper;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementCompanyRepository;
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

import java.util.List;
import java.util.Set;

@Service
public class ProcurementCompanyService extends AbstractQuickstartCrudService<ProcurementCompany, ProcurementCompanyDTO, Integer, ProcurementCompanyFilterDTO, CreateProcurementCompanyDTO, UpdateProcurementCompanyDTO> {
    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("status", "status", Set.of(StatsMetric.COUNT))
            .groupByBucket("state", "state", Set.of(StatsMetric.COUNT))
            .groupByBucket("city", "city", Set.of(StatsMetric.COUNT))
            .build();

    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(ProcurementCompany.class, new OptionSourceDescriptor(
                    ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Procurement.COMPANIES,
                    "companyId",
                    "id",
                    "legalName",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_SOURCE,
                            "code",
                            List.of("documentNumber", "city", "state"),
                            "status",
                            null,
                            "disabledReason",
                            List.of("code", "legalName", "documentNumber"),
                            null,
                            new LookupSelectionPolicy(null, "status", List.of("ACTIVE"), List.of("INACTIVE", "BLOCKED"), true, null, null),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Procurement.COMPANIES + "/{id}", "/procurement/companies/{id}", "route")
                    )
            ))
            .build();

    private final ProcurementCompanyMapper mapper;

    public ProcurementCompanyService(ProcurementCompanyRepository repository, ProcurementCompanyMapper mapper) {
        super(repository, ProcurementCompany.class, mapper::toDto, mapper::toEntity, mapper::toEntity, ProcurementCompany::getId);
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
    public ProcurementCompany mergeUpdate(ProcurementCompany existing, ProcurementCompany fromPayload) {
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

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label");
    }
}
