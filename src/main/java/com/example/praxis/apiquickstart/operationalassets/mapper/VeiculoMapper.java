package com.example.praxis.apiquickstart.operationalassets.mapper;

import com.example.praxis.apiquickstart.operationalassets.dto.VeiculoDTO;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import com.example.praxis.apiquickstart.operationalassets.entity.Veiculo;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface VeiculoMapper {

    @Mappings({
            @Mapping(target = "proprietarioId", source = "proprietario.id"),
            @Mapping(target = "proprietarioNome", source = "proprietario.nomeCompleto")
    })
    VeiculoDTO toDto(Veiculo entity);

    @Mappings({
            @Mapping(target = "proprietario", source = "proprietarioId", qualifiedByName = "funcionarioReference")
    })
    Veiculo toEntity(VeiculoDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(Veiculo source, @MappingTarget Veiculo target);

}



