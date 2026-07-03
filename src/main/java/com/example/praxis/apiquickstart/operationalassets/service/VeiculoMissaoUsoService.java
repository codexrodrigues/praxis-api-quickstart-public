package com.example.praxis.apiquickstart.operationalassets.service;

import com.example.praxis.apiquickstart.operationalassets.dto.VeiculoMissaoUsoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateVeiculoMissaoUsoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateVeiculoMissaoUsoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.VeiculoMissaoUsoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.VeiculoMissaoUso;
import com.example.praxis.apiquickstart.operationalassets.mapper.VeiculoMissaoUsoMapper;
import com.example.praxis.apiquickstart.operationalassets.repository.VeiculoMissaoUsoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service de vinculacao entre missoes e veiculos.
 *
 * <p>Assim como o controller correspondente, este service existe para deixar legivel o caso em que
 * a plataforma publica um recurso relacional explicito em vez de esconder a associacao em um
 * payload aninhado ou endpoint ad hoc.</p>
 */
@Service
public class VeiculoMissaoUsoService extends AbstractQuickstartCrudService<VeiculoMissaoUso, VeiculoMissaoUsoDTO, Integer, VeiculoMissaoUsoFilterDTO, CreateVeiculoMissaoUsoDTO, UpdateVeiculoMissaoUsoDTO> {
    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("veiculoId", "veiculo.id", Set.of(StatsMetric.COUNT))
            .groupByBucket("missaoId", "missao.id", Set.of(StatsMetric.COUNT))
            .groupByBucket("pilotoId", "piloto.id", Set.of(StatsMetric.COUNT))
            .temporalTimeSeriesField("partida", "partida")
            .temporalTimeSeriesField("chegada", "chegada")
            .build();

    private final VeiculoMissaoUsoMapper mapper;

    public VeiculoMissaoUsoService(VeiculoMissaoUsoRepository repository, VeiculoMissaoUsoMapper mapper) {
        super(repository, VeiculoMissaoUso.class, mapper::toDto, mapper::toEntity, mapper::toEntity, VeiculoMissaoUso::getId);
        this.mapper = mapper;
    }

    @Override
    public VeiculoMissaoUso mergeUpdate(VeiculoMissaoUso existing, VeiculoMissaoUso fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Override
    public StatsSupportMode getGroupByStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsSupportMode getTimeSeriesStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsSupportMode getDistributionStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsFieldRegistry getStatsFieldRegistry() {
        return STATS_FIELDS;
    }
}






