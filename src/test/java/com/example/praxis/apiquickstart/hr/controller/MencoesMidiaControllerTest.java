package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.core.RateLimiterService;
import com.example.praxis.apiquickstart.hr.dto.MencoesMidiaDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.MencoesMidiaFilterDTO;
import com.example.praxis.apiquickstart.hr.mapper.MencoesMidiaMapper;
import com.example.praxis.apiquickstart.hr.service.MencoesMidiaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.praxisplatform.uischema.dto.CursorPage;
import org.praxisplatform.uischema.configuration.OpenApiUiSchemaAutoConfiguration;
import org.praxisplatform.uischema.rest.exceptionhandler.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MencoesMidiaController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(OpenApiUiSchemaAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
class MencoesMidiaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MencoesMidiaService service;

    @MockBean
    private MencoesMidiaMapper mapper;

    @MockBean
    private RateLimiterService rateLimiter;

    @MockBean
    private com.example.praxis.apiquickstart.security.JwtTokenService jwtTokenService;

    @Test
    void filterShouldNormalizeRelativePeriodPresetIntoLastDays() throws Exception {
        stubEmptyFilterPage();

        mockMvc.perform(post("/api/human-resources/mencoes-midia/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "publicadoEmPreset": "last7"
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<MencoesMidiaFilterDTO> captor = ArgumentCaptor.forClass(MencoesMidiaFilterDTO.class);
        verify(service).filter(captor.capture(), any(Pageable.class), any());
        MencoesMidiaFilterDTO dto = captor.getValue();
        assertEquals(7, dto.getPublicadoEmLastDays());
        assertNull(dto.getPublicadoEmPreset());
        assertNull(dto.getPublicadoEmOn());
        assertNull(dto.getPublicadoEmBetween());
    }

    @Test
    void filterShouldNormalizeRelativePeriodPresetIntoOnDate() throws Exception {
        stubEmptyFilterPage();

        mockMvc.perform(post("/api/human-resources/mencoes-midia/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "publicadoEmPreset": "today"
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<MencoesMidiaFilterDTO> captor = ArgumentCaptor.forClass(MencoesMidiaFilterDTO.class);
        verify(service).filter(captor.capture(), any(Pageable.class), any());
        MencoesMidiaFilterDTO dto = captor.getValue();
        assertEquals(LocalDate.now(ZoneOffset.UTC), dto.getPublicadoEmOn());
        assertNull(dto.getPublicadoEmPreset());
        assertNull(dto.getPublicadoEmLastDays());
    }

    @Test
    void filterShouldRejectConflictingRelativePresetAndAbsoluteFields() throws Exception {
        mockMvc.perform(post("/api/human-resources/mencoes-midia/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "publicadoEmPreset": "last7",
                                  "publicadoEmLastDays": 30
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Relative period preset for field 'publicadoEm' conflicts with explicit absolute filters. Use only one source."));

        verify(service, never()).filter(any(), any(Pageable.class), any());
    }

    @Test
    void filterByCursorShouldNormalizeRelativePeriodPresetIntoBetween() throws Exception {
        when(service.getDatasetVersion()).thenReturn(Optional.empty());
        when(service.filterByCursor(any(), any(), any(), any(), any(Integer.class)))
                .thenReturn(new CursorPage<>(List.of(), null, null, 20));

        mockMvc.perform(post("/api/human-resources/mencoes-midia/filter/cursor?size=20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "publicadoEmPreset": "lastMonth"
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<MencoesMidiaFilterDTO> captor = ArgumentCaptor.forClass(MencoesMidiaFilterDTO.class);
        verify(service).filterByCursor(captor.capture(), any(), any(), any(), any(Integer.class));
        MencoesMidiaFilterDTO dto = captor.getValue();
        assertNull(dto.getPublicadoEmPreset());
        assertNull(dto.getPublicadoEmLastDays());
        assertNull(dto.getPublicadoEmOn());
        assertTrue(dto.getPublicadoEmBetween() != null && dto.getPublicadoEmBetween().size() == 2);
        YearMonth previousMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(1);
        assertEquals(previousMonth.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC), dto.getPublicadoEmBetween().get(0));
        assertEquals(previousMonth.atEndOfMonth().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC), dto.getPublicadoEmBetween().get(1));
    }

    private void stubEmptyFilterPage() {
        when(service.getDatasetVersion()).thenReturn(Optional.empty());
        when(service.filter(any(), any(Pageable.class), any()))
                .thenReturn(new PageImpl<MencoesMidiaDTO>(List.of(), PageRequest.of(0, 20), 0));
    }
}
