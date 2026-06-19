package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.CargoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateCargoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateCargoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.CargoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Cargo;
import com.example.praxis.apiquickstart.hr.mapper.CargoMapper;
import com.example.praxis.apiquickstart.hr.repository.CargoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CargoService extends AbstractQuickstartCrudService<Cargo, CargoDTO, Integer, CargoFilterDTO, CreateCargoDTO, UpdateCargoDTO> {
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Cargo.class, new OptionSourceDescriptor(
                    ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_SOURCE,
                    OptionSourceType.LIGHT_LOOKUP,
                    ApiPaths.HumanResources.CARGOS,
                    null,
                    null,
                    "nome",
                    "id",
                    List.of(),
                    lookupPolicy()
            ))
            .build();

    private final CargoMapper mapper;

    public CargoService(CargoRepository repository, CargoMapper mapper) {
        super(repository, Cargo.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Cargo::getId);
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
    public Cargo mergeUpdate(Cargo existing, Cargo fromPayload) {
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



