package com.example.praxis.apiquickstart.operationalassets.mapper;

import com.example.praxis.apiquickstart.operationalassets.dto.VeiculoMissaoUsoDTO;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import com.example.praxis.apiquickstart.operationalassets.entity.Veiculo;
import com.example.praxis.apiquickstart.operationalassets.entity.VeiculoMissaoUso;
import com.example.praxis.apiquickstart.operations.entity.Missao;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface VeiculoMissaoUsoMapper {

    @Mappings({
            @Mapping(target = "veiculoId", source = "veiculo.id"),
            @Mapping(target = "missaoId", source = "missao.id"),
            @Mapping(target = "pilotoId", source = "piloto.id"),
            @Mapping(target = "veiculoNome", source = "veiculo.nome"),
            @Mapping(target = "missaoTitulo", source = "missao.titulo"),
            @Mapping(target = "pilotoNome", source = "piloto.nomeCompleto")
    })
    VeiculoMissaoUsoDTO toDto(VeiculoMissaoUso entity);

    @Mappings({
            @Mapping(target = "veiculo", expression = "java(veiculoFromId(dto.getVeiculoId()))"),
            @Mapping(target = "missao", expression = "java(missaoFromId(dto.getMissaoId()))"),
            @Mapping(target = "piloto", source = "pilotoId", qualifiedByName = "funcionarioReference")
    })
    VeiculoMissaoUso toEntity(VeiculoMissaoUsoDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(VeiculoMissaoUso source, @MappingTarget VeiculoMissaoUso target);

    default Veiculo veiculoFromId(Integer id) {
        if (id == null) return null;
        Veiculo v = new Veiculo();
        v.setId(id);
        return v;
    }

    default Missao missaoFromId(Integer id) {
        if (id == null) return null;
        Missao m = new Missao();
        m.setId(id);
        return m;
    }

}



