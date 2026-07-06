package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.DependenteDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateDependenteDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateDependenteDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.DependenteFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Dependente;
import com.example.praxis.apiquickstart.hr.mapper.DependenteMapper;
import com.example.praxis.apiquickstart.hr.repository.DependenteRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
public class DependenteService extends AbstractQuickstartCrudService<Dependente, DependenteDTO, Integer, DependenteFilterDTO, CreateDependenteDTO, UpdateDependenteDTO> {

    private final DependenteMapper mapper;
    private final DependenteRepository repository;

    public DependenteService(DependenteRepository repository, DependenteMapper mapper) {
        super(repository, Dependente.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Dependente::getId);
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Dependente mergeUpdate(Dependente existing, Dependente fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    public java.util.List<DependenteDTO> findByFuncionarioIdForEmployeeSurface(Integer funcionarioId) {
        return repository.findByFuncionarioIdOrderByDataNascimentoAsc(funcionarioId)
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


