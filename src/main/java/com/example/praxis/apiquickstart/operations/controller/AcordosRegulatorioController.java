package com.example.praxis.apiquickstart.operations.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.entity.ResourceActionExecution;
import com.example.praxis.apiquickstart.core.service.ResourceActionExecutionService;
import com.example.praxis.apiquickstart.core.service.ResourceActionTransactionCoordinator;
import com.example.praxis.apiquickstart.operations.dto.AcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateAcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.dto.ReviewAcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateAcordosRegulatorioDTO;
import com.example.praxis.apiquickstart.operations.dto.actions.AcordoRegulatorioWorkflowRequestDTO;
import com.example.praxis.apiquickstart.operations.dto.actions.AcordoRegulatorioWorkflowResultDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.AcordosRegulatorioFilterDTO;
import com.example.praxis.apiquickstart.operations.service.AcordosRegulatorioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.ResourceIntent;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.action.ActionInteractionMode;
import org.praxisplatform.uischema.action.ActionRequirement;
import org.praxisplatform.uischema.action.ActionResourceVersionTransport;
import org.praxisplatform.uischema.action.ActionRiskLevel;
import org.praxisplatform.uischema.command.ResourceCommandExecutionRequest;
import org.praxisplatform.uischema.command.ResourceCommandExecutionResult;
import org.praxisplatform.uischema.command.ResourceCommandResponsePolicy;
import org.praxisplatform.uischema.concurrency.ResourceVersionPreconditions;
import org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition;
import org.praxisplatform.uischema.controller.base.AbstractCreateUpdateResourceController;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Links;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import java.util.List;
import java.util.UUID;

/**
 * Recurso de acordos regulatorios usado para demonstrar workflow e review de compliance.
 *
 * <p>Ele e um bom exemplo de recurso cujo valor nao esta apenas no CRUD: o quickstart usa este
 * controller para mostrar como surfaces parciais e actions de item podem publicar o ciclo de vida
 * de um artefato regulatorio de forma semantica e descobrivel.</p>
 */
@ApiResource(value = ApiPaths.Operations.ACORDOS_REGULATORIOS, resourceKey = "operations.acordos-regulatorios")
@ApiGroup("operations")
public class AcordosRegulatorioController extends AbstractCreateUpdateResourceController<AcordosRegulatorioDTO, Integer, AcordosRegulatorioFilterDTO, CreateAcordosRegulatorioDTO, UpdateAcordosRegulatorioDTO> {
    private static final String RESOURCE_KEY = "operations.acordos-regulatorios";

    private final AcordosRegulatorioService service;
    private final ResourceActionExecutionService actionExecutionService;
    private final ResourceActionTransactionCoordinator transactionCoordinator;
    private final ObjectMapper objectMapper;

    @Autowired
    public AcordosRegulatorioController(
            AcordosRegulatorioService service,
            ResourceActionExecutionService actionExecutionService,
            ResourceActionTransactionCoordinator transactionCoordinator,
            ObjectMapper objectMapper
    ) {
        this.service = service;
        this.actionExecutionService = actionExecutionService;
        this.transactionCoordinator = transactionCoordinator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected AcordosRegulatorioService getService() { return service; }

    @Override
    protected Integer getResponseId(AcordosRegulatorioDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar acordos regulatórios de operação", description = "Lista acordos por jurisdição, status, vigência e vínculo regulatório para identificar quais regras condicionam missões, licenças e atividades operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<AcordosRegulatorioDTO>>>> filter(@RequestBody AcordosRegulatorioFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar acordos regulatórios de operação com paginação por cursor", description = "Percorre acordos regulatórios em catálogos extensos usando cursor, útil para compliance operacional, auditoria contínua e telas com rolagem incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<AcordosRegulatorioDTO>>>> filterByCursor(@RequestBody AcordosRegulatorioFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar acordo regulatório de operação em listas filtradas", description = "Informa em qual posição um acordo aparece dentro do recorte filtrado, útil para retornar ao registro em tabelas de compliance e governança operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody AcordosRegulatorioFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar acordos regulatórios de operação", description = "Retorna o cadastro completo de acordos quando o consumidor precisa materializar todas as regras aplicáveis para conferência, exportação ou sincronização.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<AcordosRegulatorioDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar acordos regulatórios de operação por IDs", description = "Recupera acordos já vinculados a licenças, operações ou seleções anteriores sem reaplicar filtros de compliance.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<AcordosRegulatorioDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar acordos regulatórios de operação para formulários", description = "Produz opções compactas de acordos para campos de seleção, autocomplete e vínculos regulatórios orientados por busca.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody AcordosRegulatorioFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar acordos regulatórios de operação selecionados", description = "Reidrata opções de acordos já escolhidos em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter acordo regulatório de operação", description = "Retorna o detalhe de um acordo para inspeção de compliance, auditoria documental ou composição de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<AcordosRegulatorioDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar acordo regulatório de operação", description = "Cadastra uma nova regra regulatória com jurisdição, vigência e status para governar licenças e fluxos operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<AcordosRegulatorioDTO>> create(@jakarta.validation.Valid @RequestBody CreateAcordosRegulatorioDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar acordo regulatório de operação", description = "Atualiza metadados e condições de um acordo existente sem alterar sua identidade no catálogo regulatório.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<AcordosRegulatorioDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateAcordosRegulatorioDTO dto) {
        return super.update(id, dto);
    }

    @PatchMapping("/{id}/review")
    @UiSurface(
            id = "review",
            kind = SurfaceKind.PARTIAL_FORM,
            scope = SurfaceScope.ITEM,
            title = "Revisar metadados regulatórios",
            description = "Atualiza jurisdição e descrição sem alterar o ciclo de vida do acordo",
            intent = "compliance-review",
            order = 30,
            allowedStates = {"VIGENTE", "SUSPENSO"},
            tags = {"review", "compliance"}
    )
    @ResourceIntent(
            id = "regulatory-review",
            title = "Revisar acordo regulatório",
            description = "Atualiza apenas os metadados regulatórios operacionais",
            order = 30
    )
    @Operation(summary = "Revisar metadados do acordo regulatório", description = "Atualiza jurisdição, descrição e metadados documentais do acordo sem alterar seu estado de ciclo de vida.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metadados regulatórios revisados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Estado atual não permite revisão."),
            @ApiResponse(responseCode = "412", description = "ETag nao corresponde a versao persistida."),
            @ApiResponse(responseCode = "428", description = "If-Match nao informado.")
    })
    public ResponseEntity<RestApiResponse<AcordosRegulatorioDTO>> review(
            @PathVariable Integer id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @jakarta.validation.Valid @RequestBody ReviewAcordosRegulatorioDTO dto
    ) {
        AcordosRegulatorioDTO reviewed = service.review(
                id,
                dto,
                resourceVersionUpdatePrecondition(id, ifMatch)
        );
        // Mantem o contrato da surface parcial redescobrivel via schema publicado.
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToFilterCursor(),
                linkToUiSchema("/{id}/review", "patch", "request")
        );
        return withResourceVersion(
                ResponseEntity.ok(),
                id,
                RestApiResponse.success(reviewed, hateoasOrNull(links))
        );
    }

    @PostMapping("/{id}/actions/suspend")
    @Operation(summary = "Suspender acordo regulatório", description = "Suspende o acordo e publica a transição de estado para catálogos de workflow e capabilities.")
    @WorkflowAction(
            id = "suspend",
            title = "Suspender acordo",
            description = "Suspende temporariamente o acordo regulatório",
            scope = ActionScope.ITEM,
            order = 100,
            successMessage = "Acordo suspenso",
            allowedStates = {"VIGENTE"},
            interactionMode = ActionInteractionMode.FORM,
            riskLevel = ActionRiskLevel.HIGH,
            confirmationRequired = true,
            reversible = true,
            idempotencyKey = ActionRequirement.REQUIRED,
            correlationId = ActionRequirement.OPTIONAL,
            resourceVersion = ActionRequirement.REQUIRED,
            resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH,
            resourceVersionField = "resourceVersion",
            refreshItem = true,
            tags = {"workflow", "status"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acordo suspenso ou replay idempotente retornado."),
            @ApiResponse(responseCode = "400", description = "Command ou Idempotency-Key invalido."),
            @ApiResponse(responseCode = "409", description = "Estado ou chave idempotente conflitante."),
            @ApiResponse(responseCode = "412", description = "ETag nao corresponde a versao persistida."),
            @ApiResponse(responseCode = "428", description = "If-Match nao informado.")
    })
    public ResponseEntity<RestApiResponse<AcordoRegulatorioWorkflowResultDTO>> suspend(
            @PathVariable Integer id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @jakarta.validation.Valid @RequestBody AcordoRegulatorioWorkflowRequestDTO dto
    ) {
        return executeWorkflowAction("suspend", id, ifMatch, idempotencyKey, correlationId, dto);
    }

    @PostMapping("/{id}/actions/reinstate")
    @Operation(summary = "Reativar acordo regulatório", description = "Reativa um acordo suspenso e republica sua disponibilidade para ações e surfaces dependentes.")
    @WorkflowAction(
            id = "reinstate",
            title = "Reativar acordo",
            description = "Reativa um acordo regulatório suspenso",
            scope = ActionScope.ITEM,
            order = 110,
            successMessage = "Acordo reativado",
            allowedStates = {"SUSPENSO"},
            interactionMode = ActionInteractionMode.FORM,
            riskLevel = ActionRiskLevel.HIGH,
            confirmationRequired = true,
            reversible = true,
            idempotencyKey = ActionRequirement.REQUIRED,
            correlationId = ActionRequirement.OPTIONAL,
            resourceVersion = ActionRequirement.REQUIRED,
            resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH,
            resourceVersionField = "resourceVersion",
            refreshItem = true,
            tags = {"workflow", "status"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acordo reativado ou replay idempotente retornado."),
            @ApiResponse(responseCode = "400", description = "Command ou Idempotency-Key invalido."),
            @ApiResponse(responseCode = "409", description = "Estado ou chave idempotente conflitante."),
            @ApiResponse(responseCode = "412", description = "ETag nao corresponde a versao persistida."),
            @ApiResponse(responseCode = "428", description = "If-Match nao informado.")
    })
    public ResponseEntity<RestApiResponse<AcordoRegulatorioWorkflowResultDTO>> reinstate(
            @PathVariable Integer id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @jakarta.validation.Valid @RequestBody AcordoRegulatorioWorkflowRequestDTO dto
    ) {
        return executeWorkflowAction("reinstate", id, ifMatch, idempotencyKey, correlationId, dto);
    }

    @PostMapping("/{id}/actions/revoke")
    @Operation(summary = "Revogar acordo regulatório", description = "Revoga definitivamente o acordo e encerra sua elegibilidade operacional nos catálogos do recurso.")
    @WorkflowAction(
            id = "revoke",
            title = "Revogar acordo",
            description = "Revoga definitivamente o acordo regulatório",
            scope = ActionScope.ITEM,
            order = 120,
            successMessage = "Acordo revogado",
            allowedStates = {"VIGENTE", "SUSPENSO"},
            interactionMode = ActionInteractionMode.FORM,
            riskLevel = ActionRiskLevel.CRITICAL,
            confirmationRequired = true,
            idempotencyKey = ActionRequirement.REQUIRED,
            correlationId = ActionRequirement.OPTIONAL,
            resourceVersion = ActionRequirement.REQUIRED,
            resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH,
            resourceVersionField = "resourceVersion",
            refreshItem = true,
            tags = {"workflow", "status"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acordo revogado ou replay idempotente retornado."),
            @ApiResponse(responseCode = "400", description = "Command ou Idempotency-Key invalido."),
            @ApiResponse(responseCode = "409", description = "Estado ou chave idempotente conflitante."),
            @ApiResponse(responseCode = "412", description = "ETag nao corresponde a versao persistida."),
            @ApiResponse(responseCode = "428", description = "If-Match nao informado.")
    })
    public ResponseEntity<RestApiResponse<AcordoRegulatorioWorkflowResultDTO>> revoke(
            @PathVariable Integer id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @jakarta.validation.Valid @RequestBody AcordoRegulatorioWorkflowRequestDTO dto
    ) {
        return executeWorkflowAction("revoke", id, ifMatch, idempotencyKey, correlationId, dto);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<RestApiResponse<AcordoRegulatorioWorkflowResultDTO>> executeWorkflowAction(
            String actionId,
            Integer id,
            String ifMatch,
            String idempotencyKey,
            String correlationId,
            AcordoRegulatorioWorkflowRequestDTO dto
    ) {
        validateIdempotencyKey(idempotencyKey);
        ResourceVersionPreconditions.requireStrongEtag(ifMatch);

        var replay = actionExecutionService.findCompletedReplay(
                RESOURCE_KEY, id, actionId, idempotencyKey, dto);
        if (replay.isPresent()) {
            return workflowSuccess(id, actionId, restoreWorkflowResult(replay.get()));
        }

        // Rejeita cedo sem consumir a chave; a precondicao e validada novamente na transacao.
        requireMatchingResourceVersion(id, ifMatch);
        ResourceVersionUpdatePrecondition<Integer> precondition = resourceVersionUpdatePrecondition(id, ifMatch);
        String actor = SecurityContextHolder.getContext().getAuthentication() == null
                ? "anonymous"
                : SecurityContextHolder.getContext().getAuthentication().getName();
        String correlation = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId.trim();

        ResponseEntity<?> governedResponse = executeItemCommand(
                actionId,
                id,
                dto,
                ResourceCommandResponsePolicy.RETURN_COMMAND_RESULT,
                request -> executeNewWorkflowAction(
                        request, actionId, id, idempotencyKey, dto, correlation, actor, precondition)
        );
        if (!governedResponse.getStatusCode().is2xxSuccessful()) {
            return (ResponseEntity<RestApiResponse<AcordoRegulatorioWorkflowResultDTO>>) (ResponseEntity<?>) governedResponse;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(governedResponse.getStatusCode());
        governedResponse.getHeaders().forEach(
                (name, values) -> values.forEach(value -> responseBuilder.header(name, value))
        );
        return withResourceVersion(
                responseBuilder,
                id,
                (RestApiResponse<AcordoRegulatorioWorkflowResultDTO>) governedResponse.getBody()
        );
    }

    private ResourceCommandExecutionResult executeNewWorkflowAction(
            ResourceCommandExecutionRequest request,
            String actionId,
            Integer id,
            String idempotencyKey,
            AcordoRegulatorioWorkflowRequestDTO dto,
            String correlation,
            String actor,
            ResourceVersionUpdatePrecondition<Integer> precondition
    ) {
        var execution = actionExecutionService.reserve(
                RESOURCE_KEY, id, actionId, ActionScope.ITEM, idempotencyKey, dto, correlation, actor);
        if (execution.isPresent() && "COMPLETED".equals(execution.get().getExecutionStatus())) {
            return ResourceCommandExecutionResult.success(
                    request,
                    id,
                    restoreWorkflowResult(execution.get()),
                    java.util.Map.of("resourceKey", RESOURCE_KEY)
            );
        }

        try {
            AcordoRegulatorioWorkflowResultDTO result = transactionCoordinator.execute(
                    execution.orElseThrow(),
                    () -> switch (actionId) {
                        case "suspend" -> service.suspend(id, dto, precondition);
                        case "reinstate" -> service.reinstate(id, dto, precondition);
                        case "revoke" -> service.revoke(id, dto, precondition);
                        default -> throw new IllegalArgumentException(
                                "Unsupported agreement workflow action: " + actionId);
                    }
            );
            return ResourceCommandExecutionResult.success(
                    request, id, result, java.util.Map.of("resourceKey", RESOURCE_KEY));
        } catch (RuntimeException failure) {
            execution.ifPresent(value -> actionExecutionService.fail(
                    value,
                    "AGREEMENT_WORKFLOW_EXECUTION_FAILED",
                    "The regulatory agreement workflow command failed."
            ));
            throw failure;
        }
    }

    private ResponseEntity<RestApiResponse<AcordoRegulatorioWorkflowResultDTO>> workflowSuccess(
            Integer id,
            String actionId,
            AcordoRegulatorioWorkflowResultDTO result
    ) {
        String operationPath = "/{id}/actions/" + actionId;
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToFilterCursor(),
                linkToUiSchema(operationPath, "post", "request"),
                linkToUiSchema(operationPath, "post", "response")
        );
        return withResourceVersion(
                ResponseEntity.ok(),
                id,
                RestApiResponse.success(result, hateoasOrNull(links))
        );
    }

    private AcordoRegulatorioWorkflowResultDTO restoreWorkflowResult(ResourceActionExecution execution) {
        try {
            return objectMapper.treeToValue(
                    execution.getResponsePayload(), AcordoRegulatorioWorkflowResultDTO.class);
        } catch (Exception invalidStoredResult) {
            throw new IllegalStateException(
                    "Unable to restore the idempotent regulatory agreement workflow result.",
                    invalidStoredResult
            );
        }
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must not be blank."
            );
        }
        if (idempotencyKey.trim().length() > 255) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must have at most 255 characters."
            );
        }
    }
}



