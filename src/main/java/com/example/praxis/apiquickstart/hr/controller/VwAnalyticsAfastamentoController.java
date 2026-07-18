package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.controller.base.AbstractHrDepartmentScopedAnalyticsController;
import com.example.praxis.apiquickstart.hr.dto.VwAnalyticsAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.VwAnalyticsAfastamentoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.VwAnalyticsAfastamento;
import com.example.praxis.apiquickstart.hr.mapper.VwAnalyticsAfastamentoMapper;
import com.example.praxis.apiquickstart.hr.security.HrDepartmentScopeAccess;
import com.example.praxis.apiquickstart.hr.service.AbsenceCriticalityPolicy;
import com.example.praxis.apiquickstart.hr.service.VwAnalyticsAfastamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.annotation.AnalyticsComparisonPeriodBinding;
import org.praxisplatform.uischema.annotation.AnalyticsDimensionBinding;
import org.praxisplatform.uischema.annotation.AnalyticsIntent;
import org.praxisplatform.uischema.annotation.AnalyticsMetricBinding;
import org.praxisplatform.uischema.annotation.AnalyticsOperation;
import org.praxisplatform.uischema.annotation.AnalyticsPolicyReference;
import org.praxisplatform.uischema.annotation.AnalyticsPresentationFamily;
import org.praxisplatform.uischema.annotation.AnalyticsProjection;
import org.praxisplatform.uischema.annotation.AnalyticsRecordOpen;
import org.praxisplatform.uischema.annotation.AnalyticsSort;
import org.praxisplatform.uischema.annotation.AnalyticsSortDirection;
import org.praxisplatform.uischema.annotation.AnalyticsSurfaceTarget;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.UiAnalytics;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.exporting.CollectionExportRequest;
import org.praxisplatform.uischema.stats.ComparisonPeriodMode;
import org.praxisplatform.uischema.stats.ComparisonPeriodPreset;
import org.praxisplatform.uischema.stats.dto.ComparisonStatsRequest;
import org.praxisplatform.uischema.stats.dto.ComparisonStatsResponse;
import org.praxisplatform.uischema.stats.dto.DistributionStatsRequest;
import org.praxisplatform.uischema.stats.dto.DistributionStatsResponse;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import org.praxisplatform.uischema.stats.dto.TimeSeriesStatsRequest;
import org.praxisplatform.uischema.stats.dto.TimeSeriesStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Recurso read-only do piloto G2 de afastamentos.
 *
 * <p>A projection prova que comparison e listas criticas podem operar com lotacao historica e
 * campos seguros, sem recorrer ao departamento atual do cadastro nem expor motivo/observacoes.</p>
 */
@ApiResource(value = ApiPaths.HumanResources.VW_ANALYTICS_AFASTAMENTOS, resourceKey = "human-resources.vw-analytics-afastamentos")
@ApiGroup("human-resources")
@RestController
public class VwAnalyticsAfastamentoController extends AbstractHrDepartmentScopedAnalyticsController<VwAnalyticsAfastamento, VwAnalyticsAfastamentoDTO, String, VwAnalyticsAfastamentoFilterDTO> {

    private final VwAnalyticsAfastamentoService service;
    private final VwAnalyticsAfastamentoMapper mapper;

    public VwAnalyticsAfastamentoController(
            VwAnalyticsAfastamentoService service,
            VwAnalyticsAfastamentoMapper mapper,
            HrDepartmentScopeAccess departmentScopeAccess
    ) {
        super(departmentScopeAccess, VwAnalyticsAfastamentoFilterDTO::new);
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected VwAnalyticsAfastamentoService getService() {
        return service;
    }

    @Override
    protected VwAnalyticsAfastamentoDTO toDto(VwAnalyticsAfastamento entity) {
        return mapper.toDto(entity);
    }

    @Override
    protected VwAnalyticsAfastamento toEntity(VwAnalyticsAfastamentoDTO dto) {
        return mapper.toEntity(dto);
    }

    @Override
    protected String getEntityId(VwAnalyticsAfastamento entity) {
        return entity.getAnalyticsId();
    }

    @Override
    protected String getDtoId(VwAnalyticsAfastamentoDTO dto) {
        return dto.getAnalyticsId();
    }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar analytics de afastamentos por lotação histórica", description = "Lista linhas mensais de afastamento atribuídas à lotação efetiva do período, com dias afastados e criticidade versionada para painéis corporativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<Page<EntityModel<VwAnalyticsAfastamentoDTO>>>> filter(
            @RequestBody VwAnalyticsAfastamentoFilterDTO filterDTO,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "includeIds", required = false) List<String> includeIds,
            @RequestParam MultiValueMap<String, String> queryParams
    ) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer analytics de afastamentos por cursor", description = "Navega por grandes volumes de linhas analíticas de afastamento preservando filtros de competência, departamento e criticidade.")
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<VwAnalyticsAfastamentoDTO>>>> filterByCursor(
            @RequestBody VwAnalyticsAfastamentoFilterDTO filterDTO,
            @RequestParam(name = "after", required = false) String after,
            @RequestParam(name = "before", required = false) String before,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam MultiValueMap<String, String> queryParams
    ) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar linha analítica de afastamento", description = "Informa a posição de uma linha de analytics dentro do recorte filtrado para retomada de navegação em tabelas e dashboards.")
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(
            @RequestBody VwAnalyticsAfastamentoFilterDTO filterDTO,
            @RequestParam("id") String id,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam MultiValueMap<String, String> queryParams
    ) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar analytics de afastamentos", description = "Retorna a coleção completa da projection read-only para rotinas de análise, exportação e validação de painéis.")
    public ResponseEntity<RestApiResponse<List<EntityModel<VwAnalyticsAfastamentoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar linhas analíticas de afastamento por ID", description = "Reidrata linhas já selecionadas em painéis ou filtros salvos usando a identidade analítica estável.")
    public ResponseEntity<List<VwAnalyticsAfastamentoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<String> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar linhas de analytics de afastamentos", description = "Produz opções compactas para seletores e composições de dashboard baseadas na projection de afastamentos.")
    public ResponseEntity<Page<org.praxisplatform.uischema.dto.OptionDTO<String>>> filterOptions(
            @RequestBody VwAnalyticsAfastamentoFilterDTO filterDTO,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam MultiValueMap<String, String> queryParams
    ) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções selecionadas de afastamentos", description = "Reidrata opções já selecionadas preservando a identidade analítica.")
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<String>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<String> ids) {
        return super.getOptionsByIds(ids);
    }

    @Override
    @PostMapping("/export")
    public ResponseEntity<?> exportCollection(@RequestBody CollectionExportRequest<VwAnalyticsAfastamentoFilterDTO> request) {
        return super.exportCollection(request);
    }

    @Override
    @PostMapping("/stats/comparison")
    @UiAnalytics(
            projections = {
                    @AnalyticsProjection(
                            id = "absence-department-comparison",
                            intent = AnalyticsIntent.COMPARISON,
                            sourceOperation = AnalyticsOperation.COMPARISON,
                            sourceResource = ApiPaths.HumanResources.VW_ANALYTICS_AFASTAMENTOS,
                            primaryDimension = @AnalyticsDimensionBinding(
                                    field = "departamento",
                                    label = "Departamento",
                                    keyFilterField = "departamentoIdsIn"
                            ),
                            comparisonPeriod = @AnalyticsComparisonPeriodBinding(
                                    field = "competencia",
                                    timezone = "America/Sao_Paulo",
                                    preset = ComparisonPeriodPreset.THIS_MONTH,
                                    mode = ComparisonPeriodMode.PREVIOUS_CALENDAR_PERIOD
                            ),
                            primaryMetrics = {
                                    @AnalyticsMetricBinding(field = "funcionarioId", aggregation = "distinct_count", label = "Colaboradores"),
                                    @AnalyticsMetricBinding(field = "diasAfastado", aggregation = "sum", label = "Dias afastado")
                            },
                            policyRefs = {
                                    @AnalyticsPolicyReference(
                                            policyId = AbsenceCriticalityPolicy.POLICY_ID,
                                            policyVersion = AbsenceCriticalityPolicy.POLICY_VERSION,
                                            role = "criticality",
                                            resultField = "criticalityLevel",
                                            policyIdField = "criticalityPolicyId",
                                            policyVersionField = "criticalityPolicyVersion"
                                    )
                            },
                            defaultSort = {
                                    @AnalyticsSort(field = "diasAfastado", direction = AnalyticsSortDirection.DESC)
                            },
                            preferredFamilies = {
                                    AnalyticsPresentationFamily.ANALYTIC_TABLE,
                                    AnalyticsPresentationFamily.CHART
                            },
                            crossFilter = true,
                            recordOpen = @AnalyticsRecordOpen(
                                    sourceIdentityField = "funcionarioId",
                                    target = @AnalyticsSurfaceTarget(
                                            resourceKey = "human-resources.funcionarios",
                                            surfaceId = "hero-profile"
                                    )
                            )
                    )
            }
    )
    @Operation(summary = "Comparar afastamentos por departamento", description = "Compara periodo atual e anterior por departamento efetivo, usando DISTINCT_COUNT de colaboradores e SUM de dias afastados.")
    public ResponseEntity<RestApiResponse<ComparisonStatsResponse>> comparisonStats(
            @RequestBody ComparisonStatsRequest<VwAnalyticsAfastamentoFilterDTO> request
    ) {
        return super.comparisonStats(request);
    }

    @Override
    @PostMapping("/stats/group-by")
    public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
            @RequestBody GroupByStatsRequest<VwAnalyticsAfastamentoFilterDTO> request
    ) {
        return super.groupByStats(request);
    }

    @Override
    @PostMapping("/stats/timeseries")
    public ResponseEntity<RestApiResponse<TimeSeriesStatsResponse>> timeSeriesStats(
            @RequestBody TimeSeriesStatsRequest<VwAnalyticsAfastamentoFilterDTO> request
    ) {
        return super.timeSeriesStats(request);
    }

    @Override
    @PostMapping("/stats/distribution")
    public ResponseEntity<RestApiResponse<DistributionStatsResponse>> distributionStats(
            @RequestBody DistributionStatsRequest<VwAnalyticsAfastamentoFilterDTO> request
    ) {
        return super.distributionStats(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter linha analítica de afastamento", description = "Retorna uma linha mensal de afastamento com lotação histórica, dias e criticidade versionada.")
    public ResponseEntity<RestApiResponse<VwAnalyticsAfastamentoDTO>> getById(@PathVariable String id) {
        requireDepartment(service.findById(id).getDepartamentoId());
        return super.getById(id);
    }
}
