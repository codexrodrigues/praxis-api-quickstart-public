package com.example.praxis.apiquickstart.procurement.mapper;

import com.example.praxis.apiquickstart.procurement.dto.ProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementContract;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface ProcurementContractMapper {
    ProcurementContractDTO toDto(ProcurementContract entity);
    ProcurementContract toEntity(ProcurementContractDTO dto);
    void updateEntity(ProcurementContract source, @MappingTarget ProcurementContract target);
}
