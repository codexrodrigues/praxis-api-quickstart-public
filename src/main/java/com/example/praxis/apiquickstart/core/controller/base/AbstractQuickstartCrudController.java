package com.example.praxis.apiquickstart.core.controller.base;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.praxisplatform.uischema.controller.base.AbstractResourceController;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.stats.dto.DistributionStatsRequest;
import org.praxisplatform.uischema.stats.dto.DistributionStatsResponse;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import org.praxisplatform.uischema.stats.dto.TimeSeriesStatsRequest;
import org.praxisplatform.uischema.stats.dto.TimeSeriesStatsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Base didatica para controllers mutaveis do quickstart.
 *
 * <p>Esta classe encapsula o padrao canonico de recurso mutavel usado pela plataforma:
 * controller resource-oriented do starter + exemplos OpenAPI claros para endpoints de stats. Em
 * vez de cada controller repetir a mesma explicacao, o quickstart centraliza aqui a parte comum e
 * deixa os recursos concretos focarem no dominio.</p>
 */
public abstract class AbstractQuickstartCrudController<E, ResponseDTO, ID, FD extends GenericFilterDTO, CreateDTO, UpdateDTO>
        extends AbstractResourceController<ResponseDTO, ID, FD, CreateDTO, UpdateDTO> {

    private static final String GROUP_BY_STATS_REQUEST_EXAMPLE = """
            {
              "filter": {},
              "field": "status",
              "metrics": [
                { "metric": "count" }
              ],
              "includeNullBucket": false,
              "limit": 10
            }
            """;
    private static final String GROUP_BY_STATS_RESPONSE_EXAMPLE = """
            {
              "success": true,
              "data": {
                "field": "status",
                "buckets": [
                  { "key": "ATIVO", "label": "ATIVO", "count": 42, "metrics": { "count": 42 } }
                ],
                "totalBuckets": 1,
                "truncated": false
              }
            }
            """;
    private static final String TIME_SERIES_STATS_REQUEST_EXAMPLE = """
            {
              "filter": {},
              "field": "createdAt",
              "granularity": "day",
              "metrics": [
                { "metric": "sum", "field": "amount" }
              ],
              "from": "2026-01-01T00:00:00Z",
              "to": "2026-01-31T23:59:59Z",
              "fillGaps": true
            }
            """;
    private static final String TIME_SERIES_STATS_RESPONSE_EXAMPLE = """
            {
              "success": true,
              "data": {
                "field": "createdAt",
                "granularity": "day",
                "points": [
                  { "key": "2026-01-01", "label": "2026-01-01", "count": 3, "metrics": { "sum_amount": 1250.00 } }
                ]
              }
            }
            """;
    private static final String DISTRIBUTION_STATS_REQUEST_EXAMPLE = """
            {
              "filter": {},
              "field": "amount",
              "mode": "histogram",
              "interval": 1000,
              "metrics": [
                { "metric": "count" }
              ]
            }
            """;
    private static final String DISTRIBUTION_STATS_RESPONSE_EXAMPLE = """
            {
              "success": true,
              "data": {
                "field": "amount",
                "mode": "histogram",
                "buckets": [
                  { "key": "0", "label": "0-1000", "count": 5, "metrics": { "count": 5 } }
                ]
              }
            }
            """;

    protected abstract ResponseDTO toDto(E entity);

    protected abstract E toEntity(ResponseDTO dto);

    /** Extrai o id da entidade para manter `_links` e rotas item-based coerentes. */
    protected abstract ID getEntityId(E entity);

    /** Extrai o id do DTO de resposta publicado pelo recurso. */
    protected abstract ID getDtoId(ResponseDTO dto);

    @Override
    protected ID getResponseId(ResponseDTO dto) {
        return getDtoId(dto);
    }

    @Override
    @PostMapping("/stats/group-by")
    @Operation(
            summary = "Group-by stats sobre o conjunto filtrado",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = GroupByStatsRequest.class),
                            examples = @ExampleObject(name = "groupByCount", value = GROUP_BY_STATS_REQUEST_EXAMPLE)
                    )
            ),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Group-by calculado com sucesso",
                    content = @Content(
                            schema = @Schema(implementation = GroupByStatsResponse.class),
                            examples = @ExampleObject(name = "groupByCountResponse", value = GROUP_BY_STATS_RESPONSE_EXAMPLE)
                    )
            )
    )
    public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(@RequestBody GroupByStatsRequest<FD> request) {
        return super.groupByStats(request);
    }

    @Override
    @PostMapping("/stats/timeseries")
    @Operation(
            summary = "Time-series stats sobre o conjunto filtrado",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TimeSeriesStatsRequest.class),
                            examples = @ExampleObject(name = "timeSeriesSum", value = TIME_SERIES_STATS_REQUEST_EXAMPLE)
                    )
            ),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Serie temporal calculada com sucesso",
                    content = @Content(
                            schema = @Schema(implementation = TimeSeriesStatsResponse.class),
                            examples = @ExampleObject(name = "timeSeriesSumResponse", value = TIME_SERIES_STATS_RESPONSE_EXAMPLE)
                    )
            )
    )
    public ResponseEntity<RestApiResponse<TimeSeriesStatsResponse>> timeSeriesStats(
            @RequestBody TimeSeriesStatsRequest<FD> request
    ) {
        return super.timeSeriesStats(request);
    }

    @Override
    @PostMapping("/stats/distribution")
    @Operation(
            summary = "Distribution stats sobre o conjunto filtrado",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = DistributionStatsRequest.class),
                            examples = @ExampleObject(name = "distributionHistogram", value = DISTRIBUTION_STATS_REQUEST_EXAMPLE)
                    )
            ),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Distribuicao calculada com sucesso",
                    content = @Content(
                            schema = @Schema(implementation = DistributionStatsResponse.class),
                            examples = @ExampleObject(name = "distributionHistogramResponse", value = DISTRIBUTION_STATS_RESPONSE_EXAMPLE)
                    )
            )
    )
    public ResponseEntity<RestApiResponse<DistributionStatsResponse>> distributionStats(
            @RequestBody DistributionStatsRequest<FD> request
    ) {
        return super.distributionStats(request);
    }
}
