package com.example.praxis.apiquickstart.operationalassets.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operationalassets.dto.VeiculoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateVeiculoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateVeiculoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.VeiculoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.Veiculo;
import com.example.praxis.apiquickstart.operationalassets.mapper.VeiculoMapper;
import com.example.praxis.apiquickstart.operationalassets.repository.VeiculoRepository;
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
 * Service basico de veiculos usado para mostrar o caminho mais simples de um ativo CRUD.
 *
 * <p>Neste caso, o valor pedagogico esta justamente na simplicidade: quando nao ha workflow ou
 * surface especial, o quickstart ainda deixa explicito o merge controlado do agregado e o encaixe
 * no pipeline canonico da plataforma.</p>
 */
@Service
public class VeiculoService extends AbstractQuickstartCrudService<Veiculo, VeiculoDTO, Integer, VeiculoFilterDTO, CreateVeiculoDTO, UpdateVeiculoDTO> {
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Veiculo.class, new OptionSourceDescriptor(
                    ApiPaths.Assets.VEICULOS_VEHICLE_LOOKUP_SOURCE,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.Assets.VEICULOS,
                    "veiculoId",
                    "id",
                    "nome",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            ApiPaths.Assets.VEICULOS_VEHICLE_LOOKUP_SOURCE,
                            null,
                            List.of("tipo", "capacidade"),
                            "status",
                            null,
                            null,
                            List.of("nome"),
                            null,
                            new LookupSelectionPolicy(
                                    null,
                                    "status",
                                    List.of("OPERACIONAL"),
                                    List.of("MANUTENCAO", "INOPERANTE"),
                                    true,
                                    "Veiculo indisponivel preservado apenas para reidratacao de valores existentes.",
                                    "Selecione um veiculo operacional para nova sortie."
                            ),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(ApiPaths.Assets.VEICULOS + "/{id}", "/assets/veiculos/{id}", "route")
                    )
            ))
            .build();

    private final VeiculoMapper mapper;

    public VeiculoService(VeiculoRepository repository, VeiculoMapper mapper) {
        super(repository, Veiculo.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Veiculo::getId);
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
    public Veiculo mergeUpdate(Veiculo existing, Veiculo fromPayload) {
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







