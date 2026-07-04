package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.actions.AbsenceCoverageWorkflowRequestDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.AbsenceCoverageWorkflowResultDTO;
import com.example.praxis.apiquickstart.hr.entity.FeriasAfastamento;
import com.example.praxis.apiquickstart.hr.mapper.FeriasAfastamentoMapper;
import com.example.praxis.apiquickstart.hr.repository.FeriasAfastamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void planCoverage_shouldPersistCoverageEvidenceInNotes() {
        FeriasAfastamento absence = new FeriasAfastamento();
        absence.setId(42);
        absence.setTipo("FERIAS");
        absence.setDataInicio(LocalDate.of(2026, 7, 1));
        absence.setDataFim(LocalDate.of(2026, 7, 15));
        absence.setObservacoes("Aprovado por RH.");

        when(repository.findById(42)).thenReturn(Optional.of(absence));
        when(repository.save(any(FeriasAfastamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AbsenceCoverageWorkflowRequestDTO request = new AbsenceCoverageWorkflowRequestDTO();
        request.setPlanoCobertura("Cobertura pela equipe B.");
        request.setSubstitutoFuncionarioId(12);
        request.setJustificativa("Janela cruza fechamento de folha.");

        FeriasAfastamentoService service = new FeriasAfastamentoService(repository, mapper);
        AbsenceCoverageWorkflowResultDTO result = service.planCoverage(42, request);

        assertEquals(42, result.getId());
        assertEquals("FERIAS", result.getTipo());
        assertEquals("Cobertura pela equipe B.", result.getPlanoCobertura());
        assertEquals(12, result.getSubstitutoFuncionarioId());
        assertEquals("Cobertura da ausencia registrada.", result.getMensagem());
        assertNotNull(result.getObservacoes());
        assertTrue(result.getObservacoes().contains("Aprovado por RH."));
        assertTrue(result.getObservacoes().contains("[COBERTURA_PLANEJADA"));
        assertTrue(result.getObservacoes().contains("substitutoFuncionarioId=12"));
        assertTrue(result.getObservacoes().contains("Janela cruza fechamento de folha."));
        verify(repository).save(absence);
    }

    @Test
    void planCoverage_shouldRejectWhenCoverageEvidenceExceedsNotesCapacity() {
        FeriasAfastamento absence = new FeriasAfastamento();
        absence.setId(42);
        absence.setTipo("AFASTAMENTO");
        absence.setDataInicio(LocalDate.of(2026, 8, 1));
        absence.setDataFim(LocalDate.of(2026, 8, 10));
        absence.setObservacoes("x".repeat(1990));

        when(repository.findById(42)).thenReturn(Optional.of(absence));

        AbsenceCoverageWorkflowRequestDTO request = new AbsenceCoverageWorkflowRequestDTO();
        request.setPlanoCobertura("Cobertura pela equipe B.");

        FeriasAfastamentoService service = new FeriasAfastamentoService(repository, mapper);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.planCoverage(42, request));

        assertEquals("409 CONFLICT \"Coverage evidence exceeds absence notes capacity.\"", exception.getMessage());
    }
}
