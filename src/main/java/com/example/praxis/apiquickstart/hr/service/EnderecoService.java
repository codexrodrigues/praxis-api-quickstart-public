package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.EnderecoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateEnderecoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateEnderecoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.EnderecoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Endereco;
import com.example.praxis.apiquickstart.hr.mapper.EnderecoMapper;
import com.example.praxis.apiquickstart.hr.repository.EnderecoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
public class EnderecoService extends AbstractQuickstartCrudService<Endereco, EnderecoDTO, Integer, EnderecoFilterDTO, CreateEnderecoDTO, UpdateEnderecoDTO> {

    private final EnderecoMapper mapper;
    private final EnderecoRepository repository;

    public EnderecoService(EnderecoRepository repository, EnderecoMapper mapper) {
        super(repository, Endereco.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Endereco::getId);
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Endereco mergeUpdate(Endereco existing, Endereco fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    public java.util.List<EnderecoDTO> findByFuncionarioIdForEmployeeSurface(Integer funcionarioId) {
        return repository.findByFuncionarioIdOrderByCidadeAscLogradouroAsc(funcionarioId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public java.util.Optional<String> getDatasetVersion() {
        long count = getRepository().count();
        return java.util.Optional.of(getEntityClass().getSimpleName() + ":" + count);
    }
}


