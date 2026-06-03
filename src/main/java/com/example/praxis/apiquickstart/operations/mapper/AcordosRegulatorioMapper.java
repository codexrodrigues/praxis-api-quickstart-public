package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.AcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.dto.ReviewAcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.entity.AcordosRegulatorio;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface AcordosRegulatorioMapper {
    AcordosRegulatorioDTO toDto(AcordosRegulatorio entity);
    AcordosRegulatorio toEntity(AcordosRegulatorioDTO dto);
    void updateEntity(AcordosRegulatorio source, @MappingTarget AcordosRegulatorio target);

    @BeanMapping(ignoreByDefault = true)
    @Mappings({
            @Mapping(target = "jurisdicao", source = "jurisdicao"),
            @Mapping(target = "descricao", source = "descricao")
    })
    void updateReview(ReviewAcordosRegulatorioDTO source, @MappingTarget AcordosRegulatorio target);
}



