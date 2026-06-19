package com.example.praxis.apiquickstart.riskintelligence.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.riskintelligence.dto.AmeacaDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.CreateAmeacaDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.UpdateAmeacaDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.filter.AmeacaFilterDTO;
import com.example.praxis.apiquickstart.riskintelligence.entity.Ameaca;
import com.example.praxis.apiquickstart.riskintelligence.mapper.AmeacaMapper;
import com.example.praxis.apiquickstart.riskintelligence.repository.AmeacaRepository;
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
 * Service de ameacas usado como exemplo simples de cadastro transacional no dominio de risco.
 *
 * <p>O quickstart mantem este service enxuto para reforcar que a camada de risco tambem pode usar
 * o pipeline canonico da plataforma sem complexidade extra quando nao ha workflow ou surface
 * especializada.</p>
 */
@Service
public class AmeacaService extends AbstractQuickstartCrudService<Ameaca, AmeacaDTO, Integer, AmeacaFilterDTO, CreateAmeacaDTO, UpdateAmeacaDTO> {
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Ameaca.class, new OptionSourceDescriptor(
                    ApiPaths.RiskIntelligence.AMEACAS_THREAT_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.RiskIntelligence.AMEACAS,
                    "ameacaId",
                    "id",
                    "nome",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.RiskIntelligence.AMEACAS_THREAT_LOOKUP_SOURCE,
                            null,
                            List.of("classe", "planeta", "nivel"),
                            "status",
                            null,
                            null,
                            List.of("nome", "classe", "planeta"),
                            null,
                            new LookupSelectionPolicy(
                                    null,
                                    "status",
                                    List.of("LIVRE", "EM_OBSERVACAO", "CONFRONTO"),
                                    List.of("CAPTURADO", "ELIMINADO"),
                                    true,
                                    "Ameaca encerrada preservada apenas para reidratacao de valores existentes.",
                                    "Selecione uma ameaca em ciclo operacional."
                            ),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.RiskIntelligence.AMEACAS + "/{id}", "/risk-intelligence/ameacas/{id}", "route")
                    )
            ))
            .build();

    private final AmeacaMapper mapper;

    public AmeacaService(AmeacaRepository repository, AmeacaMapper mapper) {
        super(repository, Ameaca.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Ameaca::getId);
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
    public Ameaca mergeUpdate(Ameaca existing, Ameaca fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label");
    }
}





