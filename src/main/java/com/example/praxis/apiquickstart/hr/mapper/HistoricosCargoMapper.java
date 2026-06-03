package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.HistoricosCargoDTO;
import com.example.praxis.apiquickstart.hr.entity.Cargo;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import com.example.praxis.apiquickstart.hr.entity.HistoricosCargo;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface HistoricosCargoMapper {

    @Mappings({
            @Mapping(target = "funcionarioId", source = "funcionario.id"),
            @Mapping(target = "cargoId", source = "cargo.id"),
            @Mapping(target = "funcionarioNome", source = "funcionario.nomeCompleto"),
            @Mapping(target = "cargoNome", source = "cargo.nome")
    })
    HistoricosCargoDTO toDto(HistoricosCargo entity);

    @Mappings({
            @Mapping(target = "funcionario", expression = "java(funcionarioFromId(dto.getFuncionarioId()))"),
            @Mapping(target = "cargo", expression = "java(cargoFromId(dto.getCargoId()))")
    })
    HistoricosCargo toEntity(HistoricosCargoDTO dto);

    void updateEntity(HistoricosCargo source, @MappingTarget HistoricosCargo target);

    default Funcionario funcionarioFromId(Integer id) {
        if (id == null) return null;
        Funcionario f = new Funcionario();
        f.setId(id);
        return f;
    }

    default Cargo cargoFromId(Integer id) {
        if (id == null) return null;
        Cargo c = new Cargo();
        c.setId(id);
        return c;
    }
}
