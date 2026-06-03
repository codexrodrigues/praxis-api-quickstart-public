package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.dto.IncidenteDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateIncidenteDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateIncidenteDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.IncidenteFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.Incidente;
import com.example.praxis.apiquickstart.operations.mapper.IncidenteMapper;
import com.example.praxis.apiquickstart.operations.repository.IncidenteRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

/**
 * Service basico de incidentes usado para representar o lado transacional do risco operacional.
 *
 * <p>Assim como outros services simples do quickstart, ele reforca que um recurso pode participar
 * plenamente do contrato metadata-driven mesmo quando sua regra de negocio cabe no fluxo CRUD
 * canonico.</p>
 */
@Service
public class IncidenteService extends AbstractQuickstartCrudService<Incidente, IncidenteDTO, Integer, IncidenteFilterDTO, CreateIncidenteDTO, UpdateIncidenteDTO> {

    private final IncidenteMapper mapper;

    public IncidenteService(IncidenteRepository repository, IncidenteMapper mapper) {
        super(repository, Incidente.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Incidente::getId);
        this.mapper = mapper;
    }

    @Override
    public Incidente mergeUpdate(Incidente existing, Incidente fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }
}







