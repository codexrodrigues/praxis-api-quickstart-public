package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.mapper.SinaisSocorroMapper;
import com.example.praxis.apiquickstart.operations.repository.SinaisSocorroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SinaisSocorroServiceStatsTest {

    @Mock
    private SinaisSocorroRepository repository;

    @Mock
    private SinaisSocorroMapper mapper;

    @Test
    void shouldExposeGovernedStatsCapabilitiesForEmergencyDashboardAuthoring() {
        SinaisSocorroService service = new SinaisSocorroService(repository, mapper);

        assertEquals(StatsSupportMode.AUTO, service.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getTimeSeriesStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getDistributionStatsSupportMode());

        assertTrue(service.getStatsFieldRegistry().resolve("origem").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("local").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("status").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("nivelAmeaca").orElseThrow().distributionHistogramEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("nivelAmeaca").orElseThrow().supports(StatsMetric.AVG));
        assertTrue(service.getStatsFieldRegistry().resolve("abertoEm").orElseThrow().timeSeriesEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("fechadoEm").orElseThrow().timeSeriesEligible());
    }
}
