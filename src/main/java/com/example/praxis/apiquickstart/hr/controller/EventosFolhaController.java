package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.entity.ResourceActionExecution;
import com.example.praxis.apiquickstart.core.service.ResourceActionExecutionService;
import com.example.praxis.apiquickstart.hr.dto.CreateEventosFolhaDTO;
import com.example.praxis.apiquickstart.hr.dto.EventosFolhaResponseDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateEventosFolhaDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.BulkApproveEventosFolhaRequestDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.BulkApproveEventosFolhaResultDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.EventoFolhaWorkflowResultDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.RejectEventoFolhaRequestDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.EventosFolhaFilterDTO;
import com.example.praxis.apiquickstart.hr.enums.StatusEventoFolha;
import com.example.praxis.apiquickstart.hr.service.EventosFolhaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.command.ResourceCommandExecutionResult;
import org.praxisplatform.uischema.command.ResourceCommandExecutionRequest;
import org.praxisplatform.uischema.command.ResourceCommandResponsePolicy;
import org.praxisplatform.uischema.controller.base.AbstractResourceController;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Demonstrates governed, auditable payroll workflow actions. */
@RestController
@ApiResource(value = ApiPaths.HumanResources.EVENTOS_FOLHA, resourceKey = "human-resources.eventos-folha")
@ApiGroup("human-resources")
public class EventosFolhaController extends AbstractResourceController<
        EventosFolhaResponseDTO, Integer, EventosFolhaFilterDTO, CreateEventosFolhaDTO, UpdateEventosFolhaDTO> {

    private static final String RESOURCE_KEY = "human-resources.eventos-folha";

    private final EventosFolhaService service;
    private final ResourceActionExecutionService actionExecutionService;
    private final ObjectMapper objectMapper;

    public EventosFolhaController(
            EventosFolhaService service,
            ResourceActionExecutionService actionExecutionService,
            ObjectMapper objectMapper
    ) {
        this.service = service;
        this.actionExecutionService = actionExecutionService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected EventosFolhaService getService() {
        return service;
    }

    @Override
    protected Integer getResponseId(EventosFolhaResponseDTO dto) {
        return dto.getId();
    }

    @PostMapping("/actions/bulk-approve")
    @Operation(summary = "Aprovar eventos pendentes de folha em lote", description = "Executa a transição de aprovação para eventos de folha selecionados, com retorno individual para fechamento e tratamento de falhas parciais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eventos processados em lote."),
            @ApiResponse(responseCode = "400", description = "Comando ou versões esperadas inválidos."),
            @ApiResponse(responseCode = "409", description = "Ação indisponível, conflito de idempotência ou estado não apto.")
    })
    @WorkflowAction(id = "bulk-approve", title = "Aprovar eventos de folha em lote", description = "Aprova eventos pendentes selecionados para o fechamento de folha, preservando retorno individual.", scope = ActionScope.COLLECTION, order = 100, successMessage = "Eventos aprovados", allowedStates = {"PENDENTE"}, tags = {"payroll", "payroll-events", "approval-workflow", "bulk-action"})
    public ResponseEntity<RestApiResponse<BulkApproveEventosFolhaResultDTO>> bulkApprove(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BulkApproveEventosFolhaRequestDTO dto
    ) {
        var replay = actionExecutionService.findCompletedReplay(RESOURCE_KEY, "bulk-approve", idempotencyKey, dto);
        if (replay.isPresent()) {
            return withVersion(ResponseEntity.ok(), RestApiResponse.success(restoreBulkApproveResult(replay.get()), null));
        }

        Set<Integer> selectedIds = dto.getIds().stream().filter(id -> id != null).collect(java.util.stream.Collectors.toSet());
        if (selectedIds.size() != dto.getIds().size() || !dto.getExpectedVersions().keySet().equals(selectedIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Expected versions must contain exactly one ETag for each selected payroll event.");
        }
        selectedIds.forEach(id -> requireMatchingResourceVersion(id, dto.getExpectedVersions().get(id)));

        String actorSubject = SecurityContextHolder.getContext().getAuthentication() == null
                ? "anonymous"
                : SecurityContextHolder.getContext().getAuthentication().getName();
        String effectiveCorrelationId = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
        return governedBulkApprove(dto, idempotencyKey, actorSubject, effectiveCorrelationId);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<RestApiResponse<BulkApproveEventosFolhaResultDTO>> governedBulkApprove(
            BulkApproveEventosFolhaRequestDTO dto, String idempotencyKey, String actorSubject, String correlationId
    ) {
        return (ResponseEntity<RestApiResponse<BulkApproveEventosFolhaResultDTO>>) (ResponseEntity<?>) executeCollectionCommand(
                "bulk-approve",
                dto,
                ResourceCommandResponsePolicy.RETURN_COMMAND_RESULT,
                request -> executeIdempotentBulkApprove(request, dto, idempotencyKey, actorSubject, correlationId)
        );
    }

    private ResourceCommandExecutionResult executeIdempotentBulkApprove(
            ResourceCommandExecutionRequest request, BulkApproveEventosFolhaRequestDTO dto, String idempotencyKey,
            String actorSubject, String correlationId
    ) {
        var execution = actionExecutionService.reserve(RESOURCE_KEY, "bulk-approve", ActionScope.COLLECTION,
                idempotencyKey, dto, correlationId, actorSubject);
        BulkApproveEventosFolhaResultDTO result;
        if (execution.isPresent() && "COMPLETED".equals(execution.get().getExecutionStatus())) {
            result = restoreBulkApproveResult(execution.get());
        } else {
            try {
                result = service.bulkApprove(dto, actorSubject, correlationId);
            } catch (RuntimeException failure) {
                execution.ifPresent(value -> actionExecutionService.fail(value, failure));
                throw failure;
            }
            execution.ifPresent(value -> actionExecutionService.complete(value, result));
        }
        return ResourceCommandExecutionResult.success(request, null, result, Map.of("resourceKey", RESOURCE_KEY));
    }

    @PostMapping("/{id}/actions/reject")
    @WorkflowAction(id = "reject", title = "Rejeitar evento de folha", description = "Rejeita um evento pendente com vigência e justificativa auditáveis.", scope = ActionScope.ITEM, allowedStates = {"PENDENTE"}, successMessage = "Evento de folha rejeitado", order = 110, tags = {"payroll", "approval-workflow"})
    @Operation(summary = "Rejeitar evento pendente de folha")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Evento rejeitado."), @ApiResponse(responseCode = "409", description = "Evento não está pendente."), @ApiResponse(responseCode = "412", description = "A versão do evento está desatualizada.")})
    public ResponseEntity<RestApiResponse<EventoFolhaWorkflowResultDTO>> reject(
            @PathVariable Integer id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @Valid @RequestBody RejectEventoFolhaRequestDTO command
    ) {
        var replay = service.findRejectReplay(id);
        if (replay.isPresent()) {
            return withResourceVersion(ResponseEntity.ok(), id,
                    RestApiResponse.success(new EventoFolhaWorkflowResultDTO(replay.get(), StatusEventoFolha.REJEITADO), null));
        }
        requireMatchingResourceVersion(id, ifMatch);
        String actor = SecurityContextHolder.getContext().getAuthentication() == null
                ? "anonymous"
                : SecurityContextHolder.getContext().getAuthentication().getName();
        String correlation = correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId;
        var transitionId = service.reject(id, command, actor, correlation);
        return withResourceVersion(ResponseEntity.ok(), id,
                RestApiResponse.success(new EventoFolhaWorkflowResultDTO(transitionId, StatusEventoFolha.REJEITADO), null));
    }

    private BulkApproveEventosFolhaResultDTO restoreBulkApproveResult(ResourceActionExecution execution) {
        try {
            return objectMapper.treeToValue(execution.getResponsePayload(), BulkApproveEventosFolhaResultDTO.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Stored idempotent result cannot be restored", ex);
        }
    }
}
