package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.DependenteDTO;
import com.example.praxis.apiquickstart.hr.entity.Dependente;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface DependenteMapper {
    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto")
    })
    DependenteDTO toDto(Dependente entity);

    @Mappings({
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference")
    })
    Dependente toEntity(DependenteDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(Dependente source, @MappingTarget Dependente target);

}
