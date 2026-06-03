package com.example.praxis.apiquickstart.riskintelligence.mapper;

import com.example.praxis.apiquickstart.riskintelligence.dto.VwIndicadoresIncidenteDTO;
import com.example.praxis.apiquickstart.riskintelligence.entity.VwIndicadoresIncidente;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface VwIndicadoresIncidenteMapper {
    VwIndicadoresIncidenteDTO toDto(VwIndicadoresIncidente entity);
    VwIndicadoresIncidente toEntity(VwIndicadoresIncidenteDTO dto);
    void updateEntity(VwIndicadoresIncidente source, @MappingTarget VwIndicadoresIncidente target);
}

