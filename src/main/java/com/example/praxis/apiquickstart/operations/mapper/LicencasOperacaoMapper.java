package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.LicencasOperacaoDTO;
import com.example.praxis.apiquickstart.operations.entity.AcordosRegulatorio;
import com.example.praxis.apiquickstart.operations.entity.Equipe;
import com.example.praxis.apiquickstart.operations.entity.LicencasOperacao;
import com.example.praxis.apiquickstart.core.mapper.ManagedEntityReferenceResolver;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class, uses = ManagedEntityReferenceResolver.class)
public interface LicencasOperacaoMapper {

    @Mappings({
            @Mapping(target = "acordoId", source = "acordo.id"),
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "equipeId", source = "equipe.id"),
            @Mapping(target = "acordoNome", source = "acordo.nome"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto"),
            @Mapping(target = "equipeNome", source = "equipe.nome")
    })
    LicencasOperacaoDTO toDto(LicencasOperacao entity);

    @Mappings({
            @Mapping(target = "acordo", expression = "java(acordoFromId(dto.getAcordoId()))"),
            @Mapping(target = "funcionario", source = "funcionarioId", qualifiedByName = "funcionarioReference"),
            @Mapping(target = "equipe", expression = "java(equipeFromId(dto.getEquipeId()))")
    })
    LicencasOperacao toEntity(LicencasOperacaoDTO dto);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(LicencasOperacao source, @MappingTarget LicencasOperacao target);

    default AcordosRegulatorio acordoFromId(Integer id) {
        if (id == null) return null;
        AcordosRegulatorio a = new AcordosRegulatorio();
        a.setId(id);
        return a;
    }


    default Equipe equipeFromId(Integer id) {
        if (id == null) return null;
        Equipe e = new Equipe();
        e.setId(id);
        return e;
    }
}


