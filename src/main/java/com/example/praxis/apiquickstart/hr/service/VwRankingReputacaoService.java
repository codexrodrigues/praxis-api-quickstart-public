package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.VwRankingReputacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.VwRankingReputacaoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.VwRankingReputacao;
import com.example.praxis.apiquickstart.hr.mapper.VwRankingReputacaoMapper;
import com.example.praxis.apiquickstart.hr.repository.VwRankingReputacaoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartReadOnlyService;
import org.springframework.stereotype.Service;

@Service
public class VwRankingReputacaoService extends AbstractQuickstartReadOnlyService<VwRankingReputacao, VwRankingReputacaoDTO, Integer, VwRankingReputacaoFilterDTO> {

    private final VwRankingReputacaoMapper mapper;

    public VwRankingReputacaoService(VwRankingReputacaoRepository repository, VwRankingReputacaoMapper mapper) {
        super(repository, VwRankingReputacao.class, mapper::toDto, VwRankingReputacao::getFuncionarioId);
        this.mapper = mapper;
    }
}



