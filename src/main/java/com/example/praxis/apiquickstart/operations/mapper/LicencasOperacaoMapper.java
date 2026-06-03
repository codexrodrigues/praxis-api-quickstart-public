package com.example.praxis.apiquickstart.operations.mapper;

import com.example.praxis.apiquickstart.operations.dto.LicencasOperacaoDTO;
import com.example.praxis.apiquickstart.operations.entity.AcordosRegulatorio;
import com.example.praxis.apiquickstart.operations.entity.Equipe;
import com.example.praxis.apiquickstart.operations.entity.LicencasOperacao;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
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
            @Mapping(target = "funcionario", expression = "java(funcionarioFromId(dto.getFuncionarioId()))"),
            @Mapping(target = "equipe", expression = "java(equipeFromId(dto.getEquipeId()))")
    })
    LicencasOperacao toEntity(LicencasOperacaoDTO dto);

    void updateEntity(LicencasOperacao source, @MappingTarget LicencasOperacao target);

    default AcordosRegulatorio acordoFromId(Integer id) {
        if (id == null) return null;
        AcordosRegulatorio a = new AcordosRegulatorio();
        a.setId(id);
        return a;
    }

    default Funcionario funcionarioFromId(Integer id) {
        if (id == null) return null;
        Funcionario f = new Funcionario();
        f.setId(id);
        return f;
    }

    default Equipe equipeFromId(Integer id) {
        if (id == null) return null;
        Equipe e = new Equipe();
        e.setId(id);
        return e;
    }
}


