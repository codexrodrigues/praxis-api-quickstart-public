package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.config.DomainRuleApprovalPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleApprovalPolicyResolver;
import com.example.praxis.apiquickstart.core.service.ResourceActionTransitionService;
import com.example.praxis.apiquickstart.hr.dto.actions.BulkApproveEventosFolhaRequestDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.BulkApproveEventosFolhaResultDTO;
import com.example.praxis.apiquickstart.hr.entity.EventosFolha;
import com.example.praxis.apiquickstart.hr.enums.StatusEventoFolha;
import com.example.praxis.apiquickstart.hr.mapper.EventosFolhaMapper;
import com.example.praxis.apiquickstart.hr.repository.EventosFolhaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
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
    private DomainRuleApprovalPolicyResolver approvalPolicyResolver;

    @Mock
    private ResourceActionTransitionService transitionService;

    @Mock
    private EventosFolhaApprovalItemService approvalItemService;

    @InjectMocks
    private EventosFolhaService service;

    @BeforeEach
    void setUpApprovalPolicyFallback() {
        lenient().when(approvalPolicyResolver.resolveAppliedPolicy("human-resources.eventos-folha:bulk-approve"))
                .thenReturn(Optional.empty());
    }

    @Test
    void bulkApprove_shouldPersistPendingEvent() {
        UUID transitionId = UUID.randomUUID();
        when(approvalItemService.approve(any(), any(), any(), any())).thenReturn(transitionId);

        BulkApproveEventosFolhaRequestDTO request = command(10);

        BulkApproveEventosFolhaResultDTO result = service.bulkApprove(request, "admin", "correlation-1");

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getProcessed());
        assertEquals(0, result.getFailed());
        assertEquals(transitionId, result.getDetails().getFirst().getTransitionId());
        verify(approvalItemService).approve(eq(10), eq(request), eq("admin"), eq("correlation-1"));
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

        BulkApproveEventosFolhaRequestDTO request = command(10);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.bulkApprove(request, "admin", "correlation-1"));

        assertEquals("409 CONFLICT \"Aprovacao em massa exige decisao gerencial governada.\"", exception.getMessage());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void bulkApprove_shouldRejectEventOutsidePendingState() {
        when(approvalItemService.approve(any(), any(), any(), any()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "State not allowed: APROVADO"));

        BulkApproveEventosFolhaRequestDTO request = command(10);

        BulkApproveEventosFolhaResultDTO result = service.bulkApprove(request, "admin", "correlation-1");

        assertEquals(1, result.getTotal());
        assertEquals(0, result.getProcessed());
        assertEquals(1, result.getFailed());
        assertEquals("State not allowed: APROVADO", result.getDetails().getFirst().getError());
        verify(approvalItemService).approve(eq(10), eq(request), eq("admin"), eq("correlation-1"));
    }

    @Test
    void bulkApprove_shouldReturnNotFoundWhenAtomicUpdateDoesNotMatchAnyRow() {
        when(approvalItemService.approve(any(), any(), any(), any()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Not found"));

        BulkApproveEventosFolhaRequestDTO request = command(10);

        BulkApproveEventosFolhaResultDTO result = service.bulkApprove(request, "admin", "correlation-1");

        assertEquals(1, result.getTotal());
        assertEquals(0, result.getProcessed());
        assertEquals(1, result.getFailed());
        assertEquals("Not found", result.getDetails().getFirst().getError());
    }

    private EventosFolha event(Integer id, StatusEventoFolha status) {
        EventosFolha event = new EventosFolha();
        event.setId(id);
        event.setStatus(status);
        event.setVersion(0L);
        return event;
    }

    private BulkApproveEventosFolhaRequestDTO command(Integer... ids) {
        BulkApproveEventosFolhaRequestDTO request = new BulkApproveEventosFolhaRequestDTO();
        request.setIds(List.of(ids));
        request.setEffectiveAt(LocalDate.of(2026, 7, 11));
        request.setReasonCode("FECHAMENTO_CONFERIDO");
        request.setComment("Valores conferidos para fechamento.");
        request.setExpectedVersions(Map.of(ids[0], "\"test-etag\""));
        return request;
    }
}
