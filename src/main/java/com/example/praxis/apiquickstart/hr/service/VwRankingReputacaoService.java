package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.VwRankingReputacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.VwRankingReputacaoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.VwRankingReputacao;
import com.example.praxis.apiquickstart.hr.mapper.VwRankingReputacaoMapper;
import com.example.praxis.apiquickstart.hr.repository.VwRankingReputacaoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartReadOnlyService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class VwRankingReputacaoService extends AbstractQuickstartReadOnlyService<VwRankingReputacao, VwRankingReputacaoDTO, Integer, VwRankingReputacaoFilterDTO> {

    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("equipe", "equipe", Set.of(StatsMetric.COUNT))
            .numericHistogramMeasureField("scorePublico", "scorePublico")
            .numericHistogramMeasureField("scoreGovernamental", "scoreGovernamental")
            .numericHistogramMeasureField("media", "media")
            .numericHistogramMeasureField("posicao", "posicao")
            .build();

    private final VwRankingReputacaoMapper mapper;

    public VwRankingReputacaoService(VwRankingReputacaoRepository repository, VwRankingReputacaoMapper mapper) {
        super(repository, VwRankingReputacao.class, mapper::toDto, VwRankingReputacao::getFuncionarioId);
        this.mapper = mapper;
    }

    @Override
    public StatsSupportMode getGroupByStatsSupportMode() {
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


