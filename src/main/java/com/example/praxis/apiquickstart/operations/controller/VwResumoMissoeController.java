package com.example.praxis.apiquickstart.operations.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.VwResumoMissoeDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.VwResumoMissoeFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.VwResumoMissoe;
import com.example.praxis.apiquickstart.operations.mapper.VwResumoMissoeMapper;
import com.example.praxis.apiquickstart.operations.service.VwResumoMissoeService;
import org.praxisplatform.uischema.annotation.AnalyticsDimensionBinding;
import org.praxisplatform.uischema.annotation.AnalyticsGranularity;
import org.praxisplatform.uischema.annotation.AnalyticsIntent;
import org.praxisplatform.uischema.annotation.AnalyticsMetricBinding;
import org.praxisplatform.uischema.annotation.AnalyticsOperation;
import org.praxisplatform.uischema.annotation.AnalyticsPresentationFamily;
import org.praxisplatform.uischema.annotation.AnalyticsProjection;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.UiAnalytics;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartReadOnlyController;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
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

@ApiResource(value = ApiPaths.Operations.VW_RESUMO_MISSOES, resourceKey = "operations.vw-resumo-missoes")
@ApiGroup("operations")
/**
 * Controller read-only de referência para visão agregada de missões.
 *
 * <p>Ele existe para demonstrar que o quickstart não expõe apenas entidades de
 * escrita: também publica recursos derivados e analíticos usando o mesmo padrão
 * de filtros, paginação, locate e option source. Como exemplo público da Praxis,
 * esta classe ajuda a orientar quando usar um endpoint analítico derivado em vez
 * de tentar reconstruir dashboards diretamente a partir do CRUD primário.</p>
 */
public class VwResumoMissoeController extends AbstractQuickstartReadOnlyController<VwResumoMissoe, VwResumoMissoeDTO, Integer, VwResumoMissoeFilterDTO> {

    private final VwResumoMissoeService service;
    private final VwResumoMissoeMapper mapper;

    @Autowired
    public VwResumoMissoeController(VwResumoMissoeService service, VwResumoMissoeMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected VwResumoMissoeService getService() { return service; }

    @Override
    protected VwResumoMissoeDTO toDto(VwResumoMissoe entity) { return mapper.toDto(entity); }

    @Override
    protected VwResumoMissoe toEntity(VwResumoMissoeDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(VwResumoMissoe entity) { return entity.getMissaoId(); }

    @Override
    protected Integer getDtoId(VwResumoMissoeDTO dto) { return dto.getMissaoId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar resumos de missões", description = "Lista visões agregadas por período, status, prioridade, equipe e volume de eventos para dashboards operacionais e acompanhamento executivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<VwResumoMissoeDTO>>>> filter(@RequestBody VwResumoMissoeFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar resumos de missões com paginação por cursor", description = "Percorre grandes volumes de resumos agregados usando cursor, útil para catálogos analíticos e telas com navegação contínua.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<VwResumoMissoeDTO>>>> filterByCursor(@RequestBody VwResumoMissoeFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar resumo de missão em listas filtradas", description = "Informa em qual posição uma visão agregada aparece dentro do recorte filtrado, útil para retornar ao item certo em tabelas analíticas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody VwResumoMissoeFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar resumos de missões", description = "Retorna toda a visão agregada de missões quando o consumidor precisa materializar o conjunto completo para analytics, exportação ou reconciliação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<VwResumoMissoeDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar resumos de missões por IDs", description = "Recupera visões agregadas de missões específicas já referenciadas por outro fluxo analítico sem reaplicar filtros do dashboard.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<VwResumoMissoeDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar resumos de missões para formulários", description = "Produz opções compactas de missões resumidas para campos que precisam identificar rapidamente uma missão pelo contexto analítico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody VwResumoMissoeFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar resumos de missões selecionados", description = "Reidrata opções de missões já escolhidas em formulários e filtros salvos, preservando identificador e rótulo analítico exibido.")
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
                            id = "mission-status-chart",
                            intent = AnalyticsIntent.COMPOSITION,
                            sourceOperation = AnalyticsOperation.GROUP_BY,
                            sourceResource = ApiPaths.Operations.VW_RESUMO_MISSOES,
                            primaryDimension = @AnalyticsDimensionBinding(field = "status", label = "Status"),
                            primaryMetrics = {
                                    @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Missões")
                            },
                            preferredFamilies = {
                                    AnalyticsPresentationFamily.CHART,
                                    AnalyticsPresentationFamily.ANALYTIC_TABLE
                            },
                            pointSelection = true
                    ),
                    @AnalyticsProjection(
                            id = "mission-priority-chart",
                            intent = AnalyticsIntent.COMPOSITION,
                            sourceOperation = AnalyticsOperation.GROUP_BY,
                            sourceResource = ApiPaths.Operations.VW_RESUMO_MISSOES,
                            primaryDimension = @AnalyticsDimensionBinding(field = "prioridade", label = "Prioridade"),
                            primaryMetrics = {
                                    @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Missões")
                            },
                            preferredFamilies = {
                                    AnalyticsPresentationFamily.CHART
                            },
                            pointSelection = true
                    ),
                    @AnalyticsProjection(
                            id = "mission-threat-ranking",
                            intent = AnalyticsIntent.RANKING,
                            sourceOperation = AnalyticsOperation.GROUP_BY,
                            sourceResource = ApiPaths.Operations.VW_RESUMO_MISSOES,
                            primaryDimension = @AnalyticsDimensionBinding(field = "ameaca", label = "Ameaça"),
                            primaryMetrics = {
                                    @AnalyticsMetricBinding(field = "qtdEventos", aggregation = "sum", label = "Eventos")
                            },
                            defaultLimit = 10,
                            preferredFamilies = {
                                    AnalyticsPresentationFamily.ANALYTIC_TABLE,
                                    AnalyticsPresentationFamily.CHART
                            },
                            drillDown = true
                    )
            }
    )
    @Operation(summary = "Group-by stats sobre resumos de missões", description = "Calcula composições e rankings operacionais a partir da visão agregada de missões.")
    public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
            @RequestBody GroupByStatsRequest<VwResumoMissoeFilterDTO> request
    ) {
        return super.groupByStats(request);
    }

    @Override
    @PostMapping("/stats/timeseries")
    @UiAnalytics(
            projections = {
                    @AnalyticsProjection(
                            id = "mission-activity-trend",
                            intent = AnalyticsIntent.TREND,
                            sourceOperation = AnalyticsOperation.TIMESERIES,
                            sourceResource = ApiPaths.Operations.VW_RESUMO_MISSOES,
                            primaryDimension = @AnalyticsDimensionBinding(field = "ultimaAcao", role = "time", label = "Última ação"),
                            primaryMetrics = {
                                    @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Missões")
                            },
                            preferredFamilies = {
                                    AnalyticsPresentationFamily.CHART,
                                    AnalyticsPresentationFamily.ANALYTIC_TABLE
                            },
                            defaultGranularity = AnalyticsGranularity.DAY,
                            pointSelection = true
                    )
            }
    )
    @Operation(summary = "Time-series stats sobre resumos de missões", description = "Calcula tendência temporal da atividade operacional das missões.")
    public ResponseEntity<RestApiResponse<TimeSeriesStatsResponse>> timeSeriesStats(
            @RequestBody TimeSeriesStatsRequest<VwResumoMissoeFilterDTO> request
    ) {
        return super.timeSeriesStats(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter resumo de missão", description = "Retorna o painel resumido de uma missão específica, com métricas agregadas de participantes, eventos e evolução temporal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<VwResumoMissoeDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }
}









