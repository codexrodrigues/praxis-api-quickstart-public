package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.FolhasPagamentoDTO;
import com.example.praxis.apiquickstart.hr.entity.FolhasPagamento;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface FolhasPagamentoMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id")
    })
    FolhasPagamentoDTO toDto(FolhasPagamento entity);

    @Mappings({
            @Mapping(target = "funcionario", expression = "java(funcionarioFromId(dto.getFuncionarioId()))"),
            @Mapping(target = "eventosFolhas", ignore = true)
    })
    FolhasPagamento toEntity(FolhasPagamentoDTO dto);

    void updateEntity(FolhasPagamento source, @MappingTarget FolhasPagamento target);

    default Funcionario funcionarioFromId(Integer id) {
        if (id == null) return null;
        Funcionario f = new Funcionario();
        f.setId(id);
        return f;
    }
}

