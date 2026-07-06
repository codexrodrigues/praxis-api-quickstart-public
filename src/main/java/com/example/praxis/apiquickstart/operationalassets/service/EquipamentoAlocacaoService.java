package com.example.praxis.apiquickstart.operationalassets.service;

import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.operationalassets.dto.EquipamentoAlocacaoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateEquipamentoAlocacaoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateEquipamentoAlocacaoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.actions.AssetAvailabilityWorkflowRequestDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.actions.AssetAvailabilityWorkflowResultDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.EquipamentoAlocacaoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.Equipamento;
import com.example.praxis.apiquickstart.operationalassets.entity.EquipamentoAlocacao;
import com.example.praxis.apiquickstart.operationalassets.enums.AlocacaoStatus;
import com.example.praxis.apiquickstart.operationalassets.enums.EquipamentoStatus;
import com.example.praxis.apiquickstart.operationalassets.mapper.EquipamentoAlocacaoMapper;
import com.example.praxis.apiquickstart.operationalassets.repository.EquipamentoAlocacaoRepository;
import com.example.praxis.apiquickstart.operationalassets.repository.EquipamentoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
/**
 * Service de referência para alocação e histórico de uso de equipamentos.
 *
 * <p>No quickstart, esta classe evidencia que rastreabilidade patrimonial também
 * pode ser modelada no pipeline CRUD genérico da plataforma. O merge explícito
 * ajuda a explicar o ponto em que regras futuras de disponibilidade, devolução
 * ou auditoria podem ser incorporadas sem alterar o contrato público já exposto
 * pelo recurso de alocação.</p>
 */
public class EquipamentoAlocacaoService extends AbstractQuickstartCrudService<EquipamentoAlocacao, EquipamentoAlocacaoDTO, Integer, EquipamentoAlocacaoFilterDTO, CreateEquipamentoAlocacaoDTO, UpdateEquipamentoAlocacaoDTO> {
    private static final String RESOURCE_KEY = "assets.equipamento-alocacoes";
    private static final String WORKFLOW_POLICY_TARGET_PREFIX = RESOURCE_KEY + ":";
    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("status", "status", Set.of(StatsMetric.COUNT))
            .groupByBucket("equipamentoId", "equipamento.id", Set.of(StatsMetric.COUNT))
            .groupByBucket("funcionarioId", "funcionario.id", Set.of(StatsMetric.COUNT))
            .temporalTimeSeriesField("inicio", "inicio")
            .temporalTimeSeriesField("fim", "fim")
            .build();

    private final EquipamentoAlocacaoMapper mapper;
    private final EquipamentoAlocacaoRepository repository;
    private final EquipamentoRepository equipamentoRepository;
    private final DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    public EquipamentoAlocacaoService(
            EquipamentoAlocacaoRepository repository,
            EquipamentoAlocacaoMapper mapper,
            EquipamentoRepository equipamentoRepository,
            DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver) {
        super(repository, EquipamentoAlocacao.class, mapper::toDto, mapper::toEntity, mapper::toEntity, EquipamentoAlocacao::getId);
        this.mapper = mapper;
        this.repository = repository;
        this.equipamentoRepository = equipamentoRepository;
        this.workflowActionPolicyResolver = workflowActionPolicyResolver;
    }

    @Override
    public EquipamentoAlocacao mergeUpdate(EquipamentoAlocacao existing, EquipamentoAlocacao fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Transactional(readOnly = true)
    public List<EquipamentoAlocacaoDTO> findByEquipamentoIdForEquipmentSurface(Integer equipamentoId) {
        return repository.findByEquipamentoId(equipamentoId).stream()
                .map(mapper::toDto)
                .toList();
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

    @Transactional
    public AssetAvailabilityWorkflowResultDTO returnCustody(Integer id, AssetAvailabilityWorkflowRequestDTO dto) {
        return transitionCustody(
                id,
                "return-custody",
                List.of(AlocacaoStatus.ATIVO),
                AlocacaoStatus.DEVOLVIDO,
                EquipamentoStatus.ESTOQUE,
                dto,
                "Custodia encerrada e equipamento devolvido ao estoque operacional"
        );
    }

    @Transactional
    public AssetAvailabilityWorkflowResultDTO reportLost(Integer id, AssetAvailabilityWorkflowRequestDTO dto) {
        return transitionCustody(
                id,
                "report-lost",
                List.of(AlocacaoStatus.ATIVO),
                AlocacaoStatus.PERDIDO,
                EquipamentoStatus.PERDIDO,
                requiredReason(dto, "Reportar perda exige motivo para auditoria patrimonial."),
                dto,
                "Perda registrada e equipamento removido da disponibilidade"
        );
    }

    @Transactional
    public AssetAvailabilityWorkflowResultDTO reportDamaged(Integer id, AssetAvailabilityWorkflowRequestDTO dto) {
        return transitionCustody(
                id,
                "report-damaged",
                List.of(AlocacaoStatus.ATIVO),
                AlocacaoStatus.DANIFICADO,
                EquipamentoStatus.QUEBRADO,
                requiredReason(dto, "Reportar dano exige motivo para auditoria patrimonial."),
                dto,
                "Dano registrado e equipamento bloqueado para novas alocacoes"
        );
    }

    private AssetAvailabilityWorkflowResultDTO transitionCustody(
            Integer id,
            String actionId,
            List<AlocacaoStatus> allowedStates,
            AlocacaoStatus targetStatus,
            EquipamentoStatus targetEquipmentStatus,
            AssetAvailabilityWorkflowRequestDTO dto,
            String message
    ) {
        return transitionCustody(id, actionId, allowedStates, targetStatus, targetEquipmentStatus, null, dto, message);
    }

    private AssetAvailabilityWorkflowResultDTO transitionCustody(
            Integer id,
            String actionId,
            List<AlocacaoStatus> allowedStates,
            AlocacaoStatus targetStatus,
            EquipamentoStatus targetEquipmentStatus,
            String targetReason,
            AssetAvailabilityWorkflowRequestDTO dto,
            String message
    ) {
        EquipamentoAlocacao alocacao = getRepository().findById(id).orElseThrow(this::getNotFoundException);
        AlocacaoStatus previousStatus = alocacao.getStatus();
        enforceWorkflowActionPolicy(actionId, previousStatus == null ? null : previousStatus.name());
        if (previousStatus == null || !allowedStates.contains(previousStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Estado atual nao permite esta action: "
                    + (previousStatus == null ? "" : previousStatus.name()));
        }

        alocacao.setStatus(targetStatus);
        alocacao.setFim(OffsetDateTime.now());
        Equipamento equipamento = alocacao.getEquipamento();
        if (equipamento == null || equipamento.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alocacao sem equipamento associado.");
        }
        equipamento.setStatus(targetEquipmentStatus);
        equipamentoRepository.save(equipamento);
        EquipamentoAlocacao saved = getRepository().save(alocacao);

        AssetAvailabilityWorkflowResultDTO result = new AssetAvailabilityWorkflowResultDTO();
        result.setId(saved.getId());
        result.setNome(equipamento.getNome());
        result.setAssetType("equipment-custody");
        result.setStatusAnterior(previousStatus.name());
        result.setStatusAtual(saved.getStatus().name());
        result.setMotivo(targetReason == null ? (dto == null ? null : dto.getMotivo()) : targetReason);
        result.setMensagem(message);
        return result;
    }

    private void enforceWorkflowActionPolicy(String actionId, String currentStatus) {
        workflowActionPolicyResolver.resolveAppliedPolicy(WORKFLOW_POLICY_TARGET_PREFIX + actionId)
                .filter(policy -> policy.appliesToState(currentStatus))
                .map(DomainRuleWorkflowActionPolicy::message)
                .filter(StringUtils::hasText)
                .ifPresent(message -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, message);
                });
    }

    private static String requiredReason(AssetAvailabilityWorkflowRequestDTO dto, String fallbackMessage) {
        if (dto == null || !StringUtils.hasText(dto.getMotivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, fallbackMessage);
        }
        return dto.getMotivo().trim();
    }
}




