package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.dto.VwResumoMissoeDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.VwResumoMissoeFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.VwResumoMissoe;
import com.example.praxis.apiquickstart.operations.mapper.VwResumoMissoeMapper;
import com.example.praxis.apiquickstart.operations.repository.VwResumoMissoeRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartReadOnlyService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
/**
 * Service read-only de referência para visão agregada de missões.
 *
 * <p>No quickstart, esta implementação explica como um recurso analítico derivado
 * entra no ecossistema Praxis sem rota especial ou controller customizado pesado.
 * Ela orienta consumidores a tratar a visão como fonte de consulta e dashboard,
 * enquanto o estado transacional continua pertencendo aos recursos primários do
 * domínio operacional.</p>
 */
public class VwResumoMissoeService extends AbstractQuickstartReadOnlyService<VwResumoMissoe, VwResumoMissoeDTO, Integer, VwResumoMissoeFilterDTO> {

    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("status", "status", Set.of(StatsMetric.COUNT))
            .groupByBucket("prioridade", "prioridade", Set.of(StatsMetric.COUNT))
            .groupByBucket("local", "local", Set.of(StatsMetric.COUNT))
            .groupByBucket("ameaca", "ameaca", Set.of(StatsMetric.COUNT))
            .numericHistogramMeasureField("qtdHerois", "qtdHerois")
            .numericHistogramMeasureField("qtdEventos", "qtdEventos")
            .temporalTimeSeriesField("primeiraAcao", "primeiraAcao")
            .temporalTimeSeriesField("ultimaAcao", "ultimaAcao")
            .build();

    private final VwResumoMissoeMapper mapper;

    public VwResumoMissoeService(VwResumoMissoeRepository repository, VwResumoMissoeMapper mapper) {
        super(repository, VwResumoMissoe.class, mapper::toDto, VwResumoMissoe::getMissaoId);
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
}





