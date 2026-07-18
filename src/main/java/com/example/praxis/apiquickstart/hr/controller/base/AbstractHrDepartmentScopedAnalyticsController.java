package com.example.praxis.apiquickstart.hr.controller.base;

import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartReadOnlyController;
import com.example.praxis.apiquickstart.hr.security.HrDepartmentScopeAccess;
import com.example.praxis.apiquickstart.hr.security.HrDepartmentScopedFilter;
import org.praxisplatform.uischema.dto.CursorPage;
import org.praxisplatform.uischema.dto.LocateResponse;
import org.praxisplatform.uischema.dto.OptionDTO;
import org.praxisplatform.uischema.exporting.CollectionExportRequest;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
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

import java.util.List;
import java.util.function.Supplier;

/**
 * Applies the host-resolved HR department scope to every inherited collection operation.
 *
 * <p>Concrete resources still own endpoint documentation and semantic projections. This base owns
 * only the shared authorization boundary, so new stats or collection surfaces cannot accidentally
 * omit row scope while public filters remain ordinary query criteria.</p>
 */
public abstract class AbstractHrDepartmentScopedAnalyticsController<
        E,
        D,
        ID,
        FD extends GenericFilterDTO & HrDepartmentScopedFilter>
        extends AbstractQuickstartReadOnlyController<E, D, ID, FD> {

    private final HrDepartmentScopeAccess departmentScopeAccess;
    private final Supplier<FD> emptyFilterFactory;

    protected AbstractHrDepartmentScopedAnalyticsController(
            HrDepartmentScopeAccess departmentScopeAccess,
            Supplier<FD> emptyFilterFactory
    ) {
        this.departmentScopeAccess = departmentScopeAccess;
        this.emptyFilterFactory = emptyFilterFactory;
    }

    @Override
    public ResponseEntity<RestApiResponse<Page<EntityModel<D>>>> filter(
            FD filter,
            int page,
            int size,
            List<ID> includeIds,
            MultiValueMap<String, String> queryParams
    ) {
        return super.filter(scope(filter), page, size, includeIds, queryParams);
    }

    @Override
    public ResponseEntity<RestApiResponse<CursorPage<EntityModel<D>>>> filterByCursor(
            FD filter,
            String after,
            String before,
            int size,
            MultiValueMap<String, String> queryParams
    ) {
        return super.filterByCursor(scope(filter), after, before, size, queryParams);
    }

    @Override
    public ResponseEntity<LocateResponse> locate(
            FD filter,
            ID id,
            int size,
            MultiValueMap<String, String> queryParams
    ) {
        return super.locate(scope(filter), id, size, queryParams);
    }

    @Override
    public ResponseEntity<RestApiResponse<List<EntityModel<D>>>> getAll() {
        departmentScopeAccess.requireUnscoped();
        return super.getAll();
    }

    @Override
    public ResponseEntity<List<D>> getByIds(List<ID> ids) {
        departmentScopeAccess.requireUnscoped();
        return super.getByIds(ids);
    }

    @Override
    public ResponseEntity<Page<OptionDTO<ID>>> filterOptions(
            FD filter,
            int page,
            int size,
            MultiValueMap<String, String> queryParams
    ) {
        return super.filterOptions(scope(filter), page, size, queryParams);
    }

    @Override
    public ResponseEntity<List<OptionDTO<ID>>> getOptionsByIds(List<ID> ids) {
        departmentScopeAccess.requireUnscoped();
        return super.getOptionsByIds(ids);
    }

    @Override
    public ResponseEntity<?> exportCollection(CollectionExportRequest<FD> request) {
        FD scopedFilter = scope(request.filters());
        return super.exportCollection(new CollectionExportRequest<>(
                request.componentType(), request.componentId(), request.resourcePath(), request.format(), request.scope(),
                request.selection(), request.fields(), scopedFilter, request.sort(), request.pagination(), request.query(),
                request.includeHeaders(), request.applyFormatting(), request.maxRows(), request.fileName(), request.formatOptions(),
                request.localization(), request.metadata()
        ));
    }

    @Override
    public ResponseEntity<RestApiResponse<ComparisonStatsResponse>> comparisonStats(
            ComparisonStatsRequest<FD> request
    ) {
        FD scopedFilter = departmentScopeAccess.applyAggregateComparisonScope(
                request.filter(), emptyFilterFactory);
        return super.comparisonStats(new ComparisonStatsRequest<>(
                scopedFilter, request.field(), request.periodField(), request.metrics(), request.period(), request.limit(), request.orderBy()
        ));
    }

    @Override
    public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(GroupByStatsRequest<FD> request) {
        return super.groupByStats(new GroupByStatsRequest<>(
                scope(request.filter()), request.field(), request.metric(), request.limit(), request.orderBy(), request.metrics()
        ));
    }

    @Override
    public ResponseEntity<RestApiResponse<TimeSeriesStatsResponse>> timeSeriesStats(TimeSeriesStatsRequest<FD> request) {
        return super.timeSeriesStats(new TimeSeriesStatsRequest<>(
                scope(request.filter()), request.field(), request.granularity(), request.metric(), request.from(), request.to(),
                request.fillGaps(), request.metrics()
        ));
    }

    @Override
    public ResponseEntity<RestApiResponse<DistributionStatsResponse>> distributionStats(
            DistributionStatsRequest<FD> request
    ) {
        return super.distributionStats(new DistributionStatsRequest<>(
                scope(request.filter()), request.field(), request.mode(), request.metric(), request.bucketSize(), request.bucketCount(),
                request.limit(), request.orderBy()
        ));
    }

    protected final void requireDepartment(Integer departmentId) {
        departmentScopeAccess.requireDepartment(departmentId);
    }

    private FD scope(FD filter) {
        return departmentScopeAccess.applyAnalyticsScope(filter, emptyFilterFactory);
    }
}
