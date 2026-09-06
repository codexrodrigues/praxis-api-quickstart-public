package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.FolhasPagamentoDTO;
import com.example.praxis.apiquickstart.hr.entity.FolhasPagamento;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface FolhasPagamentoMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id")
    })
    FolhasPagamentoDTO toDto(FolhasPagamento entity);

    @Mappings({
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference"),
            @Mapping(target = "eventosFolhas", ignore = true)
    })
    FolhasPagamento toEntity(FolhasPagamentoDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(FolhasPagamento source, @MappingTarget FolhasPagamento target);

}

