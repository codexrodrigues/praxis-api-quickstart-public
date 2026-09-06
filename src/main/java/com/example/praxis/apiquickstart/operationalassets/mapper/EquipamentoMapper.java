package com.example.praxis.apiquickstart.operationalassets.mapper;

import com.example.praxis.apiquickstart.operationalassets.dto.EquipamentoDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.Equipamento;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface EquipamentoMapper {

    @Mappings({
            @Mapping(target = "proprietarioId", source = "proprietario.id"),
            @Mapping(target = "proprietarioNome", source = "proprietario.nomeCompleto")
    })
    EquipamentoDTO toDto(Equipamento entity);

    @Mappings({
            @Mapping(target = "proprietario", source = "proprietarioId", qualifiedByName = "funcionarioReference")
    })
    Equipamento toEntity(EquipamentoDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(Equipamento source, @MappingTarget Equipamento target);

}



