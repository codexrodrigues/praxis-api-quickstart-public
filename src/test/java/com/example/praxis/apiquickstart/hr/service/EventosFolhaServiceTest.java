package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.config.DomainRuleApprovalPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleApprovalPolicyResolver;
import com.example.praxis.apiquickstart.hr.dto.actions.BulkApproveEventosFolhaRequestDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.BulkApproveEventosFolhaResultDTO;
import com.example.praxis.apiquickstart.hr.mapper.EventosFolhaMapper;
import com.example.praxis.apiquickstart.hr.repository.EventosFolhaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventosFolhaServiceTest {

    @Mock
    private EventosFolhaRepository repository;

    @Mock
    private EventosFolhaMapper mapper;

    @Mock
    private JdbcTemplate apiJdbcTemplate;

    @Mock
    private DomainRuleApprovalPolicyResolver approvalPolicyResolver;

    @InjectMocks
    private EventosFolhaService service;

    @BeforeEach
    void setUpApprovalPolicyFallback() {
        lenient().when(approvalPolicyResolver.resolveAppliedPolicy("human-resources.eventos-folha:bulk-approve"))
                .thenReturn(Optional.empty());
    }

    @Test
    void bulkApprove_shouldFailWhenStatusColumnDoesNotExist() {
        when(apiJdbcTemplate.queryForObject(anyString(), eq(Boolean.class))).thenReturn(Boolean.FALSE);

        BulkApproveEventosFolhaRequestDTO request = new BulkApproveEventosFolhaRequestDTO();
        request.setIds(List.of(1, 2, 3));

        BulkApproveEventosFolhaResultDTO result = service.bulkApprove(request);

        assertEquals(3, result.getTotal());
        assertEquals(0, result.getProcessed());
        assertEquals(3, result.getFailed());
        verify(apiJdbcTemplate, never()).update(anyString(), any(), any(), any());
    }

    @Test
    void bulkApprove_shouldPersistStatusWhenColumnExists() {
        when(apiJdbcTemplate.queryForObject(anyString(), eq(Boolean.class))).thenReturn(Boolean.TRUE);
        when(apiJdbcTemplate.update(anyString(), eq("APROVADO"), eq(10), eq("PENDENTE"))).thenReturn(1);

        BulkApproveEventosFolhaRequestDTO request = new BulkApproveEventosFolhaRequestDTO();
        request.setIds(List.of(10));

        BulkApproveEventosFolhaResultDTO result = service.bulkApprove(request);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getProcessed());
        assertEquals(0, result.getFailed());
        verify(apiJdbcTemplate).update(anyString(), eq("APROVADO"), eq(10), eq("PENDENTE"));
        verify(apiJdbcTemplate, never()).queryForList(anyString(), eq(String.class), eq(10));
    }

    @Test
    void bulkApprove_shouldRejectWhenGovernedApprovalPolicyRequiresApproval() {
        when(approvalPolicyResolver.resolveAppliedPolicy("human-resources.eventos-folha:bulk-approve"))
                .thenReturn(Optional.of(new DomainRuleApprovalPolicy(
                        "human-resources.eventos-folha:bulk-approve",
                        "human-resources.eventos-folha",
                        "bulk-approve",
                        List.of("payroll-manager"),
                        List.of("hr-payroll"),
                        "payroll-events",
                        "Aprovacao em massa exige decisao gerencial governada.")));

        BulkApproveEventosFolhaRequestDTO request = new BulkApproveEventosFolhaRequestDTO();
        request.setIds(List.of(10));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.bulkApprove(request));

        assertEquals("409 CONFLICT \"Aprovacao em massa exige decisao gerencial governada.\"", exception.getMessage());
        verify(apiJdbcTemplate, never()).update(anyString(), any(), any(), any());
    }

    @Test
    void bulkApprove_shouldRejectEventOutsidePendingState() {
        when(apiJdbcTemplate.queryForObject(anyString(), eq(Boolean.class))).thenReturn(Boolean.TRUE);
        when(apiJdbcTemplate.update(anyString(), eq("APROVADO"), eq(10), eq("PENDENTE"))).thenReturn(0);
        when(apiJdbcTemplate.queryForList(anyString(), eq(String.class), eq(10)))
                .thenReturn(List.of("APROVADO"));

        BulkApproveEventosFolhaRequestDTO request = new BulkApproveEventosFolhaRequestDTO();
        request.setIds(List.of(10));

        BulkApproveEventosFolhaResultDTO result = service.bulkApprove(request);

        assertEquals(1, result.getTotal());
        assertEquals(0, result.getProcessed());
        assertEquals(1, result.getFailed());
        assertEquals("State not allowed: APROVADO", result.getDetails().getFirst().getError());
        verify(apiJdbcTemplate).update(anyString(), eq("APROVADO"), eq(10), eq("PENDENTE"));
    }

    @Test
    void bulkApprove_shouldReturnNotFoundWhenAtomicUpdateDoesNotMatchAnyRow() {
        when(apiJdbcTemplate.queryForObject(anyString(), eq(Boolean.class))).thenReturn(Boolean.TRUE);
        when(apiJdbcTemplate.update(anyString(), eq("APROVADO"), eq(10), eq("PENDENTE"))).thenReturn(0);
        when(apiJdbcTemplate.queryForList(anyString(), eq(String.class), eq(10)))
                .thenReturn(List.of());

        BulkApproveEventosFolhaRequestDTO request = new BulkApproveEventosFolhaRequestDTO();
        request.setIds(List.of(10));

        BulkApproveEventosFolhaResultDTO result = service.bulkApprove(request);

        assertEquals(1, result.getTotal());
        assertEquals(0, result.getProcessed());
        assertEquals(1, result.getFailed());
        assertEquals("Not found", result.getDetails().getFirst().getError());
    }
}
