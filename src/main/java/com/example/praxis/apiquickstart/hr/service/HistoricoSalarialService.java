package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.HistoricoSalarialDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateHistoricoSalarialDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateHistoricoSalarialDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.HistoricoSalarialFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.HistoricoSalarial;
import com.example.praxis.apiquickstart.hr.mapper.HistoricoSalarialMapper;
import com.example.praxis.apiquickstart.hr.repository.HistoricoSalarialRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
public class HistoricoSalarialService extends AbstractQuickstartCrudService<HistoricoSalarial, HistoricoSalarialDTO, Integer, HistoricoSalarialFilterDTO, CreateHistoricoSalarialDTO, UpdateHistoricoSalarialDTO> {

    private final HistoricoSalarialMapper mapper;

    public HistoricoSalarialService(HistoricoSalarialRepository repository, HistoricoSalarialMapper mapper) {
        super(repository, HistoricoSalarial.class, mapper::toDto, mapper::toEntity, mapper::toEntity, HistoricoSalarial::getId);
        this.mapper = mapper;
    }

    @Override
    public HistoricoSalarial mergeUpdate(HistoricoSalarial existing, HistoricoSalarial fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }
}




