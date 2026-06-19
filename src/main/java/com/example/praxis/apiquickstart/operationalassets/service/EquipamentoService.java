package com.example.praxis.apiquickstart.operationalassets.service;

import com.example.praxis.apiquickstart.operationalassets.dto.EquipamentoDTO;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateEquipamentoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateEquipamentoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.EquipamentoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.Equipamento;
import com.example.praxis.apiquickstart.operationalassets.mapper.EquipamentoMapper;
import com.example.praxis.apiquickstart.operationalassets.repository.EquipamentoRepository;
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

@Service
/**
 * Service de referência para cadastro de equipamentos.
 *
 * <p>Esta implementação mostra o caminho básico para persistir ativos no host
 * operacional de referência da Praxis: repositório, mapeamento explícito e merge
 * controlado de atualização. O valor didático aqui é mostrar que o recurso de
 * inventário reaproveita a infraestrutura comum do quickstart sem perder clareza
 * sobre onde regras de domínio adicionais deveriam evoluir.</p>
 */
public class EquipamentoService extends AbstractQuickstartCrudService<Equipamento, EquipamentoDTO, Integer, EquipamentoFilterDTO, CreateEquipamentoDTO, UpdateEquipamentoDTO> {
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Equipamento.class, new OptionSourceDescriptor(
                    ApiPaths.Assets.EQUIPAMENTOS_EQUIPMENT_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Assets.EQUIPAMENTOS,
                    "equipamentoId",
                    "id",
                    "nome",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Assets.EQUIPAMENTOS_EQUIPMENT_LOOKUP_SOURCE,
                            null,
                            List.of("tipo", "resistencia"),
                            "status",
                            null,
                            null,
                            List.of("nome"),
                            null,
                            new LookupSelectionPolicy(
                                    null,
                                    "status",
                                    List.of("ESTOQUE", "EM_USO"),
                                    List.of("MANUTENCAO", "QUEBRADO", "PERDIDO"),
                                    true,
                                    "Equipamento indisponivel preservado apenas para reidratacao de custodias existentes.",
                                    "Selecione um equipamento em estoque ou em uso operacional para nova alocacao."
                            ),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Assets.EQUIPAMENTOS + "/{id}", "/assets/equipamentos/{id}", "route")
                    )
            ))
            .build();

    private final EquipamentoMapper mapper;

    public EquipamentoService(EquipamentoRepository repository, EquipamentoMapper mapper) {
        super(repository, Equipamento.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Equipamento::getId);
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
    public Equipamento mergeUpdate(Equipamento existing, Equipamento fromPayload) {
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







