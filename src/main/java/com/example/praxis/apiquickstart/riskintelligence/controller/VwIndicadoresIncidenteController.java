package com.example.praxis.apiquickstart.riskintelligence.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.riskintelligence.dto.VwIndicadoresIncidenteDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.filter.VwIndicadoresIncidenteFilterDTO;
import com.example.praxis.apiquickstart.riskintelligence.entity.VwIndicadoresIncidente;
import com.example.praxis.apiquickstart.riskintelligence.mapper.VwIndicadoresIncidenteMapper;
import com.example.praxis.apiquickstart.riskintelligence.service.VwIndicadoresIncidenteService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.AnalyticsDimensionBinding;
import org.praxisplatform.uischema.annotation.AnalyticsGranularity;
import org.praxisplatform.uischema.annotation.AnalyticsIntent;
import org.praxisplatform.uischema.annotation.AnalyticsMetricBinding;
import org.praxisplatform.uischema.annotation.AnalyticsOperation;
import org.praxisplatform.uischema.annotation.AnalyticsPresentationFamily;
import org.praxisplatform.uischema.annotation.AnalyticsProjection;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.annotation.UiAnalytics;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartReadOnlyController;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.stats.dto.TimeSeriesStatsRequest;
import org.praxisplatform.uischema.stats.dto.TimeSeriesStatsResponse;
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

/**
 * Recurso read-only de indicadores de incidente usado para demonstrar analytics de risco.
 *
 * <p>Ele complementa {@code AmeacaController} mostrando a outra metade da narrativa do dominio:
 * depois do dado transacional, a plataforma pode publicar uma view agregada com filter, options,
 * stats e hypermedia sem abrir semantica de escrita.</p>
 */
@ApiResource(
        value = ApiPaths.RiskIntelligence.VW_INDICADORES_INCIDENTES,
        resourceKey = "risk-intelligence.vw-indicadores-incidentes",
        title = "Indicadores de incidentes",
        description = "Visao analitica de incidentes por severidade, impacto, periodo, base operacional e contexto de risco.",
        icon = "chart-no-axes-combined",
        visualTone = "risk-intelligence"
)
@ApiGroup("risk-intelligence")
public class VwIndicadoresIncidenteController extends AbstractQuickstartReadOnlyController<VwIndicadoresIncidente, VwIndicadoresIncidenteDTO, Integer, VwIndicadoresIncidenteFilterDTO> {

    private static final String TIME_SERIES_STATS_REQUEST_EXAMPLE = """
            {
              "filter": {},
              "field": "dataIncidente",
              "granularity": "month",
              "metrics": [
                { "metric": "count", "alias": "totalIncidentes" }
              ],
              "fillGaps": true
            }
            """;
    private static final String TIME_SERIES_STATS_RESPONSE_EXAMPLE = """
            {
              "success": true,
              "data": {
                "field": "dataIncidente",
                "granularity": "month",
                "metrics": [
                  { "aggregation": "count", "alias": "totalIncidentes" }
                ],
                "points": [
                  { "label": "2026-01", "values": { "totalIncidentes": 12 } }
                ]
              }
            }
            """;

    private final VwIndicadoresIncidenteService service;
    private final VwIndicadoresIncidenteMapper mapper;

    @Autowired
    public VwIndicadoresIncidenteController(VwIndicadoresIncidenteService service, VwIndicadoresIncidenteMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected VwIndicadoresIncidenteService getService() { return service; }

    @Override
    protected VwIndicadoresIncidenteDTO toDto(VwIndicadoresIncidente entity) { return mapper.toDto(entity); }

    @Override
    protected VwIndicadoresIncidente toEntity(VwIndicadoresIncidenteDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(VwIndicadoresIncidente entity) { return entity.getIncidenteId(); }

    @Override
    protected Integer getDtoId(VwIndicadoresIncidenteDTO dto) { return dto.getIncidenteId(); }

    @PostMapping("/filter")
    @UiSurface(
            id = "incident-intelligence-board",
            title = "Inteligencia de incidentes",
            kind = SurfaceKind.VIEW,
            scope = SurfaceScope.COLLECTION,
            description = "Explora incidentes por severidade, periodo, impacto e base para apoiar dashboards, comparacoes e investigacao analitica."
    )
    @Operation(summary = "Filtrar indicadores de incidentes", description = "Lista visões agregadas por severidade, impacto financeiro, período, base e contexto para dashboards de risco, leitura executiva e comparação analítica entre incidentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<VwIndicadoresIncidenteDTO>>>> filter(@RequestBody VwIndicadoresIncidenteFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar indicadores de incidentes com paginação por cursor", description = "Percorre grandes volumes de indicadores agregados usando cursor, útil para catálogos analíticos, exploração contínua e painéis com navegação incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<VwIndicadoresIncidenteDTO>>>> filterByCursor(@RequestBody VwIndicadoresIncidenteFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar indicador de incidente em listas filtradas", description = "Informa em qual posição uma visão agregada aparece dentro do recorte filtrado, útil para retornar ao item certo em tabelas analíticas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody VwIndicadoresIncidenteFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar indicadores de incidentes", description = "Retorna toda a visão agregada de incidentes quando o consumidor precisa materializar o conjunto completo para analytics, exportação, reconciliação ou processamento off-line.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<VwIndicadoresIncidenteDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar indicadores de incidentes por IDs", description = "Recupera visões agregadas de incidentes específicos já referenciados por outro fluxo analítico, sem reaplicar recortes do dashboard ou consultas transacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<VwIndicadoresIncidenteDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar indicadores de incidentes para formulários", description = "Produz opções compactas de incidentes agregados para filtros analíticos, seleção guiada e navegação rápida em dashboards e painéis de comparação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody VwIndicadoresIncidenteFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar indicadores de incidentes selecionados", description = "Reidrata incidentes já escolhidos em filtros, dashboards e comparações analíticas, preservando identificador e rótulo resumido exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @Override
    @PostMapping("/stats/timeseries")
    @UiSurface(
            id = "incident-trend-chart",
            title = "Evolucao de incidentes",
            kind = SurfaceKind.CHART,
            scope = SurfaceScope.COLLECTION,
            description = "Materializa a serie temporal de incidentes para visualizar tendencia, sazonalidade e picos de severidade."
    )
    @UiAnalytics(
            projections = {
                    @AnalyticsProjection(
                            id = "incident-trend",
                            intent = AnalyticsIntent.TREND,
                            sourceOperation = AnalyticsOperation.TIMESERIES,
                            sourceResource = ApiPaths.RiskIntelligence.VW_INDICADORES_INCIDENTES,
                            primaryDimension = @AnalyticsDimensionBinding(field = "dataIncidente", role = "time", label = "Data do incidente"),
                            primaryMetrics = {
                                    @AnalyticsMetricBinding(field = "totalIncidentes", aggregation = "count", label = "Total de incidentes")
                            },
                            preferredFamilies = {
                                    AnalyticsPresentationFamily.CHART,
                                    AnalyticsPresentationFamily.ANALYTIC_TABLE
                            },
                            defaultGranularity = AnalyticsGranularity.MONTH,
                            drillDown = true,
                            pointSelection = true
                    )
            }
    )
    @Operation(
            summary = "Time-series stats sobre indicadores de incidentes",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TimeSeriesStatsRequest.class),
                            examples = @ExampleObject(name = "incidentTrend", value = TIME_SERIES_STATS_REQUEST_EXAMPLE)
                    )
            ),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Serie temporal de incidentes calculada com sucesso",
                    content = @Content(
                            schema = @Schema(implementation = TimeSeriesStatsResponse.class),
                            examples = @ExampleObject(name = "incidentTrendResponse", value = TIME_SERIES_STATS_RESPONSE_EXAMPLE)
                    )
            )
    )
    public ResponseEntity<RestApiResponse<TimeSeriesStatsResponse>> timeSeriesStats(
            @org.springframework.web.bind.annotation.RequestBody TimeSeriesStatsRequest<VwIndicadoresIncidenteFilterDTO> request
    ) {
        return super.timeSeriesStats(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter indicador de incidente", description = "Retorna o painel resumido de um incidente específico, com métricas agregadas de impacto, severidade, custo e contexto para leitura analítica sem consultar o recurso transacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<VwIndicadoresIncidenteDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }
}




