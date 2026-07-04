package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.FeriasAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateFeriasAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFeriasAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.FeriasAfastamentoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.FeriasAfastamento;
import com.example.praxis.apiquickstart.hr.mapper.FeriasAfastamentoMapper;
import com.example.praxis.apiquickstart.hr.repository.FeriasAfastamentoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class FeriasAfastamentoService extends AbstractQuickstartCrudService<FeriasAfastamento, FeriasAfastamentoDTO, Integer, FeriasAfastamentoFilterDTO, CreateFeriasAfastamentoDTO, UpdateFeriasAfastamentoDTO> {

    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("tipo", "tipo", Set.of(StatsMetric.COUNT))
            .groupByBucket("funcionarioId", "funcionario.id", Set.of(StatsMetric.COUNT))
            .temporalTimeSeriesField("dataInicio", "dataInicio")
            .temporalTimeSeriesField("dataFim", "dataFim")
            .build();

    private final FeriasAfastamentoMapper mapper;

    public FeriasAfastamentoService(FeriasAfastamentoRepository repository, FeriasAfastamentoMapper mapper) {
        super(repository, FeriasAfastamento.class, mapper::toDto, mapper::toEntity, mapper::toEntity, FeriasAfastamento::getId);
        this.mapper = mapper;
    }

    @Override
    public FeriasAfastamento mergeUpdate(FeriasAfastamento existing, FeriasAfastamento fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Override
    public java.util.Optional<String> getDatasetVersion() {
        long count = getRepository().count();
        return java.util.Optional.of(getEntityClass().getSimpleName() + ":" + count);
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
}


