package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.FuncionarioDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFuncionarioProfileDTO;
import com.example.praxis.apiquickstart.hr.entity.Cargo;
import com.example.praxis.apiquickstart.hr.entity.Departamento;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;
import org.praxisplatform.uischema.concurrency.ResourceVersionEtagService;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public abstract class FuncionarioMapper {

    @Autowired
    protected ResourceVersionEtagService resourceVersionEtagService;

    @Mappings({
            @Mapping(target = "cargoId", source = "cargo.id"),
            @Mapping(target = "departamentoId", source = "departamento.id"),
            @Mapping(target = "avatarUrl", source = "fotoPerfilUrl"),
            @Mapping(target = "cargoNome", source = "cargo.nome"),
            @Mapping(target = "departamentoNome", source = "departamento.nome"),
            @Mapping(target = "resourceVersion", ignore = true)
    })
    public abstract FuncionarioDTO toDto(Funcionario entity);

    @AfterMapping
    protected void attachResourceVersion(Funcionario entity, @MappingTarget FuncionarioDTO dto) {
        long version = entity.getVersion() == null ? 0L : entity.getVersion();
        dto.setResourceVersion(resourceVersionEtagService.create("human-resources.funcionarios", entity.getId(), version));
    }

    @Mappings({
            @Mapping(target = "cargo", expression = "java(cargoFromId(dto.getCargoId()))"),
            @Mapping(target = "departamento", expression = "java(departamentoFromId(dto.getDepartamentoId()))"),
            @Mapping(target = "paisNascimento", ignore = true),
            @Mapping(target = "cidadeNascimento", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    public abstract Funcionario toEntity(FuncionarioDTO dto);

    public abstract void updateEntity(Funcionario source, @MappingTarget Funcionario target);

    @BeanMapping(ignoreByDefault = true)
    @Mappings({
            @Mapping(target = "nomeCompleto", source = "nomeCompleto"),
            @Mapping(target = "email", source = "email"),
            @Mapping(target = "telefone", source = "telefone"),
            @Mapping(target = "fotoPerfilUrl", source = "fotoPerfilUrl"),
            @Mapping(target = "estadoCivil", source = "estadoCivil")
    })
    public abstract void updateProfile(UpdateFuncionarioProfileDTO source, @MappingTarget Funcionario target);

    protected Cargo cargoFromId(Integer id) {
        if (id == null) return null;
        Cargo c = new Cargo();
        c.setId(id);
        return c;
    }

    protected Departamento departamentoFromId(Integer id) {
        if (id == null) return null;
        Departamento d = new Departamento();
        d.setId(id);
        return d;
    }
}
