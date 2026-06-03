package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.DepartamentoDTO;
import com.example.praxis.apiquickstart.hr.entity.Departamento;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface DepartamentoMapper {
    @Mappings({
            @Mapping(target = "responsavelId", source = "responsavel.id"),
            @Mapping(target = "responsavelNome", source = "responsavel.nomeCompleto")
    })
    DepartamentoDTO toDto(Departamento entity);

    @Mappings({
            @Mapping(target = "responsavel", expression = "java(responsavelFromId(dto.getResponsavelId()))")
    })
    Departamento toEntity(DepartamentoDTO dto);

    void updateEntity(Departamento source, @MappingTarget Departamento target);

    default com.example.praxis.apiquickstart.hr.entity.Funcionario responsavelFromId(Integer id) {
        if (id == null) return null;
        com.example.praxis.apiquickstart.hr.entity.Funcionario f = new com.example.praxis.apiquickstart.hr.entity.Funcionario();
        f.setId(id);
        return f;
    }
}
