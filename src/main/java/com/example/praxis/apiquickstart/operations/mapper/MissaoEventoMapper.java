package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.MissaoEventoDTO;
import com.example.praxis.apiquickstart.operations.entity.Missao;
import com.example.praxis.apiquickstart.operations.entity.MissaoEvento;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface MissaoEventoMapper {

    @Mappings({
            @Mapping(target = "missaoId", source = "missao.id"),
            @Mapping(target = "missaoTitulo", source = "missao.titulo")
    })
    MissaoEventoDTO toDto(MissaoEvento entity);

    @Mappings({
            @Mapping(target = "missao", expression = "java(missaoFromId(dto.getMissaoId()))")
    })
    MissaoEvento toEntity(MissaoEventoDTO dto);

    void updateEntity(MissaoEvento source, @MappingTarget MissaoEvento target);

    default Missao missaoFromId(Integer id) {
        if (id == null) return null;
        Missao m = new Missao();
        m.setId(id);
        return m;
    }
}


