package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.VwPerfilHeroiDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.VwPerfilHeroiFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.VwPerfilHeroi;
import com.example.praxis.apiquickstart.hr.mapper.VwPerfilHeroiMapper;
import com.example.praxis.apiquickstart.hr.repository.VwPerfilHeroiRepository;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartReadOnlyService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class VwPerfilHeroiService extends AbstractQuickstartReadOnlyService<VwPerfilHeroi, VwPerfilHeroiDTO, Integer, VwPerfilHeroiFilterDTO> {

    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("exposicaoPublica", "exposicaoPublica", Set.of(StatsMetric.COUNT))
            .groupByBucket("universo", "universo", Set.of(StatsMetric.COUNT))
            .groupByBucket("cargo", "cargo", Set.of(StatsMetric.COUNT))
            .groupByBucket("departamento", "departamento", Set.of(StatsMetric.COUNT))
            .groupByBucket("equipePrincipal", "equipePrincipal", Set.of(StatsMetric.COUNT))
            .groupByBucket("basePrincipal", "basePrincipal", Set.of(StatsMetric.COUNT))
            .numericHistogramMeasureField("scorePublico", "scorePublico")
            .numericHistogramMeasureField("scoreGovernamental", "scoreGovernamental")
            .numericHistogramMeasureField("scoreMedio", "scoreMedio")
            .build();

    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(VwPerfilHeroi.class, new OptionSourceDescriptor(
                    "universo",
                    OptionSourceType.DISTINCT_DIMENSION,
                    com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.VW_PERFIL_HEROI,
                    "universo",
                    "universo",
                    "universo",
                    "universo",
                    List.of("exposicaoPublica"),
                    new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label")
            ))
            .add(VwPerfilHeroi.class, new OptionSourceDescriptor(
                    "equipePrincipal",
                    OptionSourceType.DISTINCT_DIMENSION,
                    com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.VW_PERFIL_HEROI,
                    "equipePrincipal",
                    "equipePrincipal",
                    "equipePrincipal",
                    "equipePrincipal",
                    List.of("universo", "exposicaoPublica"),
                    new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label")
            ))
            .add(VwPerfilHeroi.class, new OptionSourceDescriptor(
                    "basePrincipal",
                    OptionSourceType.DISTINCT_DIMENSION,
                    com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.VW_PERFIL_HEROI,
                    "basePrincipal",
                    "basePrincipal",
                    "basePrincipal",
                    "basePrincipal",
                    List.of("universo", "equipePrincipal"),
                    new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label")
            ))
            .build();

    private final VwPerfilHeroiMapper mapper;

    public VwPerfilHeroiService(VwPerfilHeroiRepository repository, VwPerfilHeroiMapper mapper) {
        super(repository, VwPerfilHeroi.class, mapper::toDto, VwPerfilHeroi::getFuncionarioId);
        this.mapper = mapper;
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

    public static OptionSourceRegistry optionSources() {
        return OPTION_SOURCES;
    }

    @Override
    public OptionSourceRegistry getOptionSourceRegistry() {
        return OPTION_SOURCES;
    }
}


