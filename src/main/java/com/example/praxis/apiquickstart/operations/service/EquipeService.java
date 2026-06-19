package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.EquipeDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateEquipeDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateEquipeDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.EquipeFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.Equipe;
import com.example.praxis.apiquickstart.operations.mapper.EquipeMapper;
import com.example.praxis.apiquickstart.operations.repository.EquipeRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.options.EntityLookupDescriptor;
import org.praxisplatform.uischema.options.LookupCapabilities;
import org.praxisplatform.uischema.options.LookupDetailDescriptor;
import org.praxisplatform.uischema.options.LookupSelectionPolicy;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de referência para cadastro de equipes operacionais.
 *
 * <p>Ele demonstra a implementação mínima de um recurso organizacional no
 * quickstart, reaproveitando a infraestrutura genérica de CRUD enquanto deixa
 * o mapeamento explícito e didático. Para quem usa o projeto como guia, este
 * service mostra onde a lógica de atualização de equipes deve ser centralizada
 * antes de aparecer em controllers, workflows ou surfaces derivadas.</p>
 */
@Service
public class EquipeService extends AbstractQuickstartCrudService<Equipe, EquipeDTO, Integer, EquipeFilterDTO, CreateEquipeDTO, UpdateEquipeDTO> {
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Equipe.class, new OptionSourceDescriptor(
                    ApiPaths.Operations.EQUIPES_TEAM_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Operations.EQUIPES,
                    "equipeId",
                    "id",
                    "nome",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Operations.EQUIPES_TEAM_LOOKUP_SOURCE,
                            "sigla",
                            List.of(),
                            "status",
                            null,
                            null,
                            List.of("nome", "sigla"),
                            null,
                            new LookupSelectionPolicy(
                                    null,
                                    "status",
                                    List.of("ATIVA", "RESERVA"),
                                    List.of("DISSOLVIDA"),
                                    true,
                                    "Equipe dissolvida preservada apenas para reidratacao de valores existentes.",
                                    "Selecione uma equipe ativa ou em reserva."
                            ),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Operations.EQUIPES + "/{id}", "/operations/equipes/{id}", "route")
                    )
            ))
            .build();


    private final EquipeMapper mapper;

    public EquipeService(EquipeRepository repository, EquipeMapper mapper) {
        super(repository, Equipe.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Equipe::getId);
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
    public Equipe mergeUpdate(Equipe existing, Equipe fromPayload) {
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







