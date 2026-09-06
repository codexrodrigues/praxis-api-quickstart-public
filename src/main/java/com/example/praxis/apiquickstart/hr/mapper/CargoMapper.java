package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.CargoDTO;
import com.example.praxis.apiquickstart.hr.entity.Cargo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface CargoMapper {
    CargoDTO toDto(Cargo entity);
    Cargo toEntity(CargoDTO dto);
    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(Cargo source, @MappingTarget Cargo target);
}

