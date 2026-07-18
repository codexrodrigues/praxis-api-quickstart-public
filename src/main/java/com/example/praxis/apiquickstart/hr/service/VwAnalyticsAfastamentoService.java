package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartReadOnlyService;
import com.example.praxis.apiquickstart.hr.dto.VwAnalyticsAfastamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.VwAnalyticsAfastamentoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.VwAnalyticsAfastamento;
import com.example.praxis.apiquickstart.hr.mapper.VwAnalyticsAfastamentoMapper;
import com.example.praxis.apiquickstart.hr.repository.VwAnalyticsAfastamentoRepository;
import com.example.praxis.apiquickstart.hr.security.HrDepartmentScopeAccess;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.praxisplatform.uischema.options.service.OptionSourceOperation;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class VwAnalyticsAfastamentoService extends AbstractQuickstartReadOnlyService<VwAnalyticsAfastamento, VwAnalyticsAfastamentoDTO, String, VwAnalyticsAfastamentoFilterDTO> {

    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .labeledGroupByBucket("departamento", "departamentoId", "departamento", Set.of(StatsMetric.COUNT))
            .groupByBucket("criticalityLevel", "criticalityLevel", Set.of(StatsMetric.COUNT))
            .groupByBucket("ano", "ano", Set.of(StatsMetric.COUNT))
            .groupByBucket("mes", "mes", Set.of(StatsMetric.COUNT))
            .temporalTimeSeriesField("competencia", "competencia")
            .metricField("diasAfastado", "diasAfastado", Set.of(StatsMetric.SUM, StatsMetric.AVG, StatsMetric.MIN, StatsMetric.MAX))
            .distinctCountField("funcionarioId", "funcionarioId")
            .build();

    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(VwAnalyticsAfastamento.class, new OptionSourceDescriptor(
                    "criticalityLevel",
                    OptionSourceType.DISTINCT_DIMENSION,
                    ApiPaths.HumanResources.VW_ANALYTICS_AFASTAMENTOS,
                    "criticalityLevel",
                    "criticalityLevel",
                    "criticalityLevel",
                    "criticalityLevel",
                    List.of("competenciaBetween", "departamentoId"),
                    new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label")
            ))
            .build();

    private final HrDepartmentScopeAccess departmentScopeAccess;

    public VwAnalyticsAfastamentoService(
            VwAnalyticsAfastamentoRepository repository,
            VwAnalyticsAfastamentoMapper mapper,
            HrDepartmentScopeAccess departmentScopeAccess
    ) {
        super(repository, VwAnalyticsAfastamento.class, mapper::toDto, VwAnalyticsAfastamento::getAnalyticsId);
        this.departmentScopeAccess = departmentScopeAccess;
    }

    @Override
    public StatsSupportMode getGroupByStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsSupportMode getTimeSeriesStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsSupportMode getDistributionStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsSupportMode getComparisonStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsFieldRegistry getStatsFieldRegistry() {
        return STATS_FIELDS;
    }

    public static OptionSourceRegistry optionSources() {
        return OPTION_SOURCES;
    }

    @Override
    public OptionSourceRegistry getOptionSourceRegistry() {
        return OPTION_SOURCES;
    }

    @Override
    protected ResourceFilterAccessScope<VwAnalyticsAfastamento> resolveResourceFilterAccessScope() {
        return departmentScopeAccess.resolveAnalyticsResourceFilterAccessScope();
    }

    @Override
    protected VwAnalyticsAfastamentoFilterDTO normalizeOptionSourceFilter(
            OptionSourceDescriptor descriptor,
            OptionSourceOperation operation,
            VwAnalyticsAfastamentoFilterDTO filter
    ) {
        departmentScopeAccess.requireNominalRead();
        return departmentScopeAccess.applyAnalyticsScope(filter, VwAnalyticsAfastamentoFilterDTO::new);
    }
}
