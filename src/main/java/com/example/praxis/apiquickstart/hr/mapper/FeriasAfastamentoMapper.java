package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.FeriasAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.entity.FeriasAfastamento;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface FeriasAfastamentoMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id")
    })
    FeriasAfastamentoDTO toDto(FeriasAfastamento entity);

    @Mappings({
            @Mapping(target = "funcionario", expression = "java(funcionarioFromId(dto.getFuncionarioId()))")
    })
    FeriasAfastamento toEntity(FeriasAfastamentoDTO dto);

    void updateEntity(FeriasAfastamento source, @MappingTarget FeriasAfastamento target);

    default Funcionario funcionarioFromId(Integer id) {
        if (id == null) return null;
        Funcionario f = new Funcionario();
        f.setId(id);
        return f;
    }
}

