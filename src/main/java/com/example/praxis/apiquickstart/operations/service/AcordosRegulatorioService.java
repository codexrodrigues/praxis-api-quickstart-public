package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.AcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateAcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.dto.ReviewAcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateAcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.dto.actions.AcordoRegulatorioWorkflowRequestDTO;
import com.example.praxis.apiquickstart.operations.dto.actions.AcordoRegulatorioWorkflowResultDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.AcordosRegulatorioFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.AcordosRegulatorio;
import com.example.praxis.apiquickstart.operations.enums.AcordoStatus;
import com.example.praxis.apiquickstart.operations.mapper.AcordosRegulatorioMapper;
import com.example.praxis.apiquickstart.operations.repository.AcordosRegulatorioRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.options.EntityLookupDescriptor;
import org.praxisplatform.uischema.options.LookupCapabilities;
import org.praxisplatform.uischema.options.LookupDetailDescriptor;
import org.praxisplatform.uischema.options.LookupSelectionPolicy;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition;
import org.praxisplatform.uischema.service.base.VersionedCreateUpdateResourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.OptionalLong;
import java.util.Set;

import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Service de acordos regulatorios com foco em transicoes de compliance.
 *
 * <p>Ele demonstra como um recurso regulatorio pode publicar review parcial e workflow de item sem
 * misturar as duas semanticas: review altera metadados documentais; workflow altera o estado de
 * vigencia do acordo.</p>
 */
@Service
public class AcordosRegulatorioService extends AbstractQuickstartCrudService<AcordosRegulatorio, AcordosRegulatorioDTO, Integer, AcordosRegulatorioFilterDTO, CreateAcordosRegulatorioDTO, UpdateAcordosRegulatorioDTO> implements VersionedCreateUpdateResourceService<AcordosRegulatorioDTO, Integer, AcordosRegulatorioFilterDTO, CreateAcordosRegulatorioDTO, UpdateAcordosRegulatorioDTO> {
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(AcordosRegulatorio.class, new OptionSourceDescriptor(
                    ApiPaths.Operations.ACORDOS_REGULATORIOS_AGREEMENT_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Operations.ACORDOS_REGULATORIOS,
                    null,
                    "id",
                    "nome",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Operations.ACORDOS_REGULATORIOS_AGREEMENT_LOOKUP_SOURCE,
                            "jurisdicao",
                            List.of("descricao"),
                            "status",
                            null,
                            null,
                            List.of("nome", "jurisdicao", "descricao"),
                            null,
                            new LookupSelectionPolicy(
                                    null,
                                    "status",
                                    List.of("VIGENTE"),
                                    List.of("SUSPENSO", "REVOGADO"),
                                    true,
                                    "Acordo regulatorio suspenso ou revogado preservado apenas para reidratacao de licencas existentes.",
                                    "Selecione um acordo regulatorio vigente."
                            ),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Operations.ACORDOS_REGULATORIOS + "/{id}", "/operations/acordos-regulatorios/{id}", "route")
                    )
            ))
            .build();


    private final AcordosRegulatorioRepository repository;
    private final AcordosRegulatorioMapper mapper;

    public AcordosRegulatorioService(AcordosRegulatorioRepository repository, AcordosRegulatorioMapper mapper) {
        super(repository, AcordosRegulatorio.class, mapper::toDto, mapper::toEntity, mapper::toEntity, AcordosRegulatorio::getId);
        this.repository = repository;
        this.mapper = mapper;
    }

    public static OptionSourceRegistry optionSources() {
        return OPTION_SOURCES;
    }

    @Override
    public OptionSourceRegistry getOptionSourceRegistry() {
        return OPTION_SOURCES;
    }

    @Override
    public AcordosRegulatorio mergeUpdate(AcordosRegulatorio existing, AcordosRegulatorio fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Override
    @Transactional(readOnly = true)
    public OptionalLong getResourceVersion(Integer id) {
        return repository.findById(id)
                .map(AcordosRegulatorio::getVersion)
                .map(OptionalLong::of)
                .orElseGet(OptionalLong::empty);
    }

    @Override
    @Transactional
    public AcordosRegulatorioDTO update(
            Integer id,
            UpdateAcordosRegulatorioDTO dto,
            ResourceVersionUpdatePrecondition<Integer> precondition
    ) {
        AcordosRegulatorio existing = findEntityById(id);
        precondition.requireMatch(existing.getVersion() == null ? 0L : existing.getVersion());
        beforeUpdate(id, existing, dto);
        getResourceMapper().applyUpdate(existing, dto);
        AcordosRegulatorio saved = repository.saveAndFlush(existing);
        afterUpdate(id, saved, dto);
        return mapper.toDto(saved);
    }

    @Transactional
    public AcordosRegulatorioDTO review(
            Integer id,
            ReviewAcordosRegulatorioDTO dto,
            ResourceVersionUpdatePrecondition<Integer> precondition
    ) {
        AcordosRegulatorio existing = findEntityById(id);
        precondition.requireMatch(existing.getVersion() == null ? 0L : existing.getVersion());
        mapper.updateReview(dto, existing);
        AcordosRegulatorio saved = refreshManaged(getRepository().save(existing));
        return mapper.toDto(saved);
    }

    @Transactional
    public AcordoRegulatorioWorkflowResultDTO suspend(
            Integer id,
            AcordoRegulatorioWorkflowRequestDTO dto,
            ResourceVersionUpdatePrecondition<Integer> precondition
    ) {
        return transitionStatus(id, Set.of(AcordoStatus.VIGENTE), AcordoStatus.SUSPENSO, dto, precondition,
                "Acordo suspenso");
    }

    @Transactional
    public AcordoRegulatorioWorkflowResultDTO reinstate(
            Integer id,
            AcordoRegulatorioWorkflowRequestDTO dto,
            ResourceVersionUpdatePrecondition<Integer> precondition
    ) {
        return transitionStatus(id, Set.of(AcordoStatus.SUSPENSO), AcordoStatus.VIGENTE, dto, precondition,
                "Acordo reativado");
    }

    @Transactional
    public AcordoRegulatorioWorkflowResultDTO revoke(
            Integer id,
            AcordoRegulatorioWorkflowRequestDTO dto,
            ResourceVersionUpdatePrecondition<Integer> precondition
    ) {
        return transitionStatus(id, Set.of(AcordoStatus.VIGENTE, AcordoStatus.SUSPENSO),
                AcordoStatus.REVOGADO, dto, precondition, "Acordo revogado");
    }

    private AcordoRegulatorioWorkflowResultDTO transitionStatus(
            Integer id,
            Set<AcordoStatus> allowedStatuses,
            AcordoStatus targetStatus,
            AcordoRegulatorioWorkflowRequestDTO dto,
            ResourceVersionUpdatePrecondition<Integer> precondition,
            String message
    ) {
        AcordosRegulatorio agreement = findEntityById(id);
        long currentVersion = agreement.getVersion() == null ? 0L : agreement.getVersion();
        precondition.requireMatch(currentVersion);
        AcordoStatus currentStatus = agreement.getStatus();
        if (!allowedStatuses.contains(currentStatus)) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        agreement.setStatus(targetStatus);
        repository.saveAndFlush(agreement);
        return buildWorkflowResult(id, currentStatus, targetStatus, dto, message);
    }

    private AcordoRegulatorioWorkflowResultDTO buildWorkflowResult(
            Integer id,
            AcordoStatus previousStatus,
            AcordoStatus currentStatus,
            AcordoRegulatorioWorkflowRequestDTO dto,
            String message
    ) {
        AcordoRegulatorioWorkflowResultDTO result = new AcordoRegulatorioWorkflowResultDTO();
        result.setId(id);
        result.setStatusAnterior(previousStatus);
        result.setStatusAtual(currentStatus);
        result.setJustificativa(dto.getJustificativa());
        result.setMensagem(message);
        return result;
    }

    private AcordosRegulatorio refreshManaged(AcordosRegulatorio entity) {
        if (getEntityManager() == null) {
            return entity;
        }
        getEntityManager().flush();
        AcordosRegulatorio managed = getEntityManager().contains(entity) ? entity : getEntityManager().merge(entity);
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





