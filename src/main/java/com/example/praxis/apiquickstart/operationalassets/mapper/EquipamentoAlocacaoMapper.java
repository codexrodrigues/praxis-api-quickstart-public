package com.example.praxis.apiquickstart.operationalassets.mapper;

import com.example.praxis.apiquickstart.operationalassets.dto.EquipamentoAlocacaoDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.Equipamento;
import com.example.praxis.apiquickstart.operationalassets.entity.EquipamentoAlocacao;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface EquipamentoAlocacaoMapper {

    @Mappings({
            @Mapping(target = "equipamentoId", source = "equipamento.id"),
            @Mapping(target = "funcionarioId", source = "funcionario.id")
    })
    EquipamentoAlocacaoDTO toDto(EquipamentoAlocacao entity);

    @Mappings({
            @Mapping(target = "equipamento", expression = "java(equipamentoFromId(dto.getEquipamentoId()))"),
            @Mapping(target = "funcionario", expression = "java(funcionarioFromId(dto.getFuncionarioId()))")
    })
    EquipamentoAlocacao toEntity(EquipamentoAlocacaoDTO dto);

    void updateEntity(EquipamentoAlocacao source, @MappingTarget EquipamentoAlocacao target);

    default Equipamento equipamentoFromId(Integer id) {
        if (id == null) return null;
        Equipamento e = new Equipamento();
        e.setId(id);
        return e;
    }

    default Funcionario funcionarioFromId(Integer id) {
        if (id == null) return null;
        Funcionario f = new Funcionario();
        f.setId(id);
        return f;
    }
}




