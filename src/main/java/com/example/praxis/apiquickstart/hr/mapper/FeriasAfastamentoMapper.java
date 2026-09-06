package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.FeriasAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.entity.FeriasAfastamento;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface FeriasAfastamentoMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id")
    })
    FeriasAfastamentoDTO toDto(FeriasAfastamento entity);

    @Mappings({
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference")
    })
    FeriasAfastamento toEntity(FeriasAfastamentoDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(FeriasAfastamento source, @MappingTarget FeriasAfastamento target);

}

