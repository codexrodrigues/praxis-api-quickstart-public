package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.mapper.MissaoEventoMapper;
import com.example.praxis.apiquickstart.operations.repository.MissaoEventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MissaoEventoServiceStatsTest {

    @Mock
    private MissaoEventoRepository repository;

    @Mock
    private MissaoEventoMapper mapper;

    @Test
    void shouldExposeGovernedStatsCapabilitiesForMissionTimeline() {
        MissaoEventoService service = new MissaoEventoService(repository, mapper);

        assertEquals(StatsSupportMode.AUTO, service.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getTimeSeriesStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getDistributionStatsSupportMode());

        assertTrue(service.getStatsFieldRegistry().resolve("tipo").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("tipo").orElseThrow().distributionTermsEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("missaoStatus").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("ocorridoEm").orElseThrow().timeSeriesEligible());
    }
}
