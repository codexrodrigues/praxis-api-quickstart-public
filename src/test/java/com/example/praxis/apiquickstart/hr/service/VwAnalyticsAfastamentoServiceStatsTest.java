package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.mapper.VwAnalyticsAfastamentoMapper;
import com.example.praxis.apiquickstart.hr.repository.VwAnalyticsAfastamentoRepository;
import com.example.praxis.apiquickstart.hr.security.HrDepartmentScopeAccess;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class VwAnalyticsAfastamentoServiceStatsTest {

    @Test
    void publishesComparisonFieldsForDepartmentAbsenceAnalytics() {
        VwAnalyticsAfastamentoService service = new VwAnalyticsAfastamentoService(
                mock(VwAnalyticsAfastamentoRepository.class),
                mock(VwAnalyticsAfastamentoMapper.class),
                mock(HrDepartmentScopeAccess.class)
        );

        assertEquals(StatsSupportMode.AUTO, service.getComparisonStatsSupportMode());
        assertTrue(service.getStatsFieldRegistry().resolve("departamento").orElseThrow().groupByEligible());
        assertEquals("departamentoId", service.getStatsFieldRegistry().resolve("departamento").orElseThrow().keyPropertyPath());
        assertEquals("departamento", service.getStatsFieldRegistry().resolve("departamento").orElseThrow().labelPropertyPath());
        assertTrue(service.getStatsFieldRegistry().resolve("competencia").orElseThrow().timeSeriesEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("funcionarioId").orElseThrow().supports(StatsMetric.DISTINCT_COUNT));
        assertTrue(service.getStatsFieldRegistry().resolve("diasAfastado").orElseThrow().supports(StatsMetric.SUM));
        assertTrue(service.getStatsFieldRegistry().resolve("criticalityLevel").orElseThrow().groupByEligible());
    }
}
