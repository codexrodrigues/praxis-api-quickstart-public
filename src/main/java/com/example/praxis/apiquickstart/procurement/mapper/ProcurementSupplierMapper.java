package com.example.praxis.apiquickstart.procurement.mapper;

import com.example.praxis.apiquickstart.procurement.dto.ProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementSupplier;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface ProcurementSupplierMapper {
    ProcurementSupplierDTO toDto(ProcurementSupplier entity);
    ProcurementSupplier toEntity(ProcurementSupplierDTO dto);
    void updateEntity(ProcurementSupplier source, @MappingTarget ProcurementSupplier target);
}
