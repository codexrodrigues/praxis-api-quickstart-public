package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.ReputacaoDTO;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import com.example.praxis.apiquickstart.hr.entity.Reputacao;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface ReputacaoMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto")
    })
    ReputacaoDTO toDto(Reputacao entity);

    @Mappings({
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference")
    })
    Reputacao toEntity(ReputacaoDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(Reputacao source, @MappingTarget Reputacao target);

}
