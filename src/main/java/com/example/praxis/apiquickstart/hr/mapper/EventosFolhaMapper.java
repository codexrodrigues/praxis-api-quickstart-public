package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.CreateEventosFolhaDTO;
import com.example.praxis.apiquickstart.hr.dto.EventosFolhaResponseDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateEventosFolhaDTO;
import com.example.praxis.apiquickstart.hr.entity.EventosFolha;
import com.example.praxis.apiquickstart.hr.entity.FolhasPagamento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface EventosFolhaMapper extends ResourceMapper<
        EventosFolha,
        EventosFolhaResponseDTO,
        CreateEventosFolhaDTO,
        UpdateEventosFolhaDTO,
        Integer> {

    @Override
    @Mappings({
            @Mapping(target = "folhaPagamentoId", source = "folhaPagamento.id"),
            @Mapping(target = "folhaPagamentoNome", source = "folhaPagamento.label")
    })
    EventosFolhaResponseDTO toResponse(EventosFolha entity);

    @Override
    @Mappings({
            @Mapping(target = "folhaPagamento", expression = "java(folhaFromId(dto.getFolhaPagamentoId()))"),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "id", ignore = true)
    })
    EventosFolha newEntity(CreateEventosFolhaDTO dto);

    @Override
    @Mappings({
            @Mapping(target = "folhaPagamento", expression = "java(folhaFromId(dto.getFolhaPagamentoId()))"),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "id", ignore = true)
    })
    void applyUpdate(@MappingTarget EventosFolha entity, UpdateEventosFolhaDTO dto);

    @Override
    default Integer extractId(EventosFolha entity) {
        return entity.getId();
    }

    default FolhasPagamento folhaFromId(Integer id) {
        if (id == null) {
            return null;
        }
        FolhasPagamento folha = new FolhasPagamento();
        folha.setId(id);
        return folha;
    }
}
