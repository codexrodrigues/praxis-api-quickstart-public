package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.mapper.VwPerfilHeroiMapper;
import com.example.praxis.apiquickstart.hr.repository.VwPerfilHeroiRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class VwPerfilHeroiServiceStatsTest {

    @Mock
    private VwPerfilHeroiRepository repository;

    @Mock
    private VwPerfilHeroiMapper mapper;

    @Test
    void shouldExposeGovernedStatsCapabilitiesForVwPerfilHeroi() {
        VwPerfilHeroiService service = new VwPerfilHeroiService(repository, mapper);

        assertEquals(StatsSupportMode.AUTO, service.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getDistributionStatsSupportMode());
        assertEquals(StatsSupportMode.DISABLED, service.getTimeSeriesStatsSupportMode());

        assertTrue(service.getStatsFieldRegistry().resolve("universo").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("universo").orElseThrow().distributionTermsEligible());
        assertFalse(service.getStatsFieldRegistry().resolve("universo").orElseThrow().timeSeriesEligible());

        assertTrue(service.getStatsFieldRegistry().resolve("scoreMedio").orElseThrow().distributionHistogramEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("scoreMedio").orElseThrow().metricFieldEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("scoreMedio").orElseThrow().supports(StatsMetric.SUM));

        assertEquals(OptionSourceType.DISTINCT_DIMENSION,
                service.getOptionSourceRegistry().resolve(com.example.praxis.apiquickstart.hr.entity.VwPerfilHeroi.class, "universo").orElseThrow().type());
        assertEquals(OptionSourceType.DISTINCT_DIMENSION,
                service.getOptionSourceRegistry().resolve(com.example.praxis.apiquickstart.hr.entity.VwPerfilHeroi.class, "basePrincipal").orElseThrow().type());
    }
}
