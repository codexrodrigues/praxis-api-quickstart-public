package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.FeriasAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateFeriasAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFeriasAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.AbsenceCoverageWorkflowRequestDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.AbsenceCoverageWorkflowResultDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.FeriasAfastamentoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.FeriasAfastamento;
import com.example.praxis.apiquickstart.hr.mapper.FeriasAfastamentoMapper;
import com.example.praxis.apiquickstart.hr.service.FeriasAfastamentoService;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.command.ResourceCommandExecutionResult;
import org.praxisplatform.uischema.command.ResourceCommandResponsePolicy;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@RestController
@ApiResource(value = ApiPaths.HumanResources.FERIAS_AFASTAMENTOS, resourceKey = "human-resources.ferias-afastamentos")
@ApiGroup("human-resources")
public class FeriasAfastamentoController extends AbstractQuickstartCrudController<FeriasAfastamento, FeriasAfastamentoDTO, Integer, FeriasAfastamentoFilterDTO, CreateFeriasAfastamentoDTO, UpdateFeriasAfastamentoDTO> {

    private final FeriasAfastamentoService service;
    private final FeriasAfastamentoMapper mapper;

    @Autowired
    public FeriasAfastamentoController(FeriasAfastamentoService service, FeriasAfastamentoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected FeriasAfastamentoService getService() { return service; }

    @Override
    protected FeriasAfastamentoDTO toDto(FeriasAfastamento entity) { return mapper.toDto(entity); }

    @Override
    protected FeriasAfastamento toEntity(FeriasAfastamentoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(FeriasAfastamento entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(FeriasAfastamentoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @UiSurface(
            id = "absence-calendar-board",
            kind = SurfaceKind.VIEW,
            scope = SurfaceScope.COLLECTION,
            title = "Calendario de disponibilidade",
            description = "Mostra ferias, licencas e afastamentos por colaborador, tipo e periodo para planejar cobertura operacional, impacto em missoes e calendario de folha.",
            intent = "hr-absence-availability-calendar",
            order = 30,
            tags = {"human-resources", "absence", "availability", "calendar", "capacity-planning", "privacy"}
    )
    @Operation(summary = "Filtrar férias e afastamentos por funcionário, tipo e período", description = "Lista períodos de ausência por colaborador, natureza do afastamento, data de início, data de fim e observações para planejamento de capacidade, calendário de RH e acompanhamento de disponibilidade operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<FeriasAfastamentoDTO>>>> filter(@RequestBody FeriasAfastamentoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer férias e afastamentos em listas extensas", description = "Navega por períodos de ausência usando cursor, preservando filtros por funcionário, tipo e janela de datas em calendários de RH, tabelas operacionais e painéis de disponibilidade.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<FeriasAfastamentoDTO>>>> filterByCursor(@RequestBody FeriasAfastamentoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar ausência dentro de um recorte de disponibilidade", description = "Informa a posição de férias ou afastamento em uma lista filtrada por funcionário, tipo ou período para retomada de navegação em calendários, relatórios e atendimentos de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody FeriasAfastamentoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar cadastro completo de férias e afastamentos", description = "Retorna todos os períodos de ausência cadastrados como referência de disponibilidade de funcionários para conferência administrativa, exportação, sincronização e composição de calendários internos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<FeriasAfastamentoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar férias e afastamentos por identificadores conhecidos", description = "Recupera períodos de ausência já referenciados em formulários, calendários, filtros salvos ou seleções anteriores usando seus identificadores cadastrais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<FeriasAfastamentoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar férias e afastamentos para fluxos de RH", description = "Produz opções compactas de períodos de ausência para campos de seleção, busca, filtros de disponibilidade e vínculos administrativos orientados por busca.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody FeriasAfastamentoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de férias e afastamentos já selecionadas", description = "Reidrata opções de ausências escolhidas em formulários, calendários, filtros salvos ou painéis de RH, preservando identificador e rótulo de exibição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de férias ou afastamento", description = "Retorna o período de ausência com funcionário, tipo, datas e observações para inspeção administrativa, análise de disponibilidade e composição de visões de calendário de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<FeriasAfastamentoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar férias ou afastamento", description = "Cria um período de ausência vinculado ao funcionário com tipo, data de início, data de fim e observações para planejamento de capacidade e gestão operacional de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<FeriasAfastamentoDTO>> create(@jakarta.validation.Valid @RequestBody CreateFeriasAfastamentoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar período de férias ou afastamento", description = "Mantém tipo, datas e observações de uma ausência vinculada ao funcionário para calendário de RH, planejamento de capacidade e acompanhamento de disponibilidade.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<FeriasAfastamentoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateFeriasAfastamentoDTO dto) {
        return super.update(id, dto);
    }

    @PostMapping("/{id}/actions/plan-coverage")
    @Operation(summary = "Planejar cobertura de férias ou afastamento", description = "Registra a decisão operacional de cobertura para uma ausência, vinculando plano, substituto opcional e justificativa sem alterar o contrato estrutural do cadastro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cobertura registrada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou plano de cobertura ausente."),
            @ApiResponse(responseCode = "404", description = "Período de ausência não encontrado."),
            @ApiResponse(responseCode = "409", description = "A decisão não pôde ser registrada no estado atual do registro.")
    })
    @WorkflowAction(
            id = "plan-coverage",
            title = "Planejar cobertura",
            description = "Registra como uma ausencia sera coberta operacionalmente, com substituto opcional e justificativa auditavel.",
            scope = ActionScope.ITEM,
            order = 100,
            successMessage = "Cobertura registrada",
            tags = {"human-resources", "absence", "availability", "coverage-planning", "item-action"}
    )
    public ResponseEntity<RestApiResponse<AbsenceCoverageWorkflowResultDTO>> planCoverage(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody AbsenceCoverageWorkflowRequestDTO dto
    ) {
        return executePlanCoverageCommand(id, dto);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<RestApiResponse<AbsenceCoverageWorkflowResultDTO>> executePlanCoverageCommand(
            Integer id,
            AbsenceCoverageWorkflowRequestDTO dto
    ) {
        return (ResponseEntity<RestApiResponse<AbsenceCoverageWorkflowResultDTO>>) (ResponseEntity<?>) executeItemCommand(
                "plan-coverage",
                id,
                dto,
                ResourceCommandResponsePolicy.RETURN_COMMAND_RESULT,
                request -> ResourceCommandExecutionResult.success(
                        request,
                        id,
                        service.planCoverage(id, dto),
                        Map.of("resourceKey", "human-resources.ferias-afastamentos")
                )
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover férias ou afastamento do cadastro", description = "Remove um período de ausência do cadastro administrativo quando a agenda de RH exige saneamento, revisão de dados ou retirada de registros obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover férias e afastamentos em lote", description = "Remove múltiplos períodos de ausência em uma única chamada para saneamento administrativo, revisão de calendário ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}
