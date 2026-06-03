package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.ReputacaoDTO;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import com.example.praxis.apiquickstart.hr.entity.Reputacao;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface ReputacaoMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto")
    })
    ReputacaoDTO toDto(Reputacao entity);

    @Mappings({
            @Mapping(target = "funcionario", expression = "java(funcionarioFromId(dto.getFuncionarioId()))")
    })
    Reputacao toEntity(ReputacaoDTO dto);

    void updateEntity(Reputacao source, @MappingTarget Reputacao target);

    default Funcionario funcionarioFromId(Integer id) {
        if (id == null) return null;
        Funcionario f = new Funcionario();
        f.setId(id);
        return f;
    }
}
