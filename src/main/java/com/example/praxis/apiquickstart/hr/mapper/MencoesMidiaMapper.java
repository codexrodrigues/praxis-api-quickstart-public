package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.MencoesMidiaDTO;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import com.example.praxis.apiquickstart.hr.entity.MencoesMidia;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface MencoesMidiaMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto")
    })
    MencoesMidiaDTO toDto(MencoesMidia entity);

    @Mappings({
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference")
    })
    MencoesMidia toEntity(MencoesMidiaDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(MencoesMidia source, @MappingTarget MencoesMidia target);

}
