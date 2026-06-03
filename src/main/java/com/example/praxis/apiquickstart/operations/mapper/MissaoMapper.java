package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.MissaoDTO;
import com.example.praxis.apiquickstart.riskintelligence.entity.Ameaca;
import com.example.praxis.apiquickstart.operations.entity.Missao;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface MissaoMapper {

    @Mappings({
            @Mapping(target = "ameacaId", source = "ameaca.id"),
            @Mapping(target = "ameacaNome", source = "ameaca.nome")
    })
    MissaoDTO toDto(Missao entity);

    @Mappings({
            @Mapping(target = "ameaca", expression = "java(ameacaFromId(dto.getAmeacaId()))")
    })
    Missao toEntity(MissaoDTO dto);

    void updateEntity(Missao source, @MappingTarget Missao target);

    default Ameaca ameacaFromId(Integer id) {
        if (id == null) return null;
        Ameaca a = new Ameaca();
        a.setId(id);
        return a;
    }
}



