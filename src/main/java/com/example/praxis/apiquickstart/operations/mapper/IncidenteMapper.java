package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.IncidenteDTO;
import com.example.praxis.apiquickstart.operations.entity.Incidente;
import com.example.praxis.apiquickstart.operations.entity.Missao;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface IncidenteMapper {

    @Mappings({
            @Mapping(target = "missaoId", source = "missao.id")
    })
    IncidenteDTO toDto(Incidente entity);

    @Mappings({
            @Mapping(target = "missao", expression = "java(missaoFromId(dto.getMissaoId()))")
    })
    Incidente toEntity(IncidenteDTO dto);

    void updateEntity(Incidente source, @MappingTarget Incidente target);

    default Missao missaoFromId(Integer id) {
        if (id == null) return null;
        Missao m = new Missao();
        m.setId(id);
        return m;
    }
}



