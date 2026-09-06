package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.AcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateAcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.dto.ReviewAcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateAcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.entity.AcordosRegulatorio;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.praxisplatform.uischema.concurrency.ResourceVersionEtagService;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public abstract class AcordosRegulatorioMapper {
    private static final String RESOURCE_KEY = "operations.acordos-regulatorios";

    @Autowired
    protected ResourceVersionEtagService resourceVersionEtagService;

    @Mapping(target = "resourceVersion", ignore = true)
    public abstract AcordosRegulatorioDTO toDto(AcordosRegulatorio entity);

    @Mapping(target = "version", ignore = true)
    public abstract AcordosRegulatorio toEntity(AcordosRegulatorioDTO dto);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    public abstract AcordosRegulatorio toEntity(CreateAcordosRegulatorioDTO dto);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    public abstract AcordosRegulatorio toEntity(UpdateAcordosRegulatorioDTO dto);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    public abstract void updateEntity(AcordosRegulatorio source, @MappingTarget AcordosRegulatorio target);

    @AfterMapping
    protected void attachResourceVersion(AcordosRegulatorio entity, @MappingTarget AcordosRegulatorioDTO dto) {
        long version = entity.getVersion() == null ? 0L : entity.getVersion();
        dto.setResourceVersion(resourceVersionEtagService.create(RESOURCE_KEY, entity.getId(), version));
    }

    @BeanMapping(ignoreByDefault = true)
    @Mappings({
            @Mapping(target = "jurisdicao", source = "jurisdicao"),
            @Mapping(target = "descricao", source = "descricao")
    })
    public abstract void updateReview(ReviewAcordosRegulatorioDTO source, @MappingTarget AcordosRegulatorio target);
}

