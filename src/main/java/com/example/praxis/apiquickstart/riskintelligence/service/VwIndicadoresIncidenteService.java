package com.example.praxis.apiquickstart.riskintelligence.service;

import com.example.praxis.apiquickstart.riskintelligence.dto.VwIndicadoresIncidenteDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.filter.VwIndicadoresIncidenteFilterDTO;
import com.example.praxis.apiquickstart.riskintelligence.entity.VwIndicadoresIncidente;
import com.example.praxis.apiquickstart.riskintelligence.mapper.VwIndicadoresIncidenteMapper;
import com.example.praxis.apiquickstart.riskintelligence.repository.VwIndicadoresIncidenteRepository;
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

/**
 * Service read-only de indicadores agregados de incidente.
 *
 * <p>Este e um dos melhores exemplos do quickstart para mostrar como uma view analitica participa
 * do baseline completo da plataforma: stats declarativos, option sources dinamicos e leitura
 * consultiva sem mutacao.</p>
 */
@Service
public class VwIndicadoresIncidenteService extends AbstractQuickstartReadOnlyService<VwIndicadoresIncidente, VwIndicadoresIncidenteDTO, Integer, VwIndicadoresIncidenteFilterDTO> {

    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("missao", "missao", Set.of(StatsMetric.COUNT))
            .groupByBucket("local", "local", Set.of(StatsMetric.COUNT))
            .groupByBucket("severidade", "severidade", Set.of(StatsMetric.COUNT))
            .temporalTimeSeriesField("ocorridoEm", "ocorridoEm")
            .numericHistogramMeasureField("danosCivis", "danosCivis")
            .numericHistogramMeasureField("totalIndenizacoes", "totalIndenizacoes")
            .numericHistogramMeasureField("totalPago", "totalPago")
            .numericHistogramMeasureField("totalPendente", "totalPendente")
            .build();

    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(VwIndicadoresIncidente.class, new OptionSourceDescriptor(
                    "severidade",
                    OptionSourceType.DISTINCT_DIMENSION,
                com.example.praxis.apiquickstart.constants.ApiPaths.RiskIntelligence.VW_INDICADORES_INCIDENTES,
                    "severidade",
                    "severidade",
                    "severidade",
                    "severidade",
                    List.of("ocorridoEmBetween"),
                    new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label")
            ))
            .build();

    private final VwIndicadoresIncidenteMapper mapper;

    public VwIndicadoresIncidenteService(VwIndicadoresIncidenteRepository repository, VwIndicadoresIncidenteMapper mapper) {
        super(repository, VwIndicadoresIncidente.class, mapper::toDto, VwIndicadoresIncidente::getIncidenteId);
        this.mapper = mapper;
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

    /** Exponibiliza option sources reutilizados pelo host ao montar o registro global. */
    public static OptionSourceRegistry optionSources() {
        return OPTION_SOURCES;
    }

    @Override
    public OptionSourceRegistry getOptionSourceRegistry() {
        return OPTION_SOURCES;
    }
}



