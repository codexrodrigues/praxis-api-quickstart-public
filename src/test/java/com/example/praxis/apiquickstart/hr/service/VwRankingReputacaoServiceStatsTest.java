package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.mapper.VwRankingReputacaoMapper;
import com.example.praxis.apiquickstart.hr.repository.VwRankingReputacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class VwRankingReputacaoServiceStatsTest {

    @Mock
    private VwRankingReputacaoRepository repository;

    @Mock
    private VwRankingReputacaoMapper mapper;

    @Test
    void shouldExposeGovernedStatsCapabilitiesForReputationRanking() {
        VwRankingReputacaoService service = new VwRankingReputacaoService(repository, mapper);

        assertEquals(StatsSupportMode.AUTO, service.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getDistributionStatsSupportMode());
        assertEquals(StatsSupportMode.DISABLED, service.getTimeSeriesStatsSupportMode());

        assertTrue(service.getStatsFieldRegistry().resolve("equipe").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("scorePublico").orElseThrow().distributionHistogramEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("scoreGovernamental").orElseThrow().distributionHistogramEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("media").orElseThrow().supports(StatsMetric.AVG));
        assertTrue(service.getStatsFieldRegistry().resolve("posicao").orElseThrow().distributionHistogramEligible());
    }
}
