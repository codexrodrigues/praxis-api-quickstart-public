package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.DepartamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateDepartamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateDepartamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.DepartamentoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Departamento;
import com.example.praxis.apiquickstart.hr.mapper.DepartamentoMapper;
import com.example.praxis.apiquickstart.hr.repository.DepartamentoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
public class DepartamentoService extends AbstractQuickstartCrudService<Departamento, DepartamentoDTO, Integer, DepartamentoFilterDTO, CreateDepartamentoDTO, UpdateDepartamentoDTO> {

    private final DepartamentoMapper mapper;

    public DepartamentoService(DepartamentoRepository repository, DepartamentoMapper mapper) {
        super(repository, Departamento.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Departamento::getId);
        this.mapper = mapper;
    }

    @Override
    public Departamento mergeUpdate(Departamento existing, Departamento fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Override
    public java.util.Optional<String> getDatasetVersion() {
        long count = getRepository().count();
        return java.util.Optional.of(getEntityClass().getSimpleName() + ":" + count);
    }
}



