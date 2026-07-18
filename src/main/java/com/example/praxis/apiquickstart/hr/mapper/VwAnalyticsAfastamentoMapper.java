package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.VwAnalyticsAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.entity.VwAnalyticsAfastamento;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface VwAnalyticsAfastamentoMapper {
    VwAnalyticsAfastamentoDTO toDto(VwAnalyticsAfastamento entity);

    VwAnalyticsAfastamento toEntity(VwAnalyticsAfastamentoDTO dto);

    void updateEntity(VwAnalyticsAfastamento source, @MappingTarget VwAnalyticsAfastamento target);
}
