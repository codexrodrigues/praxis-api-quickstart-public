package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.VwResumoMissoeDTO;
import com.example.praxis.apiquickstart.operations.entity.VwResumoMissoe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VwResumoMissoeMapper {
    @Mapping(target = "missaoId", source = "missaoId")
    VwResumoMissoeDTO toDto(VwResumoMissoe entity);

    VwResumoMissoe toEntity(VwResumoMissoeDTO dto);

    void updateEntity(VwResumoMissoe source, @MappingTarget VwResumoMissoe target);
}


