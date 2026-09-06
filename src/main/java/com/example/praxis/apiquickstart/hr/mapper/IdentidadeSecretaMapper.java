package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.IdentidadeSecretaDTO;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import com.example.praxis.apiquickstart.hr.entity.IdentidadeSecreta;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface IdentidadeSecretaMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto")
    })
    IdentidadeSecretaDTO toDto(IdentidadeSecreta entity);

    @Mappings({
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference")
    })
    IdentidadeSecreta toEntity(IdentidadeSecretaDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(IdentidadeSecreta source, @MappingTarget IdentidadeSecreta target);

}
