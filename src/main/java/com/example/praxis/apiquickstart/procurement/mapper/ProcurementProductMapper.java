package com.example.praxis.apiquickstart.procurement.mapper;

import com.example.praxis.apiquickstart.procurement.dto.ProcurementProductDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementProduct;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface ProcurementProductMapper {
    ProcurementProductDTO toDto(ProcurementProduct entity);
    ProcurementProduct toEntity(ProcurementProductDTO dto);
    void updateEntity(ProcurementProduct source, @MappingTarget ProcurementProduct target);
}
