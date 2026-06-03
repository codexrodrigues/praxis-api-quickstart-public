package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.BaseAcessoDTO;
import com.example.praxis.apiquickstart.operations.entity.Base;
import com.example.praxis.apiquickstart.operations.entity.BaseAcesso;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface BaseAcessoMapper {

    @Mappings({
            @Mapping(target = "baseId", source = "base.id"),
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "baseNome", source = "base.nome"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto")
    })
    BaseAcessoDTO toDto(BaseAcesso entity);

    @Mappings({
            @Mapping(target = "base", expression = "java(baseFromId(dto.getBaseId()))"),
            @Mapping(target = "funcionario", expression = "java(funcionarioFromId(dto.getFuncionarioId()))")
    })
    BaseAcesso toEntity(BaseAcessoDTO dto);

    void updateEntity(BaseAcesso source, @MappingTarget BaseAcesso target);

    default Base baseFromId(Integer id) {
        if (id == null) return null;
        Base b = new Base();
        b.setId(id);
        return b;
    }

    default Funcionario funcionarioFromId(Integer id) {
        if (id == null) return null;
        Funcionario f = new Funcionario();
        f.setId(id);
        return f;
    }
}


