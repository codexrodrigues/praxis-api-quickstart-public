package com.example.praxis.apiquickstart.procurement.mapper;

import com.example.praxis.apiquickstart.procurement.dto.ProcurementPurchaseOrderDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementPurchaseOrder;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface ProcurementPurchaseOrderMapper {
    ProcurementPurchaseOrderDTO toDto(ProcurementPurchaseOrder entity);
    ProcurementPurchaseOrder toEntity(ProcurementPurchaseOrderDTO dto);
    void updateEntity(ProcurementPurchaseOrder source, @MappingTarget ProcurementPurchaseOrder target);
}
