package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.LegacyPayCodeDTO;
import com.example.praxis.apiquickstart.hr.entity.LegacyPayCode;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface LegacyPayCodeMapper {
    LegacyPayCodeDTO toDto(LegacyPayCode entity);
    LegacyPayCode toEntity(LegacyPayCodeDTO dto);
    void updateEntity(LegacyPayCode source, @MappingTarget LegacyPayCode target);
}
