package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.IndenizacaoDTO;
import com.example.praxis.apiquickstart.hr.entity.Indenizacao;
import com.example.praxis.apiquickstart.operations.entity.Incidente;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface IndenizacaoMapper {

    @Mappings({
            @Mapping(target = "incidenteId", source = "incidente.id"),
            @Mapping(target = "incidenteTitulo", source = "incidente.descricao")
    })
    IndenizacaoDTO toDto(Indenizacao entity);

    @Mappings({
        @Mapping(target = "incidente", expression = "java(incidenteFromId(dto.getIncidenteId()))")
    })
    Indenizacao toEntity(IndenizacaoDTO dto);

    void updateEntity(Indenizacao source, @MappingTarget Indenizacao target);

    default Incidente incidenteFromId(Integer id) {
        if (id == null) return null;
        Incidente inc = new Incidente();
        inc.setId(id);
        return inc;
    }
}

