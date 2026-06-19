package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.IncidenteDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateIncidenteDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateIncidenteDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.IncidenteFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.Incidente;
import com.example.praxis.apiquickstart.operations.mapper.IncidenteMapper;
import com.example.praxis.apiquickstart.operations.repository.IncidenteRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.options.EntityLookupDescriptor;
import org.praxisplatform.uischema.options.LookupCapabilities;
import org.praxisplatform.uischema.options.LookupDetailDescriptor;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service basico de incidentes usado para representar o lado transacional do risco operacional.
 *
 * <p>Assim como outros services simples do quickstart, ele reforca que um recurso pode participar
 * plenamente do contrato metadata-driven mesmo quando sua regra de negocio cabe no fluxo CRUD
 * canonico.</p>
 */
@Service
public class IncidenteService extends AbstractQuickstartCrudService<Incidente, IncidenteDTO, Integer, IncidenteFilterDTO, CreateIncidenteDTO, UpdateIncidenteDTO> {

    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Incidente.class, new OptionSourceDescriptor(
                    ApiPaths.Operations.INCIDENTES_INCIDENT_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Operations.INCIDENTES,
                    null,
                    "id",
                    "descricao",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Operations.INCIDENTES_INCIDENT_LOOKUP_SOURCE,
                            null,
                            List.of("severidade", "local", "ocorridoEm"),
                            null,
                            null,
                            null,
                            List.of("descricao", "local"),
                            null,
                            null,
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Operations.INCIDENTES + "/{id}", "/operations/incidentes/{id}", "route")
                    )
            ))
            .build();

    private final IncidenteMapper mapper;

    public IncidenteService(IncidenteRepository repository, IncidenteMapper mapper) {
        super(repository, Incidente.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Incidente::getId);
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
    public Incidente mergeUpdate(Incidente existing, Incidente fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(
                true,
                true,
                "contains",
                0,
                25,
                100,
                true,
                false,
                "label"
        );
    }
}







