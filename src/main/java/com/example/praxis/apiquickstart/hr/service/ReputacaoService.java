package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.ReputacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateReputacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateReputacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.ReputacaoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Reputacao;
import com.example.praxis.apiquickstart.hr.mapper.ReputacaoMapper;
import com.example.praxis.apiquickstart.hr.repository.ReputacaoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
public class ReputacaoService extends AbstractQuickstartCrudService<Reputacao, ReputacaoDTO, Integer, ReputacaoFilterDTO, CreateReputacaoDTO, UpdateReputacaoDTO> {

    private final ReputacaoMapper mapper;

    public ReputacaoService(ReputacaoRepository repository, ReputacaoMapper mapper) {
        super(repository, Reputacao.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Reputacao::getId);
        this.mapper = mapper;
    }

    @Override
    public Reputacao mergeUpdate(Reputacao existing, Reputacao fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }
}




