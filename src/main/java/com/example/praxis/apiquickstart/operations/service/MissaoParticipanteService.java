package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.dto.MissaoParticipanteDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateMissaoParticipanteDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateMissaoParticipanteDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.MissaoParticipanteFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.MissaoParticipante;
import com.example.praxis.apiquickstart.operations.mapper.MissaoParticipanteMapper;
import com.example.praxis.apiquickstart.operations.repository.MissaoParticipanteRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
/**
 * Service de referência para vínculos de participantes em missões.
 *
 * <p>Esta implementação mantém o recurso no fluxo CRUD padrão do quickstart,
 * mas deixa claro para quem estuda o projeto que a composição da missão é uma
 * regra de domínio própria, não apenas uma tabela auxiliar. O merge explícito
 * documenta onde a atualização controlada do vínculo pode evoluir para regras
 * mais específicas sem quebrar o contrato público do recurso.</p>
 */
public class MissaoParticipanteService extends AbstractQuickstartCrudService<MissaoParticipante, MissaoParticipanteDTO, Integer, MissaoParticipanteFilterDTO, CreateMissaoParticipanteDTO, UpdateMissaoParticipanteDTO> {

    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("papel", "papel", Set.of(StatsMetric.COUNT))
            .groupByBucket("resultado", "resultado", Set.of(StatsMetric.COUNT))
            .groupByBucket("missaoStatus", "missao.status", Set.of(StatsMetric.COUNT))
            .groupByBucket("missaoPrioridade", "missao.prioridade", Set.of(StatsMetric.COUNT))
            .distinctCountField("funcionarioId", "funcionario.id")
            .build();

    private final MissaoParticipanteMapper mapper;

    public MissaoParticipanteService(MissaoParticipanteRepository repository, MissaoParticipanteMapper mapper) {
        super(repository, MissaoParticipante.class, mapper::toDto, mapper::toEntity, mapper::toEntity, MissaoParticipante::getId);
        this.mapper = mapper;
    }

    @Override
    public MissaoParticipante mergeUpdate(MissaoParticipante existing, MissaoParticipante fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
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

    public List<MissaoParticipanteDTO> findByMissaoIdForCommandCenter(Integer missaoId) {
        return ((MissaoParticipanteRepository) getRepository()).findByMissaoIdForCommandCenter(missaoId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public List<MissaoParticipanteDTO> findByFuncionarioIdForEmployeeSurface(Integer funcionarioId) {
        return ((MissaoParticipanteRepository) getRepository()).findByFuncionarioIdForEmployeeSurface(funcionarioId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}







