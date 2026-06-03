package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.dto.MissaoEventoDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateMissaoEventoDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateMissaoEventoDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.MissaoEventoFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.MissaoEvento;
import com.example.praxis.apiquickstart.operations.mapper.MissaoEventoMapper;
import com.example.praxis.apiquickstart.operations.repository.MissaoEventoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
/**
 * Service de referência para persistência e atualização de eventos de missão.
 *
 * <p>No quickstart, esta classe mostra a forma mais simples de conectar um
 * recurso cronológico ao pipeline genérico da plataforma: repositório JPA,
 * mapeamento DTO-entidade e merge explícito de atualização. Ela é útil para
 * ensinar que a semântica temporal do recurso vive no domínio e no filtro,
 * enquanto o host reutiliza a infraestrutura comum do starter.</p>
 */
public class MissaoEventoService extends AbstractQuickstartCrudService<MissaoEvento, MissaoEventoDTO, Integer, MissaoEventoFilterDTO, CreateMissaoEventoDTO, UpdateMissaoEventoDTO> {

    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("tipo", "tipo", Set.of(StatsMetric.COUNT))
            .groupByBucket("missaoStatus", "missao.status", Set.of(StatsMetric.COUNT))
            .groupByBucket("missaoPrioridade", "missao.prioridade", Set.of(StatsMetric.COUNT))
            .temporalTimeSeriesField("ocorridoEm", "ocorridoEm")
            .build();

    private final MissaoEventoMapper mapper;

    public MissaoEventoService(MissaoEventoRepository repository, MissaoEventoMapper mapper) {
        super(repository, MissaoEvento.class, mapper::toDto, mapper::toEntity, mapper::toEntity, MissaoEvento::getId);
        this.mapper = mapper;
    }

    @Override
    public MissaoEvento mergeUpdate(MissaoEvento existing, MissaoEvento fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
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

    public List<MissaoEventoDTO> findTop20ByMissaoIdForTimeline(Integer missaoId) {
        return ((MissaoEventoRepository) getRepository()).findTop20ByMissaoIdForTimeline(missaoId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}







