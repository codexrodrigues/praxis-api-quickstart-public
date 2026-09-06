package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.EnderecoDTO;
import com.example.praxis.apiquickstart.hr.entity.Endereco;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface EnderecoMapper {
    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto")
    })
    EnderecoDTO toDto(Endereco entity);

    @Mappings({
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference")
    })
    Endereco toEntity(EnderecoDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(Endereco source, @MappingTarget Endereco target);

}
