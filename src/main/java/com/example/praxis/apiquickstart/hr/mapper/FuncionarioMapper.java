package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.FuncionarioDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFuncionarioProfileDTO;
import com.example.praxis.apiquickstart.hr.entity.Cargo;
import com.example.praxis.apiquickstart.hr.entity.Departamento;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface FuncionarioMapper {

    @Mappings({
            @Mapping(target = "cargoId", source = "cargo.id"),
            @Mapping(target = "departamentoId", source = "departamento.id"),
            @Mapping(target = "avatarUrl", source = "fotoPerfilUrl"),
            @Mapping(target = "cargoNome", source = "cargo.nome"),
            @Mapping(target = "departamentoNome", source = "departamento.nome")
    })
    FuncionarioDTO toDto(Funcionario entity);

    @Mappings({
            @Mapping(target = "cargo", expression = "java(cargoFromId(dto.getCargoId()))"),
            @Mapping(target = "departamento", expression = "java(departamentoFromId(dto.getDepartamentoId()))"),
            @Mapping(target = "paisNascimento", ignore = true),
            @Mapping(target = "cidadeNascimento", ignore = true)
    })
    Funcionario toEntity(FuncionarioDTO dto);

    void updateEntity(Funcionario source, @MappingTarget Funcionario target);

    @BeanMapping(ignoreByDefault = true)
    @Mappings({
            @Mapping(target = "nomeCompleto", source = "nomeCompleto"),
            @Mapping(target = "email", source = "email"),
            @Mapping(target = "telefone", source = "telefone"),
            @Mapping(target = "fotoPerfilUrl", source = "fotoPerfilUrl"),
            @Mapping(target = "estadoCivil", source = "estadoCivil")
    })
    void updateProfile(UpdateFuncionarioProfileDTO source, @MappingTarget Funcionario target);

    default Cargo cargoFromId(Integer id) {
        if (id == null) return null;
        Cargo c = new Cargo();
        c.setId(id);
        return c;
    }

    default Departamento departamentoFromId(Integer id) {
        if (id == null) return null;
        Departamento d = new Departamento();
        d.setId(id);
        return d;
    }
}
