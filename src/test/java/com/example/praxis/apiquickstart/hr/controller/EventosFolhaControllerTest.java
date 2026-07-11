package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.hr.dto.EventosFolhaResponseDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.BulkApproveEventosFolhaResultDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.EventosFolhaFilterDTO;
import com.example.praxis.apiquickstart.hr.service.EventosFolhaService;
import com.example.praxis.apiquickstart.core.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.praxisplatform.uischema.capability.AvailabilityDecision;
import org.praxisplatform.uischema.capability.CapabilityService;
import org.praxisplatform.uischema.concurrency.ResourceVersionEtagService;
import org.praxisplatform.uischema.filter.web.FilterRequestBodyAdvice;
import org.praxisplatform.uischema.rest.exceptionhandler.GlobalExceptionHandler;
import org.praxisplatform.uischema.service.base.BaseResourceCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventosFolhaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({FilterRequestBodyAdvice.class, GlobalExceptionHandler.class, EventosFolhaControllerTest.VersionEtagConfig.class})
@TestPropertySource(properties = "praxis.resource-version.etag.secret=test-secret-resource-version")
class EventosFolhaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventosFolhaService service;

    @MockBean
    private com.example.praxis.apiquickstart.core.service.ResourceActionExecutionService actionExecutionService;

    @MockBean
    private com.example.praxis.apiquickstart.security.JwtTokenService jwtTokenService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @MockBean
    private CapabilityService capabilityService;

    @Test
    void bulkApprove_shouldReturnResultEnvelope() throws Exception {
        BulkApproveEventosFolhaResultDTO result = new BulkApproveEventosFolhaResultDTO();
        result.setTotal(2);
        result.setProcessed(2);
        result.setFailed(0);

        when(capabilityService.collectionOperationAvailability(
                "human-resources.eventos-folha",
                "/api/human-resources/eventos-folha",
                "bulk-approve"))
                .thenReturn(AvailabilityDecision.allowAll());
        when(service.bulkApprove(any(), any(), any())).thenReturn(result);
        when(service.getResourceVersion(1)).thenReturn(OptionalLong.of(0));
        when(service.getDatasetVersion()).thenReturn(Optional.of("EventosFolha:2"));

        mockMvc.perform(post("/api/human-resources/eventos-folha/actions/bulk-approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkApprovePayload()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Data-Version", "EventosFolha:2"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.processed").value(2))
                .andExpect(jsonPath("$.data.failed").value(0))
                .andExpect(jsonPath("$._links.schema[0].href").exists())
                .andExpect(jsonPath("$._links.schema[1].href").exists());
    }

    @Test
    void bulkApprove_shouldNotExecuteServiceWhenCapabilityDeniesAction() throws Exception {
        when(capabilityService.collectionOperationAvailability(
                "human-resources.eventos-folha",
                "/api/human-resources/eventos-folha",
                "bulk-approve"))
                .thenReturn(AvailabilityDecision.deny("payroll-window-closed", null));
        when(service.getResourceVersion(1)).thenReturn(OptionalLong.of(0));
        when(service.getDatasetVersion()).thenReturn(Optional.of("EventosFolha:2"));

        mockMvc.perform(post("/api/human-resources/eventos-folha/actions/bulk-approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkApprovePayload()))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Data-Version", "EventosFolha:2"))
                .andExpect(jsonPath("$.status").value("failure"))
                .andExpect(jsonPath("$.message").value("payroll-window-closed"))
                .andExpect(jsonPath("$.errors[0].category").value("SECURITY"))
                .andExpect(jsonPath("$.errors[0].outcome").value("PERMISSION_DENIED"));

        verify(service, never()).bulkApprove(any(), any(), any());
    }

    @Test
    void create_shouldReturnCreatedEnvelope() throws Exception {
        EventosFolhaResponseDTO response = new EventosFolhaResponseDTO();
        response.setId(10);
        response.setDescricao("INSS");
        response.setTipo("DESCONTO");
        response.setValor(new BigDecimal("123.45"));
        response.setFolhaPagamentoId(99);
        response.setFolhaPagamentoNome("2026-03");

        when(service.create(any())).thenReturn(new BaseResourceCommandService.SavedResult<>(10, response));
        when(service.getDatasetVersion()).thenReturn(Optional.of("EventosFolha:7"));

        mockMvc.perform(post("/api/human-resources/eventos-folha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descricao": "INSS",
                                  "tipo": "DESCONTO",
                                  "valor": 123.45,
                                  "folhaPagamentoId": 99
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/human-resources/eventos-folha/10"))
                .andExpect(header().string("X-Data-Version", "EventosFolha:7"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.descricao").value("INSS"));
    }

    @Test
    void filter_shouldNormalizeValorAliasIntoValorBetween() throws Exception {
        stubEmptyFilterPage();

        mockMvc.perform(post("/api/human-resources/eventos-folha/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valor": { "minPrice": 15000, "maxPrice": 6500 }
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<EventosFolhaFilterDTO> captor = ArgumentCaptor.forClass(EventosFolhaFilterDTO.class);
        verify(service).filter(captor.capture(), any(Pageable.class), any());
        EventosFolhaFilterDTO dto = captor.getValue();
        assertNotNull(dto.getValorBetween());
        assertEquals(2, dto.getValorBetween().size());
        assertEquals(0, dto.getValorBetween().get(0).compareTo(new BigDecimal("6500")));
        assertEquals(0, dto.getValorBetween().get(1).compareTo(new BigDecimal("15000")));
    }

    @Test
    void filter_shouldNormalizeLegacyValorMinValorMaxIntoValorBetween() throws Exception {
        stubEmptyFilterPage();

        mockMvc.perform(post("/api/human-resources/eventos-folha/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valorMin": null,
                                  "valorMax": 7200
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<EventosFolhaFilterDTO> captor = ArgumentCaptor.forClass(EventosFolhaFilterDTO.class);
        verify(service).filter(captor.capture(), any(Pageable.class), any());
        EventosFolhaFilterDTO dto = captor.getValue();
        assertNotNull(dto.getValorBetween());
        assertEquals(2, dto.getValorBetween().size());
        assertNull(dto.getValorBetween().get(0));
        assertEquals(0, dto.getValorBetween().get(1).compareTo(new BigDecimal("7200")));
    }

    @Test
    void filter_shouldRejectConflictingValorSources() throws Exception {
        mockMvc.perform(post("/api/human-resources/eventos-folha/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valorBetween": [100, 200],
                                  "valor": { "minPrice": 150, "maxPrice": 300 }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Range payload for field 'valorBetween' provides conflicting sources. Use only one source."));

        verify(service, never()).filter(any(), any(Pageable.class), any());
    }

    private void stubEmptyFilterPage() {
        when(service.getDatasetVersion()).thenReturn(Optional.empty());
        when(service.getDefaultSort()).thenReturn(Sort.unsorted());
        when(service.filter(any(), any(Pageable.class), any()))
                .thenReturn(new PageImpl<>(List.of()));
    }

    private String bulkApprovePayload() {
        String etag = new ResourceVersionEtagService("test-secret-resource-version")
                .create("human-resources.eventos-folha", 1, 0)
                .replace("\"", "\\\"");
        return """
                {
                  "ids": [1],
                  "effectiveAt": "2026-07-11",
                  "reasonCode": "FECHAMENTO_CONFERIDO",
                  "comment": "Valores conferidos para fechamento.",
                  "expectedVersions": {"1": "%s"}
                }
                """.formatted(etag);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class VersionEtagConfig {
        @Bean
        ResourceVersionEtagService resourceVersionEtagService() {
            return new ResourceVersionEtagService("test-secret-resource-version");
        }
    }
}
