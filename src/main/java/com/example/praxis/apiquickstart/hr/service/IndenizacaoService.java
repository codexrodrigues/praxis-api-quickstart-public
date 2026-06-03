package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.IndenizacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateIndenizacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateIndenizacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.IndenizacaoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Indenizacao;
import com.example.praxis.apiquickstart.hr.mapper.IndenizacaoMapper;
import com.example.praxis.apiquickstart.hr.repository.IndenizacaoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
public class IndenizacaoService extends AbstractQuickstartCrudService<Indenizacao, IndenizacaoDTO, Integer, IndenizacaoFilterDTO, CreateIndenizacaoDTO, UpdateIndenizacaoDTO> {

    private final IndenizacaoMapper mapper;

    public IndenizacaoService(IndenizacaoRepository repository, IndenizacaoMapper mapper) {
        super(repository, Indenizacao.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Indenizacao::getId);
        this.mapper = mapper;
    }

    @Override
    public Indenizacao mergeUpdate(Indenizacao existing, Indenizacao fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }
}




