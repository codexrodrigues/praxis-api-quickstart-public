package com.example.praxis.apiquickstart.procurement.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartReadOnlyController;
import com.example.praxis.apiquickstart.procurement.dto.VwSupplierProcurementFunnelDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.VwSupplierProcurementFunnelFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.VwSupplierProcurementFunnel;
import com.example.praxis.apiquickstart.procurement.mapper.VwSupplierProcurementFunnelMapper;
import com.example.praxis.apiquickstart.procurement.service.VwSupplierProcurementFunnelService;
import org.praxisplatform.uischema.annotation.AnalyticsDimensionBinding;
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
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Recurso analitico que prova funnel/pyramid e tabela sobre a mesma projection remota. */
@ApiResource(
        value = ApiPaths.Procurement.SUPPLIER_PROCUREMENT_FUNNEL,
        resourceKey = "procurement.vw-supplier-procurement-funnel",
        title = "Funil de procurement por fornecedor",
        description = "Etapas cumulativas de fornecedores por empresa, da identificacao ao recebimento.",
        icon = "filter-alt",
        visualTone = "procurement"
)
@ApiGroup("procurement")
public class VwSupplierProcurementFunnelController extends AbstractQuickstartReadOnlyController<
        VwSupplierProcurementFunnel, VwSupplierProcurementFunnelDTO, String,
        VwSupplierProcurementFunnelFilterDTO> {

    private final VwSupplierProcurementFunnelService service;
    private final VwSupplierProcurementFunnelMapper mapper;

    public VwSupplierProcurementFunnelController(
            VwSupplierProcurementFunnelService service,
            VwSupplierProcurementFunnelMapper mapper
    ) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected VwSupplierProcurementFunnelService getService() { return service; }

    @Override
    protected VwSupplierProcurementFunnelDTO toDto(VwSupplierProcurementFunnel entity) {
        return mapper.toDto(entity);
    }

    @Override
    protected VwSupplierProcurementFunnel toEntity(VwSupplierProcurementFunnelDTO dto) {
        return mapper.toEntity(dto);
    }

    @Override
    protected String getEntityId(VwSupplierProcurementFunnel entity) { return entity.getAnalyticsId(); }

    @Override
    protected String getDtoId(VwSupplierProcurementFunnelDTO dto) { return dto.getAnalyticsId(); }

    @Override
    @PostMapping("/stats/group-by")
    @UiSurface(
            id = "supplier-procurement-funnel",
            title = "Funil de procurement",
            kind = SurfaceKind.CHART,
            scope = SurfaceScope.COLLECTION,
            description = "Compara o volume cumulativo de fornecedores em cada etapa persistida do ciclo de procurement."
    )
    @UiAnalytics(projections = {
            @AnalyticsProjection(
                    id = "supplier-procurement-funnel",
                    intent = AnalyticsIntent.COMPOSITION,
                    sourceOperation = AnalyticsOperation.GROUP_BY,
                    sourceResource = ApiPaths.Procurement.SUPPLIER_PROCUREMENT_FUNNEL,
                    primaryDimension = @AnalyticsDimensionBinding(field = "stage", role = "category", label = "Etapa"),
                    primaryMetrics = {
                            @AnalyticsMetricBinding(field = "volume", aggregation = "sum", label = "Fornecedores")
                    },
                    defaultSort = {
                            @AnalyticsSort(field = "stage", direction = AnalyticsSortDirection.ASC)
                    },
                    defaultLimit = 6,
                    preferredFamilies = {
                            AnalyticsPresentationFamily.CHART,
                            AnalyticsPresentationFamily.ANALYTIC_TABLE,
                            AnalyticsPresentationFamily.KPI
                    },
                    pointSelection = true
            )
    })
    public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
            @RequestBody GroupByStatsRequest<VwSupplierProcurementFunnelFilterDTO> request
    ) {
        return super.groupByStats(request);
    }
}
