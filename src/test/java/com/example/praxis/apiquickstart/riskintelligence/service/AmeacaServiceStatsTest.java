package com.example.praxis.apiquickstart.riskintelligence.service;

import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.riskintelligence.entity.Ameaca;
import com.example.praxis.apiquickstart.riskintelligence.mapper.AmeacaMapper;
import com.example.praxis.apiquickstart.riskintelligence.repository.AmeacaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AmeacaServiceStatsTest {

    @Mock
    private AmeacaRepository repository;

    @Mock
    private AmeacaMapper mapper;

    @Mock
    private DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    @Test
    void shouldExposeGovernedStatsCapabilitiesForThreatMonitoring() {
        AmeacaService service = new AmeacaService(repository, mapper, workflowActionPolicyResolver);

        assertEquals(StatsSupportMode.AUTO, service.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getDistributionStatsSupportMode());

        assertTrue(service.getStatsFieldRegistry().resolve("classe").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("status").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("planeta").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("nivel").orElseThrow().distributionHistogramEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("nivel").orElseThrow().supports(StatsMetric.AVG));
        assertTrue(service.getStatsFieldRegistry().resolve("recompensa").orElseThrow().metricFieldEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("recompensa").orElseThrow().supports(StatsMetric.SUM));
    }

    @Test
    void shouldExposeThreatDetailThroughTheCanonicalItemSurface() {
        AmeacaService service = new AmeacaService(repository, mapper, workflowActionPolicyResolver);

        var descriptor = service.getOptionSourceRegistry()
                .resolve(Ameaca.class, ApiPaths.RiskIntelligence.AMEACAS_THREAT_LOOKUP_SOURCE)
                .orElseThrow();
        var detail = descriptor.entityLookup().detail();

        assertEquals("surface", detail.kind());
        assertEquals("detail", detail.surfaceId());
        assertEquals("drawer", detail.presentation());
        assertEquals("praxis-dynamic-form", detail.preferredWidget());
        assertEquals("view", detail.mode());
        assertNull(detail.hrefTemplate());
        assertNull(detail.routeTemplate());
        assertNull(detail.openDetailMode());
    }
}
