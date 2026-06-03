package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.IdentidadeSecretaDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateIdentidadeSecretaDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateIdentidadeSecretaDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.IdentidadeSecretaFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.IdentidadeSecreta;
import com.example.praxis.apiquickstart.hr.mapper.IdentidadeSecretaMapper;
import com.example.praxis.apiquickstart.hr.repository.IdentidadeSecretaRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
public class IdentidadeSecretaService extends AbstractQuickstartCrudService<IdentidadeSecreta, IdentidadeSecretaDTO, Integer, IdentidadeSecretaFilterDTO, CreateIdentidadeSecretaDTO, UpdateIdentidadeSecretaDTO> {

    private final IdentidadeSecretaMapper mapper;

    public IdentidadeSecretaService(IdentidadeSecretaRepository repository, IdentidadeSecretaMapper mapper) {
        super(repository, IdentidadeSecreta.class, mapper::toDto, mapper::toEntity, mapper::toEntity, IdentidadeSecreta::getId);
        this.mapper = mapper;
    }

    @Override
    public IdentidadeSecreta mergeUpdate(IdentidadeSecreta existing, IdentidadeSecreta fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }
}




