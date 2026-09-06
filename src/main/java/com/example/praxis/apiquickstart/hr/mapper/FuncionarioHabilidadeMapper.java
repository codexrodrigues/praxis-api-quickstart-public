package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.FuncionarioHabilidadeDTO;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import com.example.praxis.apiquickstart.hr.entity.FuncionarioHabilidade;
import com.example.praxis.apiquickstart.hr.entity.Habilidade;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface FuncionarioHabilidadeMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "habilidadeId", source = "habilidade.id"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto"),
            @Mapping(target = "habilidadeNome", source = "habilidade.nome")
    })
    FuncionarioHabilidadeDTO toDto(FuncionarioHabilidade entity);

    @Mappings({
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference"),
            @Mapping(target = "habilidade", expression = "java(habilidadeFromId(dto.getHabilidadeId()))")
    })
    FuncionarioHabilidade toEntity(FuncionarioHabilidadeDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(FuncionarioHabilidade source, @MappingTarget FuncionarioHabilidade target);


    default Habilidade habilidadeFromId(Integer id) {
        if (id == null) return null;
        Habilidade h = new Habilidade();
        h.setId(id);
        return h;
    }
}
