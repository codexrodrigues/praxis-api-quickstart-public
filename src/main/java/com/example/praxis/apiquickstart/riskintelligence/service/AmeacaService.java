package com.example.praxis.apiquickstart.riskintelligence.service;

import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.riskintelligence.dto.AmeacaDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.CreateAmeacaDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.UpdateAmeacaDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.actions.ThreatTriageWorkflowRequestDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.actions.ThreatTriageWorkflowResultDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.filter.AmeacaFilterDTO;
import com.example.praxis.apiquickstart.riskintelligence.entity.Ameaca;
import com.example.praxis.apiquickstart.riskintelligence.enums.AmeacaStatus;
import com.example.praxis.apiquickstart.riskintelligence.mapper.AmeacaMapper;
import com.example.praxis.apiquickstart.riskintelligence.repository.AmeacaRepository;
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
 * Service de ameacas usado como exemplo simples de cadastro transacional no dominio de risco.
 *
 * <p>O quickstart mantem este service enxuto para reforcar que a camada de risco tambem pode usar
 * o pipeline canonico da plataforma sem complexidade extra quando nao ha workflow ou surface
 * especializada.</p>
 */
@Service
public class AmeacaService extends AbstractQuickstartCrudService<Ameaca, AmeacaDTO, Integer, AmeacaFilterDTO, CreateAmeacaDTO, UpdateAmeacaDTO> {
    private static final String RESOURCE_KEY = "risk-intelligence.ameacas";
    private static final String WORKFLOW_POLICY_TARGET_PREFIX = RESOURCE_KEY + ":";
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Ameaca.class, new OptionSourceDescriptor(
                    ApiPaths.RiskIntelligence.AMEACAS_THREAT_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.RiskIntelligence.AMEACAS,
                    "ameacaId",
                    "id",
                    "nome",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.RiskIntelligence.AMEACAS_THREAT_LOOKUP_SOURCE,
                            null,
                            List.of("classe", "planeta", "nivel"),
                            "status",
                            null,
                            null,
                            List.of("nome", "classe", "planeta"),
                            null,
                            new LookupSelectionPolicy(
                                    null,
                                    "status",
                                    List.of("LIVRE", "EM_OBSERVACAO", "CONFRONTO"),
                                    List.of("CAPTURADO", "ELIMINADO"),
                                    true,
                                    "Ameaca encerrada preservada apenas para reidratacao de valores existentes.",
                                    "Selecione uma ameaca em ciclo operacional."
                            ),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.RiskIntelligence.AMEACAS + "/{id}", "/risk-intelligence/ameacas/{id}", "route")
                    )
            ))
            .build();

    private final AmeacaMapper mapper;
    private final DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    public AmeacaService(
            AmeacaRepository repository,
            AmeacaMapper mapper,
            DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver) {
        super(repository, Ameaca.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Ameaca::getId);
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
    public Ameaca mergeUpdate(Ameaca existing, Ameaca fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Transactional
    public ThreatTriageWorkflowResultDTO markUnderObservation(Integer id, ThreatTriageWorkflowRequestDTO dto) {
        return transitionStatus(
                id,
                "mark-under-observation",
                List.of(AmeacaStatus.LIVRE, AmeacaStatus.CONFRONTO),
                AmeacaStatus.EM_OBSERVACAO,
                dto,
                "Ameaca colocada em observacao ativa pela inteligencia de risco"
        );
    }

    @Transactional
    public ThreatTriageWorkflowResultDTO markCaptured(Integer id, ThreatTriageWorkflowRequestDTO dto) {
        return transitionStatus(
                id,
                "mark-captured",
                List.of(AmeacaStatus.LIVRE, AmeacaStatus.EM_OBSERVACAO, AmeacaStatus.CONFRONTO),
                AmeacaStatus.CAPTURADO,
                dto,
                "Ameaca encerrada como capturada e removida de novas selecoes operacionais"
        );
    }

    private ThreatTriageWorkflowResultDTO transitionStatus(
            Integer id,
            String actionId,
            List<AmeacaStatus> allowedStates,
            AmeacaStatus targetStatus,
            ThreatTriageWorkflowRequestDTO dto,
            String message
    ) {
        Ameaca threat = getRepository().findById(id).orElseThrow(this::getNotFoundException);
        AmeacaStatus previousStatus = threat.getStatus();
        enforceWorkflowActionPolicy(actionId, previousStatus == null ? null : previousStatus.name());
        if (previousStatus == null || !allowedStates.contains(previousStatus)) {
            throw new ResponseStatusException(CONFLICT, "Estado atual nao permite esta action: " + (previousStatus == null ? "" : previousStatus.name()));
        }

        threat.setStatus(targetStatus);
        Ameaca saved = getRepository().save(threat);
        return result(saved, previousStatus.name(), targetStatus.name(), dto, message);
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

    private static ThreatTriageWorkflowResultDTO result(
            Ameaca threat,
            String previousStatus,
            String currentStatus,
            ThreatTriageWorkflowRequestDTO dto,
            String message
    ) {
        ThreatTriageWorkflowResultDTO result = new ThreatTriageWorkflowResultDTO();
        result.setId(threat.getId());
        result.setNome(threat.getNome());
        result.setClasse(threat.getClasse() == null ? null : threat.getClasse().name());
        result.setNivel(threat.getNivel());
        result.setStatusAnterior(previousStatus);
        result.setStatusAtual(currentStatus);
        result.setMotivo(dto == null ? null : dto.getMotivo());
        result.setMensagem(message);
        return result;
    }

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label");
    }
}




