package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.HabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.HabilidadeFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Habilidade;
import com.example.praxis.apiquickstart.hr.mapper.HabilidadeMapper;
import com.example.praxis.apiquickstart.hr.repository.HabilidadeRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
public class HabilidadeService extends AbstractQuickstartCrudService<Habilidade, HabilidadeDTO, Integer, HabilidadeFilterDTO, CreateHabilidadeDTO, UpdateHabilidadeDTO> {

    private final HabilidadeMapper mapper;

    public HabilidadeService(HabilidadeRepository repository, HabilidadeMapper mapper) {
        super(repository, Habilidade.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Habilidade::getId);
        this.mapper = mapper;
    }

    @Override
    public Habilidade mergeUpdate(Habilidade existing, Habilidade fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }
}




