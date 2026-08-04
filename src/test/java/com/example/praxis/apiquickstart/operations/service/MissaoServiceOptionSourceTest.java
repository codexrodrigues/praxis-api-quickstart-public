package com.example.praxis.apiquickstart.operations.service;

import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.entity.Missao;
import com.example.praxis.apiquickstart.operations.mapper.MissaoMapper;
import com.example.praxis.apiquickstart.operations.repository.MissaoParticipanteRepository;
import com.example.praxis.apiquickstart.operations.repository.MissaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class MissaoServiceOptionSourceTest {

    @Mock
    private MissaoRepository repository;

    @Mock
    private MissaoParticipanteRepository participanteRepository;

    @Mock
    private MissaoMapper mapper;

    @Mock
    private DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    @Test
    void shouldExposeMissionDetailThroughTheGovernedSummarySurface() {
        MissaoService service = new MissaoService(
                repository,
                participanteRepository,
                mapper,
                workflowActionPolicyResolver
        );

        var descriptor = service.getOptionSourceRegistry()
                .resolve(Missao.class, ApiPaths.Operations.MISSOES_MISSION_LOOKUP_SOURCE)
                .orElseThrow();
        var detail = descriptor.entityLookup().detail();

        assertEquals("surface", detail.kind());
        assertEquals("summary", detail.surfaceId());
        assertEquals("modal", detail.presentation());
        assertEquals("praxis-dynamic-form", detail.preferredWidget());
        assertEquals("view", detail.mode());
        assertNull(detail.hrefTemplate());
        assertNull(detail.routeTemplate());
        assertNull(detail.openDetailMode());
    }
}
