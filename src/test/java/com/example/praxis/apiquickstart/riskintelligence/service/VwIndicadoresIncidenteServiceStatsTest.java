package com.example.praxis.apiquickstart.riskintelligence.service;

import com.example.praxis.apiquickstart.riskintelligence.mapper.VwIndicadoresIncidenteMapper;
import com.example.praxis.apiquickstart.riskintelligence.repository.VwIndicadoresIncidenteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class VwIndicadoresIncidenteServiceStatsTest {

    @Mock
    private VwIndicadoresIncidenteRepository repository;

    @Mock
    private VwIndicadoresIncidenteMapper mapper;

    @Test
    void shouldExposeGovernedStatsCapabilitiesForVwIndicadoresIncidente() {
        VwIndicadoresIncidenteService service = new VwIndicadoresIncidenteService(repository, mapper);

        assertEquals(StatsSupportMode.AUTO, service.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getTimeSeriesStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getDistributionStatsSupportMode());

        assertTrue(service.getStatsFieldRegistry().resolve("severidade").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("severidade").orElseThrow().distributionTermsEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("ocorridoEm").orElseThrow().timeSeriesEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("totalPago").orElseThrow().distributionHistogramEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("totalPago").orElseThrow().metricFieldEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("totalPago").orElseThrow().supports(StatsMetric.SUM));

        assertEquals(OptionSourceType.DISTINCT_DIMENSION,
                service.getOptionSourceRegistry().resolve(com.example.praxis.apiquickstart.riskintelligence.entity.VwIndicadoresIncidente.class, "severidade").orElseThrow().type());
    }
}

