package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.HabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.HabilidadeFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Habilidade;
import com.example.praxis.apiquickstart.hr.mapper.HabilidadeMapper;
import com.example.praxis.apiquickstart.hr.repository.HabilidadeRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HabilidadeService extends AbstractQuickstartCrudService<Habilidade, HabilidadeDTO, Integer, HabilidadeFilterDTO, CreateHabilidadeDTO, UpdateHabilidadeDTO> {
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Habilidade.class, new OptionSourceDescriptor(
                    ApiPaths.HumanResources.HABILIDADES_SKILL_LOOKUP_SOURCE,
                    OptionSourceType.LIGHT_LOOKUP,
                    ApiPaths.HumanResources.HABILIDADES,
                    null,
                    null,
                    "nome",
                    "id",
                    List.of(),
                    lookupPolicy()
            ))
            .build();

    private final HabilidadeMapper mapper;

    public HabilidadeService(HabilidadeRepository repository, HabilidadeMapper mapper) {
        super(repository, Habilidade.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Habilidade::getId);
        this.mapper = mapper;
    }

    public static OptionSourceRegistry optionSources() {
        return OPTION_SOURCES;
    }

    @Override
    public OptionSourceRegistry getOptionSourceRegistry() {
        return OPTION_SOURCES;
    }

    @Override
    public Habilidade mergeUpdate(Habilidade existing, Habilidade fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label");
    }
}




