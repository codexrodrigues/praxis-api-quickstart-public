package com.example.praxis.apiquickstart.operationalassets.service;

import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operationalassets.entity.Equipamento;
import com.example.praxis.apiquickstart.operationalassets.mapper.EquipamentoMapper;
import com.example.praxis.apiquickstart.operationalassets.repository.EquipamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class EquipamentoServiceOptionSourceTest {

    @Mock
    private EquipamentoRepository repository;

    @Mock
    private EquipamentoMapper mapper;

    @Mock
    private DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    @Test
    void shouldExposeEquipmentDetailThroughTheGovernedItemSurface() {
        EquipamentoService service = new EquipamentoService(
                repository,
                mapper,
                workflowActionPolicyResolver
        );

        var descriptor = service.getOptionSourceRegistry()
                .resolve(Equipamento.class, ApiPaths.Assets.EQUIPAMENTOS_EQUIPMENT_LOOKUP_SOURCE)
                .orElseThrow();
        var detail = descriptor.entityLookup().detail();

        assertEquals("surface", detail.kind());
        assertEquals("view", detail.surfaceId());
        assertEquals("drawer", detail.presentation());
        assertEquals("praxis-dynamic-form", detail.preferredWidget());
        assertEquals("view", detail.mode());
        assertNull(detail.hrefTemplate());
        assertNull(detail.routeTemplate());
        assertNull(detail.openDetailMode());
    }
}
