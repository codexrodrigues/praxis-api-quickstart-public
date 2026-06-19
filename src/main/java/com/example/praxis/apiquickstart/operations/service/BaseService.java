package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.BaseDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateBaseDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateBaseOpsContextDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateBaseDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.BaseFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.Base;
import com.example.praxis.apiquickstart.operations.mapper.BaseMapper;
import com.example.praxis.apiquickstart.operations.repository.BaseRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.options.EntityLookupDescriptor;
import org.praxisplatform.uischema.options.LookupCapabilities;
import org.praxisplatform.uischema.options.LookupDetailDescriptor;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service de bases operacionais com foco em contexto tatico do ativo.
 *
 * <p>Ele evidencia a diferenca entre update completo do cadastro e ajuste parcial do contexto
 * operacional publicado pela surface {@code ops-context}.</p>
 */
@Service
public class BaseService extends AbstractQuickstartCrudService<Base, BaseDTO, Integer, BaseFilterDTO, CreateBaseDTO, UpdateBaseDTO> {
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Base.class, new OptionSourceDescriptor(
                    ApiPaths.Operations.BASES_BASE_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Operations.BASES,
                    null,
                    "id",
                    "nome",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Operations.BASES_BASE_LOOKUP_SOURCE,
                            null,
                            List.of("tipo", "planeta"),
                            null,
                            null,
                            null,
                            List.of("nome", "planeta"),
                            null,
                            null,
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Operations.BASES + "/{id}", "/operations/bases/{id}", "route")
                    )
            ))
            .build();

    private final BaseRepository repository;
    private final BaseMapper mapper;

    public BaseService(BaseRepository repository, BaseMapper mapper) {
        super(repository, Base.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Base::getId);
        this.repository = repository;
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
    public Base mergeUpdate(Base existing, Base fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Transactional
    public BaseDTO updateOpsContext(Integer id, UpdateBaseOpsContextDTO dto) {
        Base existing = repository.findById(id).orElseThrow(this::getNotFoundException);
        existing.setSigilo(dto.getSigilo());
        existing.setPlaneta(dto.getPlaneta());
        existing.setLatitude(dto.getLatitude());
        existing.setLongitude(dto.getLongitude());
        Base saved = refreshManaged(repository.save(existing));
        return mapper.toDto(saved);
    }

    /** Recarrega a entidade apos salvar para devolver o estado persistido mais recente. */
    private Base refreshManaged(Base entity) {
        if (getEntityManager() == null) {
            return entity;
        }
        getEntityManager().flush();
        Base managed = getEntityManager().contains(entity) ? entity : getEntityManager().merge(entity);
        getEntityManager().refresh(managed);
        return managed;
    }

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label");
    }
}







