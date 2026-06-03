package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.FuncionarioHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import com.example.praxis.apiquickstart.hr.entity.FuncionarioHabilidade;
import com.example.praxis.apiquickstart.hr.entity.Habilidade;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface FuncionarioHabilidadeMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "habilidadeId", source = "habilidade.id"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto"),
            @Mapping(target = "habilidadeNome", source = "habilidade.nome")
    })
    FuncionarioHabilidadeDTO toDto(FuncionarioHabilidade entity);

    @Mappings({
            @Mapping(target = "funcionario", expression = "java(funcionarioFromId(dto.getFuncionarioId()))"),
            @Mapping(target = "habilidade", expression = "java(habilidadeFromId(dto.getHabilidadeId()))")
    })
    FuncionarioHabilidade toEntity(FuncionarioHabilidadeDTO dto);

    void updateEntity(FuncionarioHabilidade source, @MappingTarget FuncionarioHabilidade target);

    default Funcionario funcionarioFromId(Integer id) {
        if (id == null) return null;
        Funcionario f = new Funcionario();
        f.setId(id);
        return f;
    }

    default Habilidade habilidadeFromId(Integer id) {
        if (id == null) return null;
        Habilidade h = new Habilidade();
        h.setId(id);
        return h;
    }
}
