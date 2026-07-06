package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.FuncionarioHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateFuncionarioHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFuncionarioHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.FuncionarioHabilidadeFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.FuncionarioHabilidade;
import com.example.praxis.apiquickstart.hr.mapper.FuncionarioHabilidadeMapper;
import com.example.praxis.apiquickstart.hr.repository.FuncionarioHabilidadeRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FuncionarioHabilidadeService extends AbstractQuickstartCrudService<FuncionarioHabilidade, FuncionarioHabilidadeDTO, Integer, FuncionarioHabilidadeFilterDTO, CreateFuncionarioHabilidadeDTO, UpdateFuncionarioHabilidadeDTO> {

    private final FuncionarioHabilidadeMapper mapper;
    private final FuncionarioHabilidadeRepository repository;

    public FuncionarioHabilidadeService(FuncionarioHabilidadeRepository repository, FuncionarioHabilidadeMapper mapper) {
        super(repository, FuncionarioHabilidade.class, mapper::toDto, mapper::toEntity, mapper::toEntity, FuncionarioHabilidade::getId);
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public FuncionarioHabilidade mergeUpdate(FuncionarioHabilidade existing, FuncionarioHabilidade fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    public List<FuncionarioHabilidadeDTO> findByFuncionarioIdForEmployeeSurface(Integer funcionarioId) {
        return repository.findByFuncionarioIdOrderByProficienciaDescHabilidadeNomeAsc(funcionarioId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}


