package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.BaseDTO;
import com.example.praxis.apiquickstart.operations.entity.Base;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface BaseMapper {
    BaseDTO toDto(Base entity);
    Base toEntity(BaseDTO dto);
    void updateEntity(Base source, @MappingTarget Base target);
}



