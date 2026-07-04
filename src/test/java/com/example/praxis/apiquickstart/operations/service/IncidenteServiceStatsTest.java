package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.mapper.IncidenteMapper;
import com.example.praxis.apiquickstart.operations.repository.IncidenteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class IncidenteServiceStatsTest {

    @Mock
    private IncidenteRepository repository;

    @Mock
    private IncidenteMapper mapper;

    @Test
    void shouldExposeGovernedStatsCapabilitiesForIncidentInvestigation() {
        IncidenteService service = new IncidenteService(repository, mapper);

        assertEquals(StatsSupportMode.AUTO, service.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getTimeSeriesStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getDistributionStatsSupportMode());

        assertTrue(service.getStatsFieldRegistry().resolve("severidade").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("local").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("missaoId").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("ocorridoEm").orElseThrow().timeSeriesEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("danosCivis").orElseThrow().distributionHistogramEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("danosCivis").orElseThrow().supports(StatsMetric.SUM));
        assertTrue(service.getStatsFieldRegistry().resolve("feridos").orElseThrow().metricFieldEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("mortos").orElseThrow().metricFieldEligible());
    }
}
