package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.CreateEventosFolhaDTO;
import com.example.praxis.apiquickstart.hr.dto.EventosFolhaResponseDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateEventosFolhaDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.BulkApproveEventosFolhaRequestDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.BulkApproveEventosFolhaResultDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.EventosFolhaFilterDTO;
import com.example.praxis.apiquickstart.hr.service.EventosFolhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.command.ResourceCommandExecutionResult;
import org.praxisplatform.uischema.command.ResourceCommandResponsePolicy;
import org.praxisplatform.uischema.controller.base.AbstractResourceController;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recurso de eventos de folha usado para demonstrar workflow action de coleção.
 *
 * <p>Diferentemente de um CRUD puramente administrativo, este controller mostra como a plataforma
 * representa um comando de negócio explícito associado ao fechamento da folha. O método
 * {@code bulk-approve} é publicado como {@code @WorkflowAction}, ganhando catálogo próprio,
 * schemas dedicados e descoberta via capabilities/actions.</p>
 */
@RestController
@ApiResource(value = ApiPaths.HumanResources.EVENTOS_FOLHA, resourceKey = "human-resources.eventos-folha")
@ApiGroup("human-resources")
public class EventosFolhaController extends AbstractResourceController<
        EventosFolhaResponseDTO,
        Integer,
        EventosFolhaFilterDTO,
        CreateEventosFolhaDTO,
        UpdateEventosFolhaDTO> {

    private final EventosFolhaService service;

    public EventosFolhaController(EventosFolhaService service) {
        this.service = service;
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
    @Operation(summary = "Aprovar eventos pendentes de folha em lote", description = "Executa a transição de aprovação para eventos de folha selecionados, retornando totais e resultado por item para apoiar fechamento de competência, conferência financeira e tratamento de falhas parciais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eventos aprovados em lote com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou seleção de eventos inconsistente."),
            @ApiResponse(responseCode = "409", description = "Um ou mais eventos não estão aptos para aprovação em lote.")
    })
    @WorkflowAction(
            id = "bulk-approve",
            title = "Aprovar eventos de folha em lote",
            description = "Aprova eventos pendentes selecionados para fechamento de folha, mantendo retorno individual por item processado.",
            scope = ActionScope.COLLECTION,
            order = 100,
            successMessage = "Eventos aprovados",
            allowedStates = {"PENDENTE"},
            tags = {"payroll", "payroll-events", "approval-workflow", "bulk-action"}
    )
    public ResponseEntity<RestApiResponse<BulkApproveEventosFolhaResultDTO>> bulkApprove(
            @Valid @RequestBody BulkApproveEventosFolhaRequestDTO dto
    ) {
        return governedBulkApprove(dto);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<RestApiResponse<BulkApproveEventosFolhaResultDTO>> governedBulkApprove(
            BulkApproveEventosFolhaRequestDTO dto
    ) {
        return (ResponseEntity<RestApiResponse<BulkApproveEventosFolhaResultDTO>>) (ResponseEntity<?>) executeCollectionCommand(
                "bulk-approve",
                dto,
                ResourceCommandResponsePolicy.RETURN_COMMAND_RESULT,
                request -> ResourceCommandExecutionResult.success(
                        request,
                        null,
                        service.bulkApprove(dto),
                        java.util.Map.of("resourceKey", "human-resources.eventos-folha")
                )
        );
    }
}



