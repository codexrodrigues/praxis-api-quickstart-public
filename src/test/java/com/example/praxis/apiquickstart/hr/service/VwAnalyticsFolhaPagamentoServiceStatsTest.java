package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.mapper.VwAnalyticsFolhaPagamentoMapper;
import com.example.praxis.apiquickstart.hr.repository.VwAnalyticsFolhaPagamentoRepository;
import com.example.praxis.apiquickstart.hr.security.HrDepartmentScopeAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class VwAnalyticsFolhaPagamentoServiceStatsTest {

    @Mock
    private VwAnalyticsFolhaPagamentoRepository repository;

    @Mock
    private VwAnalyticsFolhaPagamentoMapper mapper;

    @Mock
    private HrDepartmentScopeAccess departmentScopeAccess;

    @Test
    void shouldExposeGovernedStatsCapabilitiesForVwAnalyticsFolhaPagamento() {
        VwAnalyticsFolhaPagamentoService service = new VwAnalyticsFolhaPagamentoService(
                repository, mapper, departmentScopeAccess);

        assertEquals(StatsSupportMode.AUTO, service.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getTimeSeriesStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getDistributionStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getComparisonStatsSupportMode());

        var departmentField = service.getStatsFieldRegistry().resolve("departamento").orElseThrow();
        assertTrue(departmentField.groupByEligible());
        assertEquals("departamentoId", departmentField.keyPropertyPath());
        assertEquals("departamento", departmentField.labelPropertyPath());
        assertTrue(service.getStatsFieldRegistry().resolve("payrollProfile").orElseThrow().distributionTermsEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("competencia").orElseThrow().timeSeriesEligible());

        assertTrue(service.getStatsFieldRegistry().resolve("salarioLiquido").orElseThrow().metricFieldEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("salarioLiquido").orElseThrow().distributionHistogramEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("salarioLiquido").orElseThrow().supports(StatsMetric.SUM));

        assertTrue(service.getStatsFieldRegistry().resolve("pctDesconto").orElseThrow().metricFieldEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("pctDesconto").orElseThrow().supports(StatsMetric.AVG));

        assertEquals(OptionSourceType.DISTINCT_DIMENSION,
                service.getOptionSourceRegistry().resolve(com.example.praxis.apiquickstart.hr.entity.VwAnalyticsFolhaPagamento.class, "payrollProfile").orElseThrow().type());
        assertEquals(Map.of("universo", "universoContexto"),
                service.getOptionSourceRegistry().resolve(com.example.praxis.apiquickstart.hr.entity.VwAnalyticsFolhaPagamento.class, "payrollProfile").orElseThrow().dependencyFilterMap());
        assertEquals(OptionSourceType.CATEGORICAL_BUCKET,
                service.getOptionSourceRegistry().resolve(com.example.praxis.apiquickstart.hr.entity.VwAnalyticsFolhaPagamento.class, "faixaPctDesconto").orElseThrow().type());
    }
}
