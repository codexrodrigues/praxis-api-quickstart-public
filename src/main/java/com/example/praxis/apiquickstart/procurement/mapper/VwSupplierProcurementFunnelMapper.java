package com.example.praxis.apiquickstart.procurement.mapper;

import com.example.praxis.apiquickstart.procurement.dto.VwSupplierProcurementFunnelDTO;
import com.example.praxis.apiquickstart.procurement.entity.VwSupplierProcurementFunnel;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface VwSupplierProcurementFunnelMapper {
    VwSupplierProcurementFunnelDTO toDto(VwSupplierProcurementFunnel entity);
    VwSupplierProcurementFunnel toEntity(VwSupplierProcurementFunnelDTO dto);
    void updateEntity(VwSupplierProcurementFunnel source, @MappingTarget VwSupplierProcurementFunnel target);
}
