package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import com.example.praxis.apiquickstart.hr.dto.CreateFolhasPagamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.FolhasPagamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.ScheduleFolhaPagamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFolhasPagamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.FolhaPagamentoWorkflowRequestDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.FolhaPagamentoWorkflowResultDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.FolhasPagamentoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.FolhasPagamento;
import com.example.praxis.apiquickstart.hr.mapper.FolhasPagamentoMapper;
import com.example.praxis.apiquickstart.hr.service.FolhasPagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.ResourceIntent;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.command.ResourceCommandExecutionResult;
import org.praxisplatform.uischema.command.ResourceCommandResponsePolicy;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Links;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Recurso de folhas de pagamento usado para demonstrar surfaces item-based e workflow de item.
 *
 * <p>Ele combina, no mesmo recurso, três tipos de experiência importantes para a plataforma:
 * CRUD completo, surface parcial para reagendamento e workflow actions para avançar o ciclo
 * operacional da folha.</p>
 */
@RestController
@ApiResource(value = ApiPaths.HumanResources.FOLHAS_PAGAMENTO, resourceKey = "human-resources.folhas-pagamento")
@ApiGroup("human-resources")
public class FolhasPagamentoController extends AbstractQuickstartCrudController<FolhasPagamento, FolhasPagamentoDTO, Integer, FolhasPagamentoFilterDTO, CreateFolhasPagamentoDTO, UpdateFolhasPagamentoDTO> {

    private final FolhasPagamentoService service;
    private final FolhasPagamentoMapper mapper;

    @Autowired
    public FolhasPagamentoController(FolhasPagamentoService service, FolhasPagamentoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected FolhasPagamentoService getService() { return service; }

    @Override
    protected FolhasPagamentoDTO toDto(FolhasPagamento entity) { return mapper.toDto(entity); }

    @Override
    protected FolhasPagamento toEntity(FolhasPagamentoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(FolhasPagamento entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(FolhasPagamentoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar folhas por competência, colaborador e valores financeiros", description = "Lista folhas de pagamento por funcionário, ano, mês, data de pagamento, salário bruto, descontos e salário líquido para conferência mensal, fechamento financeiro e acompanhamento operacional da folha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<FolhasPagamentoDTO>>>> filter(@RequestBody FolhasPagamentoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer folhas de pagamento em grandes volumes", description = "Navega por folhas usando cursor, preservando filtros de competência, colaborador, data de pagamento e faixas financeiras em rotinas mensais, tabelas de fechamento e consultas operacionais extensas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<FolhasPagamentoDTO>>>> filterByCursor(@RequestBody FolhasPagamentoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar folha dentro de um recorte financeiro", description = "Informa a posição de uma folha em lista filtrada por competência, colaborador, pagamento ou valores para retomada de navegação em tabelas de fechamento, conciliação e auditoria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody FolhasPagamentoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar cadastro completo de folhas de pagamento", description = "Retorna todas as folhas cadastradas como referência mensal de remuneração por funcionário para conferência financeira, exportação, sincronização e composição de painéis internos de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<FolhasPagamentoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar folhas por identificadores conhecidos", description = "Recupera folhas já referenciadas em eventos, análises, filtros salvos ou seleções anteriores usando seus identificadores do cadastro financeiro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<FolhasPagamentoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar folhas para eventos e rotinas de pagamento", description = "Produz opções compactas de folhas para campos de seleção, busca, vínculos com eventos de folha, ajustes financeiros e rotinas de pagamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody FolhasPagamentoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de folhas já selecionadas", description = "Reidrata opções de folhas escolhidas em formulários, eventos, filtros salvos ou painéis, preservando identificador e rótulo de competência.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de folha de pagamento", description = "Retorna a folha mensal de um funcionário com competência, valores consolidados, descontos, salário líquido e data de pagamento para inspeção financeira, conciliação e composição de visões de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<FolhasPagamentoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar folha mensal de pagamento", description = "Cria uma folha de pagamento vinculada a um funcionário com ano, mês, valores financeiros consolidados e data de pagamento para processamento operacional de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<FolhasPagamentoDTO>> create(@jakarta.validation.Valid @RequestBody CreateFolhasPagamentoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar folha mensal de pagamento", description = "Mantém competência, valores consolidados, descontos, salário líquido, data de pagamento e vínculo com funcionário para processamento, conciliação e análise da folha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<FolhasPagamentoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateFolhasPagamentoDTO dto) {
        return super.update(id, dto);
    }

    @PatchMapping("/{id}/payment-schedule")
    @UiSurface(
            id = "payment-schedule",
            kind = SurfaceKind.PARTIAL_FORM,
            scope = SurfaceScope.ITEM,
            title = "Ajustar agenda de pagamento",
            description = "Reagenda a data operacional de pagamento da folha para alinhamento com tesouraria, fechamento financeiro e comunicação interna.",
            intent = "payroll-scheduling",
            order = 30,
            allowedStates = {"AGUARDANDO_EVENTOS", "PROGRAMADA"},
            tags = {"payroll", "payment-schedule", "partial-update", "treasury"}
    )
    @ResourceIntent(
            id = "payroll-scheduling",
            title = "Ajustar agenda da folha",
            description = "Mantém a data operacional de pagamento usada por tesouraria, conciliação e fechamento da folha.",
            order = 30
    )
    @Operation(summary = "Ajustar agenda de pagamento da folha", description = "Atualiza a data prevista de pagamento da folha para alinhamento com tesouraria, fechamento financeiro, conferência mensal e auditoria operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agenda de pagamento atualizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Data de pagamento inválida ou incoerente com a requisição."),
            @ApiResponse(responseCode = "404", description = "Folha de pagamento não encontrada."),
            @ApiResponse(responseCode = "409", description = "O estado atual da folha não permite reagendamento.")
    })
    public ResponseEntity<RestApiResponse<FolhasPagamentoDTO>> schedulePayment(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody ScheduleFolhaPagamentoDTO dto
    ) {
        FolhasPagamentoDTO updated = service.schedulePayment(id, dto);
        // Mantém a surface parcial autodocumentada via schemaUrl do próprio host.
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToFilterCursor(),
                linkToUiSchema("/{id}/payment-schedule", "patch", "request")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(updated, hateoasOrNull(links)));
    }

    @PostMapping("/{id}/actions/approve-events")
    @Operation(summary = "Aprovar eventos pendentes da folha", description = "Consolida eventos pendentes vinculados à folha, registra a transição de workflow e libera a próxima etapa do fechamento financeiro da competência.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eventos pendentes aprovados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes para a aprovação."),
            @ApiResponse(responseCode = "404", description = "Folha de pagamento não encontrada."),
            @ApiResponse(responseCode = "409", description = "O estado atual da folha não permite aprovar eventos.")
    })
    @WorkflowAction(
            id = "approve-events",
            title = "Aprovar eventos da folha",
            description = "Aprova eventos pendentes associados à folha e registra a transição operacional para continuidade do fechamento financeiro.",
            scope = ActionScope.ITEM,
            order = 100,
            successMessage = "Eventos pendentes aprovados",
            allowedStates = {"AGUARDANDO_EVENTOS"},
            tags = {"payroll", "payroll-events", "approval-workflow", "item-action"}
    )
    public ResponseEntity<RestApiResponse<FolhaPagamentoWorkflowResultDTO>> approveEvents(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody FolhaPagamentoWorkflowRequestDTO dto
    ) {
        return governedApproveEvents(id, dto);
    }

    @PostMapping("/{id}/actions/mark-paid")
    @Operation(summary = "Marcar folha como paga", description = "Confirma a liquidação da folha programada, registra a transição de workflow e encerra o ciclo operacional de pagamento da competência.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Folha marcada como paga com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes para a quitação."),
            @ApiResponse(responseCode = "404", description = "Folha de pagamento não encontrada."),
            @ApiResponse(responseCode = "409", description = "O estado atual da folha não permite marcar o pagamento.")
    })
    @WorkflowAction(
            id = "mark-paid",
            title = "Marcar como paga",
            description = "Confirma a liquidação de uma folha programada e conclui o fluxo operacional de quitação.",
            scope = ActionScope.ITEM,
            order = 110,
            successMessage = "Folha marcada como paga",
            allowedStates = {"PROGRAMADA"},
            tags = {"payroll", "payment-settlement", "workflow", "item-action"}
    )
    public ResponseEntity<RestApiResponse<FolhaPagamentoWorkflowResultDTO>> markPaid(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody FolhaPagamentoWorkflowRequestDTO dto
    ) {
        return governedMarkPaid(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover folha de pagamento", description = "Remove uma folha do cadastro financeiro quando a base mensal exige saneamento, revisão operacional ou retirada de registros obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover folhas de pagamento em lote", description = "Remove múltiplas folhas em uma única chamada para saneamento administrativo, revisão financeira ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<RestApiResponse<FolhaPagamentoWorkflowResultDTO>> governedApproveEvents(
            Integer id,
            FolhaPagamentoWorkflowRequestDTO dto
    ) {
        return (ResponseEntity<RestApiResponse<FolhaPagamentoWorkflowResultDTO>>) (ResponseEntity<?>) executeItemCommand(
                "approve-events",
                id,
                dto,
                ResourceCommandResponsePolicy.RETURN_COMMAND_RESULT,
                request -> ResourceCommandExecutionResult.success(
                        request,
                        id,
                        service.approvePendingEvents(id, dto),
                        java.util.Map.of("resourceKey", "human-resources.folhas-pagamento")
                )
        );
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<RestApiResponse<FolhaPagamentoWorkflowResultDTO>> governedMarkPaid(
            Integer id,
            FolhaPagamentoWorkflowRequestDTO dto
    ) {
        return (ResponseEntity<RestApiResponse<FolhaPagamentoWorkflowResultDTO>>) (ResponseEntity<?>) executeItemCommand(
                "mark-paid",
                id,
                dto,
                ResourceCommandResponsePolicy.RETURN_COMMAND_RESULT,
                request -> ResourceCommandExecutionResult.success(
                        request,
                        id,
                        service.markAsPaid(id, dto),
                        java.util.Map.of("resourceKey", "human-resources.folhas-pagamento")
                )
        );
    }
}
