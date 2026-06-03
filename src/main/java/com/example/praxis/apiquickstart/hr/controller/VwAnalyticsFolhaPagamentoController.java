package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.VwAnalyticsFolhaPagamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.VwAnalyticsFolhaPagamentoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.VwAnalyticsFolhaPagamento;
import com.example.praxis.apiquickstart.hr.mapper.VwAnalyticsFolhaPagamentoMapper;
import com.example.praxis.apiquickstart.hr.service.VwAnalyticsFolhaPagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.annotation.AnalyticsDimensionBinding;
import org.praxisplatform.uischema.annotation.AnalyticsGranularity;
import org.praxisplatform.uischema.annotation.AnalyticsIntent;
import org.praxisplatform.uischema.annotation.AnalyticsMetricBinding;
import org.praxisplatform.uischema.annotation.AnalyticsOperation;
import org.praxisplatform.uischema.annotation.AnalyticsPresentationFamily;
import org.praxisplatform.uischema.annotation.AnalyticsProjection;
import org.praxisplatform.uischema.annotation.AnalyticsSort;
import org.praxisplatform.uischema.annotation.AnalyticsSortDirection;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.UiAnalytics;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartReadOnlyController;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import org.praxisplatform.uischema.stats.dto.TimeSeriesStatsRequest;
import org.praxisplatform.uischema.stats.dto.TimeSeriesStatsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Recurso read-only de analytics da folha usado para demonstrar views derivadas na plataforma.
 *
 * <p>Este controller mostra que uma view consultiva também participa plenamente do contrato
 * metadata-driven: ela publica filter, cursor, locate, options, detail, stats e capabilities, mas
 * preserva a semântica read-only da projeção analítica.</p>
 */
@ApiResource(value = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO, resourceKey = "human-resources.vw-analytics-folha-pagamento")
@ApiGroup("human-resources")
public class VwAnalyticsFolhaPagamentoController extends AbstractQuickstartReadOnlyController<VwAnalyticsFolhaPagamento, VwAnalyticsFolhaPagamentoDTO, Integer, VwAnalyticsFolhaPagamentoFilterDTO> {

    private static final String GROUP_BY_STATS_REQUEST_EXAMPLE = """
            {
              "filter": {},
              "field": "universo",
              "metrics": [
                { "metric": "sum", "field": "salarioLiquido", "alias": "salarioLiquido" }
              ],
              "limit": 10
            }
            """;
    private static final String TIME_SERIES_STATS_REQUEST_EXAMPLE = """
            {
              "filter": {},
              "field": "competencia",
              "granularity": "month",
              "metrics": [
                { "metric": "sum", "field": "salarioLiquido", "alias": "salarioLiquido" }
              ]
            }
            """;
    private static final String TIME_SERIES_STATS_RESPONSE_EXAMPLE = """
            {
              "success": true,
              "data": {
                "field": "competencia",
                "granularity": "month",
                "metrics": [
                  { "field": "salarioLiquido", "aggregation": "sum", "alias": "salarioLiquido" }
                ],
                "points": [
                  { "label": "2026-01", "values": { "salarioLiquido": 94250.35 } }
                ]
              }
            }
            """;
    private static final String GROUP_BY_STATS_RESPONSE_EXAMPLE = """
            {
              "success": true,
              "data": {
                "field": "universo",
                "metrics": [
                  { "field": "salarioLiquido", "aggregation": "sum", "alias": "salarioLiquido" }
                ],
                "buckets": [
                  { "key": "OPERACOES", "label": "OPERACOES", "values": { "salarioLiquido": 125000.00 } }
                ]
              }
            }
            """;

    private final VwAnalyticsFolhaPagamentoService service;
    private final VwAnalyticsFolhaPagamentoMapper mapper;

    @Autowired
    public VwAnalyticsFolhaPagamentoController(VwAnalyticsFolhaPagamentoService service, VwAnalyticsFolhaPagamentoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected VwAnalyticsFolhaPagamentoService getService() { return service; }

    @Override
    protected VwAnalyticsFolhaPagamentoDTO toDto(VwAnalyticsFolhaPagamento entity) { return mapper.toDto(entity); }

    @Override
    protected VwAnalyticsFolhaPagamento toEntity(VwAnalyticsFolhaPagamentoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(VwAnalyticsFolhaPagamento entity) { return entity.getFolhaPagamentoId(); }

    @Override
    protected Integer getDtoId(VwAnalyticsFolhaPagamentoDTO dto) { return dto.getFolhaPagamentoId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar visão analítica de folha por período, colaborador e composição financeira", description = "Lista registros analíticos de folha por competência, data de pagamento, funcionário, cargo, departamento, equipe, base, universo, perfil de folha, composição e faixas financeiras para painéis, conciliação e leitura gerencial.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<Page<EntityModel<VwAnalyticsFolhaPagamentoDTO>>>> filter(@RequestBody VwAnalyticsFolhaPagamentoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer visão analítica de folha em grandes volumes para painéis financeiros", description = "Navega por registros analíticos de folha usando cursor, preservando filtros de período, organização, perfil de folha e métricas financeiras em tabelas extensas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<VwAnalyticsFolhaPagamentoDTO>>>> filterByCursor(@RequestBody VwAnalyticsFolhaPagamentoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar registro de folha dentro de um recorte analítico filtrado", description = "Informa a posição de uma linha analítica de folha em um recorte filtrado por período, colaborador ou dimensão organizacional para retomada de navegação em tabelas e painéis.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody VwAnalyticsFolhaPagamentoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar base agregada de análise de folha de pagamento", description = "Retorna a coleção completa da projeção analítica de folha para materialização em rotinas de análise, conciliação, exportação ou sincronização de painéis.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<List<EntityModel<VwAnalyticsFolhaPagamentoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar registros analíticos de folha por identificadores de folha", description = "Recupera linhas analíticas já referenciadas por painéis, seleções ou comparações usando identificadores conhecidos de folha de pagamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<VwAnalyticsFolhaPagamentoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar registros analíticos de folha para seletores e comparações", description = "Produz opções compactas de registros analíticos de folha para seleção em formulários, vínculos de painel e comparações guiadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody VwAnalyticsFolhaPagamentoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções selecionadas de análise de folha", description = "Reidrata opções de análises de folha já escolhidas em formulários, filtros salvos ou painéis, preservando identificador e rótulo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @Override
    @PostMapping("/stats/group-by")
    @UiAnalytics(
            projections = {
                    @AnalyticsProjection(
                            id = "payroll-ranking-table",
                            intent = AnalyticsIntent.RANKING,
                            sourceOperation = AnalyticsOperation.GROUP_BY,
                            sourceResource = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO,
                            primaryDimension = @AnalyticsDimensionBinding(field = "universo", label = "Universo"),
                            primaryMetrics = {
                                    @AnalyticsMetricBinding(field = "salarioLiquido", aggregation = "sum", label = "Salário líquido")
                            },
                            defaultSort = {
                                    @AnalyticsSort(field = "salarioLiquido", direction = AnalyticsSortDirection.DESC)
                            },
                            defaultLimit = 10,
                            preferredFamilies = {
                                    AnalyticsPresentationFamily.ANALYTIC_TABLE
                            },
                            drillDown = true
                    ),
                    @AnalyticsProjection(
                            id = "payroll-ranking-chart",
                            intent = AnalyticsIntent.RANKING,
                            sourceOperation = AnalyticsOperation.GROUP_BY,
                            sourceResource = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO,
                            primaryDimension = @AnalyticsDimensionBinding(field = "universo", label = "Universo"),
                            primaryMetrics = {
                                    @AnalyticsMetricBinding(field = "salarioLiquido", aggregation = "sum", label = "Salário líquido")
                            },
                            defaultSort = {
                                    @AnalyticsSort(field = "salarioLiquido", direction = AnalyticsSortDirection.DESC)
                            },
                            defaultLimit = 10,
                            preferredFamilies = {
                                    AnalyticsPresentationFamily.CHART
                            },
                            drillDown = true
                    ),
                    @AnalyticsProjection(
                            id = "payroll-profile-chart",
                            intent = AnalyticsIntent.COMPOSITION,
                            sourceOperation = AnalyticsOperation.GROUP_BY,
                            sourceResource = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO,
                            primaryDimension = @AnalyticsDimensionBinding(field = "payrollProfile", label = "Perfil"),
                            primaryMetrics = {
                                    @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Registros")
                            },
                            defaultSort = {
                                    @AnalyticsSort(field = "count", direction = AnalyticsSortDirection.DESC)
                            },
                            defaultLimit = 8,
                            preferredFamilies = {
                                    AnalyticsPresentationFamily.CHART
                            },
                            pointSelection = true
                    )
            }
    )
    @Operation(
            summary = "Agrupar salário líquido por universo, perfil ou composição de folha",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = GroupByStatsRequest.class),
                            examples = @ExampleObject(name = "payrollGroupBy", value = GROUP_BY_STATS_REQUEST_EXAMPLE)
                    )
            ),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Agrupamento analítico de folha calculado com sucesso.",
                    content = @Content(
                            schema = @Schema(implementation = GroupByStatsResponse.class),
                            examples = @ExampleObject(name = "payrollGroupByResponse", value = GROUP_BY_STATS_RESPONSE_EXAMPLE)
                    )
            )
    )
    public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(@RequestBody GroupByStatsRequest<VwAnalyticsFolhaPagamentoFilterDTO> request) {
        return super.groupByStats(request);
    }

    @Override
    @PostMapping("/stats/timeseries")
    @UiAnalytics(
            projections = {
                    @AnalyticsProjection(
                            id = "payroll-trend-chart",
                            intent = AnalyticsIntent.TREND,
                            sourceOperation = AnalyticsOperation.TIMESERIES,
                            sourceResource = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO,
                            primaryDimension = @AnalyticsDimensionBinding(field = "competencia", role = "time", label = "Competência"),
                            primaryMetrics = {
                                    @AnalyticsMetricBinding(field = "salarioLiquido", aggregation = "sum", label = "Salário líquido")
                            },
                            preferredFamilies = {
                                    AnalyticsPresentationFamily.CHART
                            },
                            defaultGranularity = AnalyticsGranularity.MONTH,
                            pointSelection = true
                    )
            }
    )
    @Operation(
            summary = "Calcular tendência mensal de salário líquido na folha",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TimeSeriesStatsRequest.class),
                            examples = @ExampleObject(name = "payrollTrend", value = TIME_SERIES_STATS_REQUEST_EXAMPLE)
                    )
            ),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Série temporal de folha calculada com sucesso.",
                    content = @Content(
                            schema = @Schema(implementation = TimeSeriesStatsResponse.class),
                            examples = @ExampleObject(name = "payrollTrendResponse", value = TIME_SERIES_STATS_RESPONSE_EXAMPLE)
                    )
            )
    )
    public ResponseEntity<RestApiResponse<TimeSeriesStatsResponse>> timeSeriesStats(
            @RequestBody TimeSeriesStatsRequest<VwAnalyticsFolhaPagamentoFilterDTO> request
    ) {
        return super.timeSeriesStats(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe analítico de folha de um colaborador", description = "Retorna a linha analítica de folha com contexto de colaborador, organização, operação, competência, valores de folha, eventos e faixas financeiras para leitura gerencial ou composição de visões derivadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<VwAnalyticsFolhaPagamentoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }
}







