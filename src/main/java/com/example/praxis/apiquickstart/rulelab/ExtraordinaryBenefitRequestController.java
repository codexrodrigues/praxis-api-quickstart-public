package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.entity.ResourceActionExecution;
import com.example.praxis.apiquickstart.core.service.ResourceActionExecutionService;
import com.example.praxis.apiquickstart.core.service.ResourceActionTransactionCoordinator;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitBatchEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitBatchEvaluationResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationCommandResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestFilter;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitTransitionRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitTransitionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.time.DateTimeException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.command.ResourceCommandExecutionRequest;
import org.praxisplatform.uischema.command.ResourceCommandExecutionResult;
import org.praxisplatform.uischema.command.ResourceCommandOutcome;
import org.praxisplatform.uischema.command.ResourceCommandResponsePolicy;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Recurso persistente QL-05: consultas sao read-only e toda mutacao ocorre por action governada. */
@RestController
@ApiResource(
        value = ApiPaths.HumanResources.EXTRAORDINARY_BENEFIT_REQUESTS,
        resourceKey = ExtraordinaryBenefitRequestController.RESOURCE_KEY,
        title = "Solicitacoes de beneficio extraordinario",
        description = "Fila persistida de beneficios elegiveis com lifecycle, concorrencia, idempotencia e execucao de efeito auditavel.",
        icon = "volunteer_activism",
        visualTone = "support")
@ApiGroup("human-resources")
public class ExtraordinaryBenefitRequestController extends AbstractReadOnlyResourceController<
        ExtraordinaryBenefitRequestResponse, Long, ExtraordinaryBenefitRequestFilter> {
    static final String RESOURCE_KEY = "human-resources.extraordinary-benefit-requests";

    private final ExtraordinaryBenefitRequestQueryService queryService;
    private final ExtraordinaryBenefitWorkflowService workflowService;
    private final ResourceActionExecutionService actionExecutionService;
    private final ResourceActionTransactionCoordinator transactionCoordinator;
    private final ObjectMapper objectMapper;

    public ExtraordinaryBenefitRequestController(
            ExtraordinaryBenefitRequestQueryService queryService,
            ExtraordinaryBenefitWorkflowService workflowService,
            ResourceActionExecutionService actionExecutionService,
            ResourceActionTransactionCoordinator transactionCoordinator,
            ObjectMapper objectMapper) {
        this.queryService = queryService;
        this.workflowService = workflowService;
        this.actionExecutionService = actionExecutionService;
        this.transactionCoordinator = transactionCoordinator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected ExtraordinaryBenefitRequestQueryService getService() {
        return queryService;
    }

    @Override
    protected Long getResponseId(ExtraordinaryBenefitRequestResponse response) {
        return response.id();
    }

    @PostMapping("/actions/evaluate")
    @WorkflowAction(
            id = "evaluate",
            title = "Avaliar e registrar beneficio",
            description = "Avalia fatos congelados e persiste somente ALLOW; negacao, inconclusao e falha nunca criam solicitacao nem efeito.",
            scope = ActionScope.COLLECTION,
            requiredAuthorities = {"ROLE_ADMIN"},
            order = 10,
            successMessage = "Avaliacao concluida",
            tags = {"human-resources", "benefits", "deterministic-rules", "idempotent"})
    @Operation(summary = "Avaliar e persistir solicitacao elegivel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliacao concluida; resource existe apenas para ALLOW."),
            @ApiResponse(responseCode = "400", description = "Payload, Idempotency-Key ou fuso invalido."),
            @ApiResponse(responseCode = "403", description = "Ator sem autoridade para solicitar o beneficio."),
            @ApiResponse(responseCode = "409", description = "Referencia ou chave idempotente conflita com comando anterior."),
            @ApiResponse(responseCode = "412", description = "Snapshot governado indisponivel.")
    })
    public ResponseEntity<RestApiResponse<ExtraordinaryBenefitEvaluationCommandResponse>> evaluate(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @Valid @RequestBody ExtraordinaryBenefitEvaluationRequest request) {
        requireAdmin();
        String actor = actorSubject();
        return executeIdempotentCollection(
                "evaluate", idempotencyKey, request, correlationId, actor,
                ExtraordinaryBenefitEvaluationCommandResponse.class,
                () -> workflowService.evaluateAndPersist(request, resolveActorPermissions(), actor));
    }

    @PostMapping("/actions/evaluate-batch")
    @WorkflowAction(
            id = "evaluate-batch",
            title = "Avaliar beneficios em lote",
            description = "Processa ate cinquenta itens em ordem, com decisao e transacao independentes e resultado parcial explicito.",
            scope = ActionScope.COLLECTION,
            requiredAuthorities = {"ROLE_ADMIN"},
            order = 20,
            successMessage = "Lote avaliado",
            tags = {"human-resources", "benefits", "batch", "partial-results", "idempotent"})
    @Operation(summary = "Avaliar lote nao atomico de beneficios")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lote concluido com resultado individual ordenado."),
            @ApiResponse(responseCode = "400", description = "Lote vazio, acima de cinquenta itens ou header invalido."),
            @ApiResponse(responseCode = "403", description = "Ator sem autoridade para solicitar beneficios."),
            @ApiResponse(responseCode = "409", description = "Chave idempotente reutilizada com outro lote."),
            @ApiResponse(responseCode = "412", description = "Snapshot governado indisponivel.")
    })
    public ResponseEntity<RestApiResponse<ExtraordinaryBenefitBatchEvaluationResponse>> evaluateBatch(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @Valid @RequestBody ExtraordinaryBenefitBatchEvaluationRequest request) {
        requireAdmin();
        String actor = actorSubject();
        return executeIdempotentCollection(
                "evaluate-batch", idempotencyKey, request, correlationId, actor,
                ExtraordinaryBenefitBatchEvaluationResponse.class,
                () -> workflowService.evaluateBatch(request, resolveActorPermissions(), actor));
    }

    @PostMapping("/{id}/actions/submit")
    @WorkflowAction(id = "submit", title = "Submeter solicitacao", description = "Avanca uma avaliacao elegivel para a fila de aprovacao.", scope = ActionScope.ITEM, requiredAuthorities = {"ROLE_ADMIN"}, allowedStates = {"EVALUATED"}, order = 100)
    @Operation(summary = "Submeter solicitacao avaliada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitacao submetida ou replay idempotente devolvido."),
            @ApiResponse(responseCode = "403", description = "Ator ou estado sem disponibilidade para a action."),
            @ApiResponse(responseCode = "404", description = "Solicitacao inexistente."),
            @ApiResponse(responseCode = "409", description = "Estado ou chave idempotente conflitante."),
            @ApiResponse(responseCode = "412", description = "ETag nao corresponde a versao persistida."),
            @ApiResponse(responseCode = "428", description = "If-Match nao informado.")
    })
    public ResponseEntity<RestApiResponse<ExtraordinaryBenefitTransitionResponse>> submit(
            @PathVariable Long id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @Valid @RequestBody ExtraordinaryBenefitTransitionRequest request) {
        return executeTransition("submit", id, ifMatch, idempotencyKey, correlationId, request,
                () -> workflowService.submit(id, request, actorSubject(), correlation(correlationId)));
    }

    @PostMapping("/{id}/actions/approve")
    @WorkflowAction(id = "approve", title = "Aprovar solicitacao", description = "Confirma aprovacao antes de qualquer efeito financeiro.", scope = ActionScope.ITEM, requiredAuthorities = {"ROLE_ADMIN"}, allowedStates = {"SUBMITTED"}, order = 110)
    @Operation(summary = "Aprovar solicitacao submetida")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitacao aprovada ou replay idempotente devolvido."),
            @ApiResponse(responseCode = "403", description = "Ator ou estado sem disponibilidade para a action."),
            @ApiResponse(responseCode = "404", description = "Solicitacao inexistente."),
            @ApiResponse(responseCode = "409", description = "Estado ou chave idempotente conflitante."),
            @ApiResponse(responseCode = "412", description = "ETag nao corresponde a versao persistida."),
            @ApiResponse(responseCode = "428", description = "If-Match nao informado.")
    })
    public ResponseEntity<RestApiResponse<ExtraordinaryBenefitTransitionResponse>> approve(
            @PathVariable Long id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @Valid @RequestBody ExtraordinaryBenefitTransitionRequest request) {
        return executeTransition("approve", id, ifMatch, idempotencyKey, correlationId, request,
                () -> workflowService.approve(id, request, actorSubject(), correlation(correlationId)));
    }

    @PostMapping("/{id}/actions/apply")
    @WorkflowAction(id = "apply", title = "Aplicar beneficio", description = "Executa exatamente uma vez a intencao aprovada e fecha o lifecycle.", scope = ActionScope.ITEM, requiredAuthorities = {"ROLE_ADMIN"}, allowedStates = {"APPROVED"}, order = 120)
    @Operation(summary = "Executar efeito aprovado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Efeito aplicado exatamente uma vez ou replay devolvido."),
            @ApiResponse(responseCode = "403", description = "Ator ou estado sem disponibilidade para a action."),
            @ApiResponse(responseCode = "404", description = "Solicitacao inexistente."),
            @ApiResponse(responseCode = "409", description = "Estado, ledger ou chave idempotente conflitante."),
            @ApiResponse(responseCode = "412", description = "ETag nao corresponde a versao persistida."),
            @ApiResponse(responseCode = "428", description = "If-Match nao informado.")
    })
    public ResponseEntity<RestApiResponse<ExtraordinaryBenefitTransitionResponse>> apply(
            @PathVariable Long id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @Valid @RequestBody ExtraordinaryBenefitTransitionRequest request) {
        return executeTransition("apply", id, ifMatch, idempotencyKey, correlationId, request,
                () -> workflowService.apply(id, request, actorSubject(), correlation(correlationId)));
    }

    private ResponseEntity<RestApiResponse<ExtraordinaryBenefitTransitionResponse>> executeTransition(
            String actionId,
            Long id,
            String ifMatch,
            String idempotencyKey,
            String correlationId,
            ExtraordinaryBenefitTransitionRequest request,
            Supplier<ExtraordinaryBenefitTransitionResponse> transition) {
        requireAdmin();
        validateIdempotencyKey(idempotencyKey);
        var replay = actionExecutionService.findCompletedReplay(RESOURCE_KEY, id, actionId, idempotencyKey, request);
        if (replay.isPresent()) {
            ExtraordinaryBenefitTransitionResponse restored = restore(replay.get(), ExtraordinaryBenefitTransitionResponse.class);
            return withResourceVersion(ResponseEntity.ok(), id, RestApiResponse.success(restored, null));
        }
        requireMatchingResourceVersion(id, ifMatch);
        String actor = actorSubject();
        String effectiveCorrelation = correlation(correlationId);
        @SuppressWarnings("unchecked")
        ResponseEntity<RestApiResponse<ExtraordinaryBenefitTransitionResponse>> response =
                (ResponseEntity<RestApiResponse<ExtraordinaryBenefitTransitionResponse>>) (ResponseEntity<?>) executeItemCommand(
                        actionId, id, request, ResourceCommandResponsePolicy.RETURN_COMMAND_RESULT,
                        command -> executeReserved(command, id, actionId, idempotencyKey, request,
                                effectiveCorrelation, actor, transition));
        return withResourceVersion(ResponseEntity.status(response.getStatusCode()), id, response.getBody());
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<RestApiResponse<T>> executeIdempotentCollection(
            String actionId,
            String idempotencyKey,
            Object request,
            String correlationId,
            String actor,
            Class<T> responseType,
            Supplier<T> operation) {
        validateIdempotencyKey(idempotencyKey);
        var replay = actionExecutionService.findCompletedReplay(RESOURCE_KEY, actionId, idempotencyKey, request);
        if (replay.isPresent()) {
            return ResponseEntity.ok(RestApiResponse.success(restore(replay.get(), responseType), null));
        }
        return (ResponseEntity<RestApiResponse<T>>) (ResponseEntity<?>) executeCollectionCommand(
                actionId, request, ResourceCommandResponsePolicy.RETURN_COMMAND_RESULT,
                command -> executeReserved(command, null, actionId, idempotencyKey, request,
                        correlation(correlationId), actor, operation));
    }

    private <T> ResourceCommandExecutionResult executeReserved(
            ResourceCommandExecutionRequest command,
            Object resourceId,
            String actionId,
            String idempotencyKey,
            Object request,
            String correlationId,
            String actor,
            Supplier<T> operation) {
        var execution = actionExecutionService.reserve(
                RESOURCE_KEY, resourceId, actionId,
                resourceId == null ? ActionScope.COLLECTION : ActionScope.ITEM,
                idempotencyKey, request, correlationId, actor);
        if (execution.isPresent() && "COMPLETED".equals(execution.get().getExecutionStatus())) {
            return ResourceCommandExecutionResult.success(command, resourceId,
                    execution.get().getResponsePayload(), Map.of("replayed", true));
        }
        try {
            T result = execution.isPresent() && !"evaluate-batch".equals(actionId)
                    ? transactionCoordinator.execute(execution.get(), operation)
                    : operation.get();
            if (execution.isPresent() && "evaluate-batch".equals(actionId)) {
                actionExecutionService.complete(execution.get(), result);
            }
            return ResourceCommandExecutionResult.success(command, resourceId, result,
                    Map.of("resourceKey", RESOURCE_KEY, "idempotent", true));
        } catch (ExtraordinaryGrantRuleSnapshotUnavailableException unavailable) {
            execution.ifPresent(value -> actionExecutionService.fail(value, unavailable));
            return ResourceCommandExecutionResult.failure(command, ResourceCommandOutcome.PRECONDITION_FAILED,
                    "No governed extraordinary-benefit snapshot is active and effective.", Map.of());
        } catch (DateTimeException invalidTimeZone) {
            execution.ifPresent(value -> actionExecutionService.fail(value, invalidTimeZone));
            return ResourceCommandExecutionResult.failure(command, ResourceCommandOutcome.VALIDATION_FAILED,
                    "The informed userTimeZone is not a valid IANA time zone.", Map.of());
        } catch (DataIntegrityViolationException conflict) {
            execution.ifPresent(value -> actionExecutionService.fail(value, conflict));
            return ResourceCommandExecutionResult.failure(command, ResourceCommandOutcome.CONFLICT_DUPLICATE,
                    "The command conflicts with a persisted request or effect.", Map.of());
        } catch (ObjectOptimisticLockingFailureException stale) {
            execution.ifPresent(value -> actionExecutionService.fail(value, stale));
            return ResourceCommandExecutionResult.failure(command, ResourceCommandOutcome.PRECONDITION_FAILED,
                    "The resource changed concurrently; reload it and retry with the new ETag.", Map.of());
        } catch (ResponseStatusException domainFailure) {
            execution.ifPresent(value -> actionExecutionService.fail(value, domainFailure));
            return ResourceCommandExecutionResult.failure(
                    command,
                    commandOutcome(domainFailure.getStatusCode().value()),
                    domainFailure.getReason() == null ? "The business command could not be completed." : domainFailure.getReason(),
                    Map.of());
        } catch (RuntimeException failure) {
            execution.ifPresent(value -> actionExecutionService.fail(value, failure));
            throw failure;
        }
    }

    private <T> T restore(ResourceActionExecution execution, Class<T> responseType) {
        try {
            return objectMapper.treeToValue(execution.getResponsePayload(), responseType);
        } catch (Exception invalidStoredResult) {
            throw new IllegalStateException("Unable to restore the idempotent command result.", invalidStoredResult);
        }
    }

    private void requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals);
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ROLE_ADMIN is required for this pilot action.");
        }
    }

    private String actorSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getName() == null ? "anonymous" : authentication.getName();
    }

    private Set<String> resolveActorPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return Set.of();
        Set<String> permissions = new LinkedHashSet<>();
        authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).forEach(permissions::add);
        if (permissions.contains("ROLE_ADMIN") || permissions.contains("BENEFIT_REQUEST")) {
            permissions.add("benefit:request");
        }
        return Set.copyOf(permissions);
    }

    private String correlation(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) return UUID.randomUUID().toString();
        String normalized = correlationId.trim();
        if (normalized.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Correlation-ID must have at most 255 characters.");
        }
        return normalized;
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must not be blank.");
        }
        if (idempotencyKey.trim().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must have at most 255 characters.");
        }
    }

    private ResourceCommandOutcome commandOutcome(int httpStatus) {
        return switch (httpStatus) {
            case 400 -> ResourceCommandOutcome.VALIDATION_FAILED;
            case 403 -> ResourceCommandOutcome.PERMISSION_DENIED;
            case 404 -> ResourceCommandOutcome.NOT_FOUND;
            case 409 -> ResourceCommandOutcome.CONFLICT_DEPENDENCY;
            case 412, 428 -> ResourceCommandOutcome.PRECONDITION_FAILED;
            default -> ResourceCommandOutcome.UNEXPECTED_SANITIZED;
        };
    }
}
