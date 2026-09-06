package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.EquipeMembroDTO;
import com.example.praxis.apiquickstart.operations.entity.Equipe;
import com.example.praxis.apiquickstart.operations.entity.EquipeMembro;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface EquipeMembroMapper {

    @Mappings({
        @Mapping(target = "equipeId", source = "equipe.id"),
        @Mapping(target = "funcionarioId", source = "funcionario.id"),
        @Mapping(target = "equipeNome", source = "equipe.nome"),
        @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto")
    })
    EquipeMembroDTO toDto(EquipeMembro entity);

    @Mappings({
        @Mapping(target = "equipe", expression = "java(equipeFromId(dto.getEquipeId()))"),
        @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference")
    })
    EquipeMembro toEntity(EquipeMembroDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(EquipeMembro source, @MappingTarget EquipeMembro target);

    default Equipe equipeFromId(Integer id) {
        if (id == null) return null;
        Equipe e = new Equipe();
        e.setId(id);
        return e;
    }

}


