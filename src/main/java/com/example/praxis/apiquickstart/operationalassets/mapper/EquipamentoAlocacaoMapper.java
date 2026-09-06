package com.example.praxis.apiquickstart.operationalassets.mapper;

import com.example.praxis.apiquickstart.operationalassets.dto.EquipamentoAlocacaoDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.Equipamento;
import com.example.praxis.apiquickstart.operationalassets.entity.EquipamentoAlocacao;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface EquipamentoAlocacaoMapper {

    @Mappings({
            @Mapping(target = "equipamentoId", source = "equipamento.id"),
            @Mapping(target = "funcionarioId", source = "funcionario.id")
    })
    EquipamentoAlocacaoDTO toDto(EquipamentoAlocacao entity);

    @Mappings({
            @Mapping(target = "equipamento", expression = "java(equipamentoFromId(dto.getEquipamentoId()))"),
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference")
    })
    EquipamentoAlocacao toEntity(EquipamentoAlocacaoDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(EquipamentoAlocacao source, @MappingTarget EquipamentoAlocacao target);

    default Equipamento equipamentoFromId(Integer id) {
        if (id == null) return null;
        Equipamento e = new Equipamento();
        e.setId(id);
        return e;
    }

}




