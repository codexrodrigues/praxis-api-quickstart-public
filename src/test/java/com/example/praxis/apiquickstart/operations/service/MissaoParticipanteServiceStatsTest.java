package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.operations.mapper.MissaoParticipanteMapper;
import com.example.praxis.apiquickstart.operations.repository.MissaoParticipanteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MissaoParticipanteServiceStatsTest {

    @Mock
    private MissaoParticipanteRepository repository;

    @Mock
    private MissaoParticipanteMapper mapper;

    @Test
    void shouldExposeGovernedStatsCapabilitiesForMissionTeamComposition() {
        MissaoParticipanteService service = new MissaoParticipanteService(repository, mapper);

        assertEquals(StatsSupportMode.AUTO, service.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getDistributionStatsSupportMode());

        assertTrue(service.getStatsFieldRegistry().resolve("papel").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("resultado").orElseThrow().distributionTermsEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("missaoStatus").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("missaoPrioridade").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("funcionarioId").orElseThrow().supports(StatsMetric.DISTINCT_COUNT));
    }
}
