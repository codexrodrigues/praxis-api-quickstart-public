package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.MencoesMidiaDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateMencoesMidiaDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateMencoesMidiaDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.MencoesMidiaFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.MencoesMidia;
import com.example.praxis.apiquickstart.hr.mapper.MencoesMidiaMapper;
import com.example.praxis.apiquickstart.hr.repository.MencoesMidiaRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
public class MencoesMidiaService extends AbstractQuickstartCrudService<MencoesMidia, MencoesMidiaDTO, Integer, MencoesMidiaFilterDTO, CreateMencoesMidiaDTO, UpdateMencoesMidiaDTO> {

    private final MencoesMidiaMapper mapper;

    public MencoesMidiaService(MencoesMidiaRepository repository, MencoesMidiaMapper mapper) {
        super(repository, MencoesMidia.class, mapper::toDto, mapper::toEntity, mapper::toEntity, MencoesMidia::getId);
        this.mapper = mapper;
    }

    @Override
    public MencoesMidia mergeUpdate(MencoesMidia existing, MencoesMidia fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }
}




