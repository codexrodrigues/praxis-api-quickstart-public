package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.MissaoParticipanteDTO;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import com.example.praxis.apiquickstart.operations.entity.Missao;
import com.example.praxis.apiquickstart.operations.entity.MissaoParticipante;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface MissaoParticipanteMapper {

    @Mappings({
            @Mapping(target = "missaoId", source = "missao.id"),
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "missaoTitulo", source = "missao.titulo"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto"),
            @Mapping(target = "funcionarioFotoUrl", source = "funcionario.fotoPerfilUrl")
    })
    MissaoParticipanteDTO toDto(MissaoParticipante entity);

    @Mappings({
            @Mapping(target = "missao", expression = "java(missaoFromId(dto.getMissaoId()))"),
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference")
    })
    MissaoParticipante toEntity(MissaoParticipanteDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(MissaoParticipante source, @MappingTarget MissaoParticipante target);

    default Missao missaoFromId(Integer id) {
        if (id == null) return null;
        Missao m = new Missao();
        m.setId(id);
        return m;
    }

}


