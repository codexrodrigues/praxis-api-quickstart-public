package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.HistoricosCargoDTO;
import com.example.praxis.apiquickstart.hr.entity.Cargo;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import com.example.praxis.apiquickstart.hr.entity.HistoricosCargo;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface HistoricosCargoMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "cargoId", source = "cargo.id"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto"),
            @Mapping(target = "cargoNome", source = "cargo.nome")
    })
    HistoricosCargoDTO toDto(HistoricosCargo entity);

    @Mappings({
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference"),
            @Mapping(target = "cargo", expression = "java(cargoFromId(dto.getCargoId()))")
    })
    HistoricosCargo toEntity(HistoricosCargoDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(HistoricosCargo source, @MappingTarget HistoricosCargo target);


    default Cargo cargoFromId(Integer id) {
        if (id == null) return null;
        Cargo c = new Cargo();
        c.setId(id);
        return c;
    }
}
