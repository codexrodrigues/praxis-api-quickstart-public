package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.MissaoDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateMissaoDTO;
import com.example.praxis.apiquickstart.operations.dto.PlanejarEquipeMissaoDTO;
import com.example.praxis.apiquickstart.operations.dto.PlanejarEquipeMissaoParticipanteDTO;
import com.example.praxis.apiquickstart.operations.dto.RescheduleMissaoDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateMissaoDTO;
import com.example.praxis.apiquickstart.operations.dto.actions.MissaoWorkflowRequestDTO;
import com.example.praxis.apiquickstart.operations.dto.actions.MissaoWorkflowResultDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.MissaoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import com.example.praxis.apiquickstart.operations.entity.Missao;
import com.example.praxis.apiquickstart.operations.entity.MissaoParticipante;
import com.example.praxis.apiquickstart.operations.enums.MissaoStatus;
import com.example.praxis.apiquickstart.operations.enums.ResultadoMissao;
import com.example.praxis.apiquickstart.operations.mapper.MissaoMapper;
import com.example.praxis.apiquickstart.operations.repository.MissaoParticipanteRepository;
import com.example.praxis.apiquickstart.operations.repository.MissaoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.options.EntityLookupDescriptor;
import org.praxisplatform.uischema.options.LookupCapabilities;
import org.praxisplatform.uischema.options.LookupDetailDescriptor;
import org.praxisplatform.uischema.options.LookupSelectionPolicy;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Service de missoes que demonstra workflow de item com transicoes de estado explicitas.
 *
 * <p>Ele concentra uma das narrativas mais importantes do quickstart: em Praxis, workflow nao e
 * um detalhe escondido no frontend. O host valida o estado atual, aplica a transicao permitida,
 * persiste timestamps relevantes e devolve um resultado semantico para a action publicada.</p>
 */
@Service
public class MissaoService extends AbstractQuickstartCrudService<Missao, MissaoDTO, Integer, MissaoFilterDTO, CreateMissaoDTO, UpdateMissaoDTO> {

    private static final String WORKFLOW_POLICY_TARGET_PREFIX = "operations.missoes:";
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Missao.class, new OptionSourceDescriptor(
                    ApiPaths.Operations.MISSOES_MISSION_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Operations.MISSOES,
                    null,
                    "id",
                    "titulo",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Operations.MISSOES_MISSION_LOOKUP_SOURCE,
                            null,
                            List.of("prioridade", "local"),
                            "status",
                            null,
                            null,
                            List.of("titulo", "local"),
                            null,
                            new LookupSelectionPolicy(
                                    null,
                                    "status",
                                    List.of("PLANEJADA", "EM_ANDAMENTO", "PAUSADA"),
                                    List.of("CONCLUIDA", "FALHOU"),
                                    true,
                                    "Missao encerrada preservada apenas para reidratacao de valores existentes.",
                                    "Selecione uma missao planejada, em andamento ou pausada."
                            ),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Operations.MISSOES + "/{id}", "/operations/missoes/{id}", "route")
                    )
            ))
            .build();

    private final MissaoRepository repository;
    private final MissaoParticipanteRepository participanteRepository;
    private final MissaoMapper mapper;
    private final DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    public MissaoService(
            MissaoRepository repository,
            MissaoParticipanteRepository participanteRepository,
            MissaoMapper mapper,
            DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver
    ) {
        super(repository, Missao.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Missao::getId);
        this.repository = repository;
        this.participanteRepository = participanteRepository;
        this.mapper = mapper;
        this.workflowActionPolicyResolver = workflowActionPolicyResolver;
    }

    public static OptionSourceRegistry optionSources() {
        return OPTION_SOURCES;
    }

    @Override
    public OptionSourceRegistry getOptionSourceRegistry() {
        return OPTION_SOURCES;
    }

    @Override
    public Missao mergeUpdate(Missao existing, Missao fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Transactional
    public MissaoDTO reschedule(Integer id, RescheduleMissaoDTO dto) {
        Missao existing = findEntityById(id);
        MissaoStatus currentStatus = existing.getStatus();
        if (currentStatus != MissaoStatus.PLANEJADA && currentStatus != MissaoStatus.PAUSADA) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        if (dto.getFimPrev().isBefore(dto.getInicioPrev())) {
            throw new ResponseStatusException(BAD_REQUEST, "fimPrev must be greater than or equal to inicioPrev");
        }
        existing.setLocal(dto.getLocal());
        existing.setInicioPrev(dto.getInicioPrev());
        existing.setFimPrev(dto.getFimPrev());
        existing.setObjetivo(dto.getObjetivo());
        Missao saved = refreshManaged(getRepository().save(existing));
        return mapper.toDto(saved);
    }

    @Transactional
    public MissaoDTO planTeam(Integer id, PlanejarEquipeMissaoDTO dto) {
        Missao existing = findEntityById(id);
        MissaoStatus currentStatus = existing.getStatus();
        if (currentStatus != MissaoStatus.PLANEJADA && currentStatus != MissaoStatus.PAUSADA) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }

        List<PlanejarEquipeMissaoParticipanteDTO> requested = dto.getParticipantes();
        validateTeamPlan(requested);

        Map<Integer, MissaoParticipante> existingById = new LinkedHashMap<>();
        for (MissaoParticipante participante : participanteRepository.findByMissaoIdForPlanning(id)) {
            existingById.put(participante.getId(), participante);
        }

        Set<Integer> retainedIds = requested.stream()
                .map(PlanejarEquipeMissaoParticipanteDTO::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        List<MissaoParticipante> removed = existingById.values()
                .stream()
                .filter(participante -> !retainedIds.contains(participante.getId()))
                .toList();
        participanteRepository.deleteAll(removed);

        List<MissaoParticipante> retainedExisting = existingById.values()
                .stream()
                .filter(participante -> retainedIds.contains(participante.getId()))
                .toList();
        for (MissaoParticipante participante : retainedExisting) {
            participante.setPrincipal(false);
        }
        participanteRepository.saveAll(retainedExisting);
        participanteRepository.flush();

        for (int index = 0; index < requested.size(); index++) {
            PlanejarEquipeMissaoParticipanteDTO item = requested.get(index);
            MissaoParticipante participante = resolveParticipant(existing, existingById, item);
            participante.setFuncionario(funcionarioFromId(item.getFuncionarioId()));
            participante.setPapel(item.getPapel());
            participante.setPrincipal(Boolean.TRUE.equals(item.getPrincipal()));
            participante.setOrdem(index);
            if (participante.getResultado() == null) {
                participante.setResultado(ResultadoMissao.NA);
            }
            participanteRepository.save(participante);
        }

        return mapper.toDto(refreshManaged(existing));
    }

    private void validateTeamPlan(List<PlanejarEquipeMissaoParticipanteDTO> participantes) {
        if (participantes == null || participantes.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "participantes must contain at least one item");
        }

        Set<Integer> funcionarioIds = new HashSet<>();
        int principalCount = 0;
        for (PlanejarEquipeMissaoParticipanteDTO participante : participantes) {
            if (participante.getFuncionarioId() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "participantes.funcionarioId is required");
            }
            if (!funcionarioIds.add(participante.getFuncionarioId())) {
                throw new ResponseStatusException(BAD_REQUEST, "funcionarioId must be unique inside participantes");
            }
            if (participante.getPapel() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "participantes.papel is required");
            }
            if (Boolean.TRUE.equals(participante.getPrincipal())) {
                principalCount++;
            }
        }
        if (principalCount != 1) {
            throw new ResponseStatusException(BAD_REQUEST, "exactly one participante must be principal");
        }
    }

    private MissaoParticipante resolveParticipant(
            Missao missao,
            Map<Integer, MissaoParticipante> existingById,
            PlanejarEquipeMissaoParticipanteDTO item
    ) {
        Integer itemId = item.getId();
        if (itemId == null) {
            MissaoParticipante novo = new MissaoParticipante();
            novo.setMissao(missao);
            return novo;
        }

        MissaoParticipante existing = existingById.get(itemId);
        if (existing == null || !Objects.equals(existing.getMissao().getId(), missao.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "participantes.id does not belong to this mission");
        }
        return existing;
    }

    private Funcionario funcionarioFromId(Integer id) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        return funcionario;
    }

    @Transactional
    public MissaoWorkflowResultDTO start(Integer id, MissaoWorkflowRequestDTO dto) {
        MissaoStatus currentStatus = repository.findStatusById(id)
                .orElseThrow(this::getNotFoundException);
        if (currentStatus != MissaoStatus.PLANEJADA) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        enforceWorkflowActionPolicy("start", currentStatus);
        OffsetDateTime occurredAt = resolveOccurredAt(dto);
        int updated = repository.startMission(id, MissaoStatus.PLANEJADA, MissaoStatus.EM_ANDAMENTO, occurredAt);
        if (updated == 0) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        return buildWorkflowResult(id, MissaoStatus.PLANEJADA, MissaoStatus.EM_ANDAMENTO, dto, occurredAt, occurredAt, null, "Missão iniciada");
    }

    @Transactional
    public MissaoWorkflowResultDTO pause(Integer id, MissaoWorkflowRequestDTO dto) {
        return transitionStatus(id, "pause", MissaoStatus.EM_ANDAMENTO, MissaoStatus.PAUSADA, dto, "Missão pausada");
    }

    @Transactional
    public MissaoWorkflowResultDTO resume(Integer id, MissaoWorkflowRequestDTO dto) {
        return transitionStatus(id, "resume", MissaoStatus.PAUSADA, MissaoStatus.EM_ANDAMENTO, dto, "Missão retomada");
    }

    @Transactional
    public MissaoWorkflowResultDTO complete(Integer id, MissaoWorkflowRequestDTO dto) {
        MissaoStatus currentStatus = repository.findStatusById(id)
                .orElseThrow(this::getNotFoundException);
        if (currentStatus != MissaoStatus.EM_ANDAMENTO) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        enforceWorkflowActionPolicy("complete", currentStatus);
        OffsetDateTime occurredAt = resolveOccurredAt(dto);
        int updated = repository.finishMission(id, MissaoStatus.EM_ANDAMENTO, MissaoStatus.CONCLUIDA, occurredAt);
        if (updated == 0) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        Missao mission = repository.findById(id).orElseThrow(this::getNotFoundException);
        return buildWorkflowResult(id, MissaoStatus.EM_ANDAMENTO, MissaoStatus.CONCLUIDA, dto, occurredAt, mission.getInicioReal(), mission.getFimReal(), "Missão concluída");
    }

    @Transactional
    public MissaoWorkflowResultDTO fail(Integer id, MissaoWorkflowRequestDTO dto) {
        OffsetDateTime occurredAt = resolveOccurredAt(dto);
        MissaoStatus currentStatus = repository.findStatusById(id)
                .orElseThrow(this::getNotFoundException);
        if (currentStatus != MissaoStatus.EM_ANDAMENTO && currentStatus != MissaoStatus.PAUSADA) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        enforceWorkflowActionPolicy("fail", currentStatus);
        int updated = repository.finishMission(id, currentStatus, MissaoStatus.FALHOU, occurredAt);
        if (updated == 0) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        Missao mission = repository.findById(id).orElseThrow(this::getNotFoundException);
        return buildWorkflowResult(id, currentStatus, MissaoStatus.FALHOU, dto, occurredAt, mission.getInicioReal(), mission.getFimReal(), "Missão encerrada com falha");
    }

    private MissaoWorkflowResultDTO transitionStatus(
            Integer id,
            String actionId,
            MissaoStatus expectedStatus,
            MissaoStatus targetStatus,
            MissaoWorkflowRequestDTO dto,
            String message
    ) {
        MissaoStatus currentStatus = repository.findStatusById(id)
                .orElseThrow(this::getNotFoundException);
        if (currentStatus != expectedStatus) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        enforceWorkflowActionPolicy(actionId, currentStatus);
        OffsetDateTime occurredAt = resolveOccurredAt(dto);
        int updated = repository.transitionStatus(id, expectedStatus, targetStatus);
        if (updated == 0) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        Missao mission = repository.findById(id).orElseThrow(this::getNotFoundException);
        return buildWorkflowResult(id, expectedStatus, targetStatus, dto, occurredAt, mission.getInicioReal(), mission.getFimReal(), message);
    }

    private void enforceWorkflowActionPolicy(String actionId, MissaoStatus currentStatus) {
        workflowActionPolicyResolver.resolveAppliedPolicy(WORKFLOW_POLICY_TARGET_PREFIX + actionId)
                .filter(policy -> policy.appliesToState(currentStatus.name()))
                .map(DomainRuleWorkflowActionPolicy::message)
                .filter(StringUtils::hasText)
                .ifPresent(message -> {
                    throw new ResponseStatusException(CONFLICT, message);
                });
    }

    private MissaoWorkflowResultDTO buildWorkflowResult(
            Integer id,
            MissaoStatus previousStatus,
            MissaoStatus currentStatus,
            MissaoWorkflowRequestDTO dto,
            OffsetDateTime occurredAt,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            String message
    ) {
        MissaoWorkflowResultDTO result = new MissaoWorkflowResultDTO();
        result.setId(id);
        result.setStatusAnterior(previousStatus);
        result.setStatusAtual(currentStatus);
        result.setJustificativa(dto.getJustificativa());
        result.setOcorridoEm(occurredAt);
        result.setInicioReal(startedAt);
        result.setFimReal(endedAt);
        result.setMensagem(message);
        return result;
    }

    /** Usa o timestamp informado pela action ou cai para o momento atual do host. */
    private OffsetDateTime resolveOccurredAt(MissaoWorkflowRequestDTO dto) {
        return dto.getOcorridoEm() != null ? dto.getOcorridoEm() : OffsetDateTime.now();
    }

    private Missao refreshManaged(Missao entity) {
        if (getEntityManager() == null) {
            return entity;
        }
        getEntityManager().flush();
        Missao managed = getEntityManager().contains(entity) ? entity : getEntityManager().merge(entity);
        getEntityManager().refresh(managed);
        return managed;
    }

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(
                true,
                true,
                "contains",
                0,
                25,
                100,
                true,
                false,
                "label"
        );
    }
}






