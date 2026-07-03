package com.example.praxis.apiquickstart.operationalassets.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.operationalassets.dto.VeiculoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateVeiculoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateVeiculoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.actions.AssetAvailabilityWorkflowRequestDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.actions.AssetAvailabilityWorkflowResultDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.VeiculoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.Veiculo;
import com.example.praxis.apiquickstart.operationalassets.enums.VeiculoStatus;
import com.example.praxis.apiquickstart.operationalassets.mapper.VeiculoMapper;
import com.example.praxis.apiquickstart.operationalassets.repository.VeiculoRepository;
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

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Service basico de veiculos usado para mostrar o caminho mais simples de um ativo CRUD.
 *
 * <p>Neste caso, o valor pedagogico esta justamente na simplicidade: quando nao ha workflow ou
 * surface especial, o quickstart ainda deixa explicito o merge controlado do agregado e o encaixe
 * no pipeline canonico da plataforma.</p>
 */
@Service
public class VeiculoService extends AbstractQuickstartCrudService<Veiculo, VeiculoDTO, Integer, VeiculoFilterDTO, CreateVeiculoDTO, UpdateVeiculoDTO> {
    private static final String RESOURCE_KEY = "assets.veiculos";
    private static final String WORKFLOW_POLICY_TARGET_PREFIX = RESOURCE_KEY + ":";
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Veiculo.class, new OptionSourceDescriptor(
                    ApiPaths.Assets.VEICULOS_VEHICLE_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Assets.VEICULOS,
                    "veiculoId",
                    "id",
                    "nome",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Assets.VEICULOS_VEHICLE_LOOKUP_SOURCE,
                            null,
                            List.of("tipo", "capacidade"),
                            "status",
                            null,
                            null,
                            List.of("nome"),
                            null,
                            new LookupSelectionPolicy(
                                    null,
                                    "status",
                                    List.of("OPERACIONAL"),
                                    List.of("MANUTENCAO", "INOPERANTE"),
                                    true,
                                    "Veiculo indisponivel preservado apenas para reidratacao de valores existentes.",
                                    "Selecione um veiculo operacional para nova sortie."
                            ),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Assets.VEICULOS + "/{id}", "/assets/veiculos/{id}", "route")
                    )
            ))
            .build();

    private final VeiculoMapper mapper;
    private final DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    public VeiculoService(
            VeiculoRepository repository,
            VeiculoMapper mapper,
            DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver) {
        super(repository, Veiculo.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Veiculo::getId);
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
    public Veiculo mergeUpdate(Veiculo existing, Veiculo fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Transactional
    public AssetAvailabilityWorkflowResultDTO sendToMaintenance(Integer id, AssetAvailabilityWorkflowRequestDTO dto) {
        return transitionStatus(
                id,
                "send-to-maintenance",
                List.of(VeiculoStatus.OPERACIONAL),
                VeiculoStatus.MANUTENCAO,
                dto,
                "Veiculo retirado da frota operacional para manutencao"
        );
    }

    @Transactional
    public AssetAvailabilityWorkflowResultDTO returnToOperation(Integer id, AssetAvailabilityWorkflowRequestDTO dto) {
        return transitionStatus(
                id,
                "return-to-operation",
                List.of(VeiculoStatus.MANUTENCAO, VeiculoStatus.INOPERANTE),
                VeiculoStatus.OPERACIONAL,
                dto,
                "Veiculo liberado para operacao"
        );
    }

    private AssetAvailabilityWorkflowResultDTO transitionStatus(
            Integer id,
            String actionId,
            List<VeiculoStatus> allowedStates,
            VeiculoStatus targetStatus,
            AssetAvailabilityWorkflowRequestDTO dto,
            String message
    ) {
        Veiculo veiculo = getRepository().findById(id).orElseThrow(this::getNotFoundException);
        VeiculoStatus previousStatus = veiculo.getStatus();
        enforceWorkflowActionPolicy(actionId, previousStatus == null ? null : previousStatus.name());
        if (previousStatus == null || !allowedStates.contains(previousStatus)) {
            throw new ResponseStatusException(CONFLICT, "Estado atual nao permite esta action: " + (previousStatus == null ? "" : previousStatus.name()));
        }

        veiculo.setStatus(targetStatus);
        Veiculo saved = getRepository().save(veiculo);
        return result(saved.getId(), saved.getNome(), previousStatus.name(), targetStatus.name(), dto, message);
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
            String previousStatus,
            String currentStatus,
            AssetAvailabilityWorkflowRequestDTO dto,
            String message
    ) {
        AssetAvailabilityWorkflowResultDTO result = new AssetAvailabilityWorkflowResultDTO();
        result.setId(id);
        result.setNome(name);
        result.setAssetType("vehicle");
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





