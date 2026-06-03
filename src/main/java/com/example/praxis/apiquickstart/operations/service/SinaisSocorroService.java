package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.dto.SinaisSocorroDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateSinaisSocorroDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateSinaisSocorroDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.SinaisSocorroFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.SinaisSocorro;
import com.example.praxis.apiquickstart.operations.mapper.SinaisSocorroMapper;
import com.example.praxis.apiquickstart.operations.repository.SinaisSocorroRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

@Service
/**
 * Service de referência para alertas de socorro e triagem operacional.
 *
 * <p>No projeto de exemplo, esta classe mostra um caso em que um CRUD simples
 * ganha um pequeno comportamento de coesão operacional ao publicar uma versão
 * de dataset baseada em cardinalidade. Isso ajuda a explicar como recursos do
 * host podem sinalizar mudanças relevantes para consumers sem redefinir os
 * contratos canônicos dos starters da plataforma.</p>
 */
public class SinaisSocorroService extends AbstractQuickstartCrudService<SinaisSocorro, SinaisSocorroDTO, Integer, SinaisSocorroFilterDTO, CreateSinaisSocorroDTO, UpdateSinaisSocorroDTO> {

    private final SinaisSocorroMapper mapper;

    public SinaisSocorroService(SinaisSocorroRepository repository, SinaisSocorroMapper mapper) {
        super(repository, SinaisSocorro.class, mapper::toDto, mapper::toEntity, mapper::toEntity, SinaisSocorro::getId);
        this.mapper = mapper;
    }

    @Override
    public SinaisSocorro mergeUpdate(SinaisSocorro existing, SinaisSocorro fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Override
    public java.util.Optional<String> getDatasetVersion() {
        long count = getRepository().count();
        return java.util.Optional.of(getEntityClass().getSimpleName() + ":" + count);
    }
}






