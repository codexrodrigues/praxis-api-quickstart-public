package com.example.praxis.apiquickstart.operations.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Service de acordos regulatorios com foco em transicoes de compliance.
 *
 * <p>Ele demonstra como um recurso regulatorio pode publicar review parcial e workflow de item sem
 * misturar as duas semanticas: review altera metadados documentais; workflow altera o estado de
 * vigencia do acordo.</p>
 */
@Service
public class AcordosRegulatorioService extends AbstractQuickstartCrudService<AcordosRegulatorio, AcordosRegulatorioDTO, Integer, AcordosRegulatorioFilterDTO, CreateAcordosRegulatorioDTO, UpdateAcordosRegulatorioDTO> {

    private final AcordosRegulatorioRepository repository;
    private final AcordosRegulatorioMapper mapper;

    public AcordosRegulatorioService(AcordosRegulatorioRepository repository, AcordosRegulatorioMapper mapper) {
        super(repository, AcordosRegulatorio.class, mapper::toDto, mapper::toEntity, mapper::toEntity, AcordosRegulatorio::getId);
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AcordosRegulatorio mergeUpdate(AcordosRegulatorio existing, AcordosRegulatorio fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Transactional
    public AcordosRegulatorioDTO review(Integer id, ReviewAcordosRegulatorioDTO dto) {
        AcordosRegulatorio existing = findEntityById(id);
        mapper.updateReview(dto, existing);
        AcordosRegulatorio saved = refreshManaged(getRepository().save(existing));
        return mapper.toDto(saved);
    }

    @Transactional
    public AcordoRegulatorioWorkflowResultDTO suspend(Integer id, AcordoRegulatorioWorkflowRequestDTO dto) {
        return transitionStatus(id, AcordoStatus.VIGENTE, AcordoStatus.SUSPENSO, dto, "Acordo suspenso");
    }

    @Transactional
    public AcordoRegulatorioWorkflowResultDTO reinstate(Integer id, AcordoRegulatorioWorkflowRequestDTO dto) {
        return transitionStatus(id, AcordoStatus.SUSPENSO, AcordoStatus.VIGENTE, dto, "Acordo reativado");
    }

    @Transactional
    public AcordoRegulatorioWorkflowResultDTO revoke(Integer id, AcordoRegulatorioWorkflowRequestDTO dto) {
        AcordoStatus currentStatus = repository.findStatusById(id)
                .orElseThrow(this::getNotFoundException);
        if (currentStatus == AcordoStatus.REVOGADO) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        int updated = repository.transitionStatus(id, currentStatus, AcordoStatus.REVOGADO);
        if (updated == 0) {
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        return buildWorkflowResult(id, currentStatus, AcordoStatus.REVOGADO, dto, "Acordo revogado");
    }

    private AcordoRegulatorioWorkflowResultDTO transitionStatus(
            Integer id,
            AcordoStatus expectedStatus,
            AcordoStatus targetStatus,
            AcordoRegulatorioWorkflowRequestDTO dto,
            String message
    ) {
        int updated = repository.transitionStatus(id, expectedStatus, targetStatus);
        if (updated == 0) {
            AcordoStatus currentStatus = repository.findStatusById(id)
                    .orElseThrow(this::getNotFoundException);
            throw new ResponseStatusException(CONFLICT, "State not allowed: " + currentStatus.name());
        }
        return buildWorkflowResult(id, expectedStatus, targetStatus, dto, message);
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
}







