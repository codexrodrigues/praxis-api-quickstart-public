package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.HabilidadeDTO;
import com.example.praxis.apiquickstart.hr.entity.Habilidade;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface HabilidadeMapper {
    HabilidadeDTO toDto(Habilidade entity);
    Habilidade toEntity(HabilidadeDTO dto);
    void updateEntity(Habilidade source, @MappingTarget Habilidade target);
}

