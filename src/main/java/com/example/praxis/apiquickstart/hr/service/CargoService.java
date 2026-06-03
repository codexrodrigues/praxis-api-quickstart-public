package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.CargoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateCargoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateCargoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.CargoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Cargo;
import com.example.praxis.apiquickstart.hr.mapper.CargoMapper;
import com.example.praxis.apiquickstart.hr.repository.CargoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
public class CargoService extends AbstractQuickstartCrudService<Cargo, CargoDTO, Integer, CargoFilterDTO, CreateCargoDTO, UpdateCargoDTO> {

    private final CargoMapper mapper;

    public CargoService(CargoRepository repository, CargoMapper mapper) {
        super(repository, Cargo.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Cargo::getId);
        this.mapper = mapper;
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
}



