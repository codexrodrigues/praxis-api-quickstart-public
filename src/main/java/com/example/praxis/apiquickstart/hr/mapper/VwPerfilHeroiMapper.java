package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.VwPerfilHeroiDTO;
import com.example.praxis.apiquickstart.hr.entity.VwPerfilHeroi;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface VwPerfilHeroiMapper {
    VwPerfilHeroiDTO toDto(VwPerfilHeroi entity);
    VwPerfilHeroi toEntity(VwPerfilHeroiDTO dto);
    void updateEntity(VwPerfilHeroi source, @MappingTarget VwPerfilHeroi target);
}

