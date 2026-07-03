package com.example.praxis.apiquickstart.operationalassets.service;

import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.operationalassets.dto.EquipamentoDTO;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateEquipamentoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateEquipamentoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.actions.AssetAvailabilityWorkflowRequestDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.actions.AssetAvailabilityWorkflowResultDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.EquipamentoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.Equipamento;
import com.example.praxis.apiquickstart.operationalassets.enums.EquipamentoStatus;
import com.example.praxis.apiquickstart.operationalassets.mapper.EquipamentoMapper;
import com.example.praxis.apiquickstart.operationalassets.repository.EquipamentoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
/**
 * Service de referência para cadastro de equipamentos.
 *
 * <p>Esta implementação mostra o caminho básico para persistir ativos no host
 * operacional de referência da Praxis: repositório, mapeamento explícito e merge
 * controlado de atualização. O valor didático aqui é mostrar que o recurso de
 * inventário reaproveita a infraestrutura comum do quickstart sem perder clareza
 * sobre onde regras de domínio adicionais deveriam evoluir.</p>
 */
public class EquipamentoService extends AbstractQuickstartCrudService<Equipamento, EquipamentoDTO, Integer, EquipamentoFilterDTO, CreateEquipamentoDTO, UpdateEquipamentoDTO> {
    private static final String RESOURCE_KEY = "assets.equipamentos";
    private static final String WORKFLOW_POLICY_TARGET_PREFIX = RESOURCE_KEY + ":";
    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("status", "status", Set.of(StatsMetric.COUNT))
            .groupByBucket("tipo", "tipo", Set.of(StatsMetric.COUNT))
            .numericHistogramMeasureField("resistencia", "resistencia")
            .build();
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Equipamento.class, new OptionSourceDescriptor(
                    ApiPaths.Assets.EQUIPAMENTOS_EQUIPMENT_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Assets.EQUIPAMENTOS,
                    "equipamentoId",
                    "id",
                    "nome",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Assets.EQUIPAMENTOS_EQUIPMENT_LOOKUP_SOURCE,
                            null,
                            List.of("tipo", "resistencia"),
                            "status",
                            null,
                            null,
                            List.of("nome"),
                            null,
                            new LookupSelectionPolicy(
                                    null,
                                    "status",
                                    List.of("ESTOQUE", "EM_USO"),
                                    List.of("MANUTENCAO", "QUEBRADO", "PERDIDO"),
                                    true,
                                    "Equipamento indisponivel preservado apenas para reidratacao de custodias existentes.",
                                    "Selecione um equipamento em estoque ou em uso operacional para nova alocacao."
                            ),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Assets.EQUIPAMENTOS + "/{id}", "/assets/equipamentos/{id}", "route")
                    )
            ))
            .build();

    private final EquipamentoMapper mapper;
    private final DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    public EquipamentoService(
            EquipamentoRepository repository,
            EquipamentoMapper mapper,
            DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver) {
        super(repository, Equipamento.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Equipamento::getId);
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
    public Equipamento mergeUpdate(Equipamento existing, Equipamento fromPayload) {
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

    @Transactional
    public AssetAvailabilityWorkflowResultDTO sendToMaintenance(Integer id, AssetAvailabilityWorkflowRequestDTO dto) {
        return transitionStatus(
                id,
                "send-to-maintenance",
                List.of(EquipamentoStatus.ESTOQUE, EquipamentoStatus.EM_USO),
                EquipamentoStatus.MANUTENCAO,
                dto,
                "Equipamento retirado da disponibilidade operacional para manutencao"
        );
    }

    @Transactional
    public AssetAvailabilityWorkflowResultDTO returnToStock(Integer id, AssetAvailabilityWorkflowRequestDTO dto) {
        return transitionStatus(
                id,
                "return-to-stock",
                List.of(EquipamentoStatus.MANUTENCAO),
                EquipamentoStatus.ESTOQUE,
                dto,
                "Equipamento liberado para estoque operacional"
        );
    }

    private AssetAvailabilityWorkflowResultDTO transitionStatus(
            Integer id,
            String actionId,
            List<EquipamentoStatus> allowedStates,
            EquipamentoStatus targetStatus,
            AssetAvailabilityWorkflowRequestDTO dto,
            String message
    ) {
        Equipamento equipamento = getRepository().findById(id).orElseThrow(this::getNotFoundException);
        EquipamentoStatus previousStatus = equipamento.getStatus();
        enforceWorkflowActionPolicy(actionId, previousStatus == null ? null : previousStatus.name());
        if (previousStatus == null || !allowedStates.contains(previousStatus)) {
            throw new ResponseStatusException(CONFLICT, "Estado atual nao permite esta action: " + (previousStatus == null ? "" : previousStatus.name()));
        }

        equipamento.setStatus(targetStatus);
        Equipamento saved = getRepository().save(equipamento);
        return result(saved.getId(), saved.getNome(), "equipment", previousStatus.name(), targetStatus.name(), dto, message);
    }

    private void enforceWorkflowActionPolicy(String actionId, String currentStatus) {
        workflowActionPolicyResolver.resolveAppliedPolicy(WORKFLOW_POLICY_TARGET_PREFIX + actionId)
                .filter(policy -> policy.appliesToState(currentStatus))
                .map(DomainRuleWorkflowActionPolicy::message)
                .filter(StringUtils::hasText)
                .ifPresent(message -> {
                    throw new ResponseStatusException(CONFLICT, message);
                });
    }

    private static AssetAvailabilityWorkflowResultDTO result(
            Integer id,
            String name,
            String assetType,
            String previousStatus,
            String currentStatus,
            AssetAvailabilityWorkflowRequestDTO dto,
            String message
    ) {
        AssetAvailabilityWorkflowResultDTO result = new AssetAvailabilityWorkflowResultDTO();
        result.setId(id);
        result.setNome(name);
        result.setAssetType(assetType);
        result.setStatusAnterior(previousStatus);
        result.setStatusAtual(currentStatus);
        result.setMotivo(dto == null ? null : dto.getMotivo());
        result.setMensagem(message);
        return result;
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




