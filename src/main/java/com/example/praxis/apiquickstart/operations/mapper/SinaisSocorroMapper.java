package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.SinaisSocorroDTO;
import com.example.praxis.apiquickstart.operations.entity.SinaisSocorro;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface SinaisSocorroMapper {
    SinaisSocorroDTO toDto(SinaisSocorro entity);
    SinaisSocorro toEntity(SinaisSocorroDTO dto);
    void updateEntity(SinaisSocorro source, @MappingTarget SinaisSocorro target);
}



