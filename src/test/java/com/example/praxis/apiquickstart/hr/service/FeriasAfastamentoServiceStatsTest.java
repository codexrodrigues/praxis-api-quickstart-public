package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.mapper.FeriasAfastamentoMapper;
import com.example.praxis.apiquickstart.hr.repository.FeriasAfastamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class FeriasAfastamentoServiceStatsTest {

    @Mock
    private FeriasAfastamentoRepository repository;

    @Mock
    private FeriasAfastamentoMapper mapper;

    @Test
    void shouldExposeGovernedStatsCapabilitiesForAbsenceCalendar() {
        FeriasAfastamentoService service = new FeriasAfastamentoService(repository, mapper);

        assertEquals(StatsSupportMode.AUTO, service.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getTimeSeriesStatsSupportMode());
        assertEquals(StatsSupportMode.DISABLED, service.getDistributionStatsSupportMode());

        assertTrue(service.getStatsFieldRegistry().resolve("tipo").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("funcionarioId").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("dataInicio").orElseThrow().timeSeriesEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("dataFim").orElseThrow().timeSeriesEligible());
    }
}
