package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.FeriasAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateFeriasAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFeriasAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.AbsenceCoverageWorkflowRequestDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.AbsenceCoverageWorkflowResultDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.FeriasAfastamentoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.FeriasAfastamento;
import com.example.praxis.apiquickstart.hr.mapper.FeriasAfastamentoMapper;
import com.example.praxis.apiquickstart.hr.repository.FeriasAfastamentoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Set;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class FeriasAfastamentoService extends AbstractQuickstartCrudService<FeriasAfastamento, FeriasAfastamentoDTO, Integer, FeriasAfastamentoFilterDTO, CreateFeriasAfastamentoDTO, UpdateFeriasAfastamentoDTO> {

    private static final int OBSERVACOES_MAX_LENGTH = 2000;

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

    @Transactional
    public AbsenceCoverageWorkflowResultDTO planCoverage(Integer id, AbsenceCoverageWorkflowRequestDTO dto) {
        FeriasAfastamento absence = getRepository().findById(id).orElseThrow(this::getNotFoundException);

        String previousNotes = normalize(absence.getObservacoes());
        String coverageEvidence = buildCoverageEvidence(dto);
        String mergedNotes = previousNotes.isBlank() ? coverageEvidence : previousNotes + "\n" + coverageEvidence;
        if (mergedNotes.length() > OBSERVACOES_MAX_LENGTH) {
            throw new ResponseStatusException(CONFLICT, "Coverage evidence exceeds absence notes capacity.");
        }

        absence.setObservacoes(mergedNotes);
        FeriasAfastamento saved = refreshManaged(getRepository().save(absence));
        return buildCoverageResult(saved, dto, "Cobertura da ausencia registrada.");
    }

    private String buildCoverageEvidence(AbsenceCoverageWorkflowRequestDTO dto) {
        StringBuilder builder = new StringBuilder("[COBERTURA_PLANEJADA ");
        builder.append(LocalDate.now()).append("] ");
        builder.append(normalize(dto.getPlanoCobertura()));
        if (dto.getSubstitutoFuncionarioId() != null) {
            builder.append(" | substitutoFuncionarioId=").append(dto.getSubstitutoFuncionarioId());
        }
        String justification = normalize(dto.getJustificativa());
        if (!justification.isBlank()) {
            builder.append(" | justificativa=").append(justification);
        }
        return builder.toString();
    }

    private AbsenceCoverageWorkflowResultDTO buildCoverageResult(
            FeriasAfastamento absence,
            AbsenceCoverageWorkflowRequestDTO dto,
            String message
    ) {
        AbsenceCoverageWorkflowResultDTO result = new AbsenceCoverageWorkflowResultDTO();
        result.setId(absence.getId());
        result.setTipo(absence.getTipo());
        result.setDataInicio(absence.getDataInicio());
        result.setDataFim(absence.getDataFim());
        result.setPlanoCobertura(dto.getPlanoCobertura());
        result.setSubstitutoFuncionarioId(dto.getSubstitutoFuncionarioId());
        result.setJustificativa(dto.getJustificativa());
        result.setObservacoes(absence.getObservacoes());
        result.setMensagem(message);
        return result;
    }

    private FeriasAfastamento refreshManaged(FeriasAfastamento entity) {
        if (getEntityManager() == null) {
            return entity;
        }
        getEntityManager().flush();
        FeriasAfastamento managed = getEntityManager().contains(entity) ? entity : getEntityManager().merge(entity);
        getEntityManager().refresh(managed);
        return managed;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

