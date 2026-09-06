package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.HistoricoSalarialDTO;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import com.example.praxis.apiquickstart.hr.entity.HistoricoSalarial;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface HistoricoSalarialMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id")
    })
    HistoricoSalarialDTO toDto(HistoricoSalarial entity);

    @Mappings({
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference")
    })
    HistoricoSalarial toEntity(HistoricoSalarialDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(HistoricoSalarial source, @MappingTarget HistoricoSalarial target);

}

