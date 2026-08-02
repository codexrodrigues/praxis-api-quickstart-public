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
import org.praxisplatform.uischema.concurrency.ResourceVersionEtagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.mapstruct.AfterMapping;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public abstract class EventosFolhaMapper implements ResourceMapper<
        EventosFolha,
        EventosFolhaResponseDTO,
        CreateEventosFolhaDTO,
        UpdateEventosFolhaDTO,
        Integer> {

    @Autowired
    protected ResourceVersionEtagService resourceVersionEtagService;

    @Override
    @Mappings({
            @Mapping(target = "folhaPagamentoId", source = "folhaPagamento.id"),
            @Mapping(target = "folhaPagamentoNome", source = "folhaPagamento.label"),
            @Mapping(target = "resourceVersion", ignore = true)
    })
    public abstract EventosFolhaResponseDTO toResponse(EventosFolha entity);

    @AfterMapping
    protected void attachResourceVersion(EventosFolha entity, @MappingTarget EventosFolhaResponseDTO dto) {
        long version = entity.getVersion() == null ? 0L : entity.getVersion();
        dto.setResourceVersion(resourceVersionEtagService.create("human-resources.eventos-folha", entity.getId(), version));
    }

    @Override
    @Mappings({
            @Mapping(target = "folhaPagamento", expression = "java(folhaFromId(dto.getFolhaPagamentoId()))"),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "id", ignore = true)
    })
    public abstract EventosFolha newEntity(CreateEventosFolhaDTO dto);

    @Override
    @Mappings({
            @Mapping(target = "folhaPagamento", expression = "java(folhaFromId(dto.getFolhaPagamentoId()))"),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "id", ignore = true)
    })
    public abstract void applyUpdate(@MappingTarget EventosFolha entity, UpdateEventosFolhaDTO dto);

    @Override
    public Integer extractId(EventosFolha entity) {
        return entity.getId();
    }

    protected FolhasPagamento folhaFromId(Integer id) {
        if (id == null) {
            return null;
        }
        FolhasPagamento folha = new FolhasPagamento();
        folha.setId(id);
        return folha;
    }
}
