package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.DepartamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateDepartamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateDepartamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.DepartamentoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Departamento;
import com.example.praxis.apiquickstart.hr.mapper.DepartamentoMapper;
import com.example.praxis.apiquickstart.hr.repository.DepartamentoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartamentoService extends AbstractQuickstartCrudService<Departamento, DepartamentoDTO, Integer, DepartamentoFilterDTO, CreateDepartamentoDTO, UpdateDepartamentoDTO> {
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Departamento.class, new OptionSourceDescriptor(
                    ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_SOURCE,
                    OptionSourceType.LIGHT_LOOKUP,
                    ApiPaths.HumanResources.DEPARTAMENTOS,
                    null,
                    null,
                    "nome",
                    "id",
                    List.of(),
                    lookupPolicy()
            ))
            .build();

    private final DepartamentoMapper mapper;

    public DepartamentoService(DepartamentoRepository repository, DepartamentoMapper mapper) {
        super(repository, Departamento.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Departamento::getId);
        this.mapper = mapper;
    }

    public static OptionSourceRegistry optionSources() {
        return OPTION_SOURCES;
    }

    @Override
    public OptionSourceRegistry getOptionSourceRegistry() {
        return OPTION_SOURCES;
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

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label");
    }
}



