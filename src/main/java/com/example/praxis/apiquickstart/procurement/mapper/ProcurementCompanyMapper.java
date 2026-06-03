package com.example.praxis.apiquickstart.procurement.mapper;

import com.example.praxis.apiquickstart.procurement.dto.ProcurementCompanyDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementCompany;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface ProcurementCompanyMapper {
    ProcurementCompanyDTO toDto(ProcurementCompany entity);
    ProcurementCompany toEntity(ProcurementCompanyDTO dto);
    void updateEntity(ProcurementCompany source, @MappingTarget ProcurementCompany target);
}
