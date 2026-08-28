package com.example.praxis.apiquickstart.procurement.service;

import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartReadOnlyService;
import com.example.praxis.apiquickstart.procurement.dto.VwSupplierProcurementFunnelDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.VwSupplierProcurementFunnelFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.VwSupplierProcurementFunnel;
import com.example.praxis.apiquickstart.procurement.mapper.VwSupplierProcurementFunnelMapper;
import com.example.praxis.apiquickstart.procurement.repository.VwSupplierProcurementFunnelRepository;
import java.util.Set;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

/** Publica o dataset cumulativo como resource read-only com stats nativos. */
@Service
public class VwSupplierProcurementFunnelService extends AbstractQuickstartReadOnlyService<
        VwSupplierProcurementFunnel, VwSupplierProcurementFunnelDTO, String,
        VwSupplierProcurementFunnelFilterDTO> {

    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .labeledGroupByBucket("stage", "stageOrder", "stageLabel", Set.of(StatsMetric.SUM))
            .groupByBucket("companyId", "companyId", Set.of(StatsMetric.SUM))
            .metricField("volume", "volume", Set.of(StatsMetric.SUM, StatsMetric.AVG, StatsMetric.MIN, StatsMetric.MAX))
            .metricField("dropOffCount", "dropOffCount", Set.of(StatsMetric.SUM, StatsMetric.AVG, StatsMetric.MIN, StatsMetric.MAX))
            .metricField("conversionRate", "conversionRate", Set.of(StatsMetric.AVG, StatsMetric.MIN, StatsMetric.MAX))
            .metricField("dropOffRate", "dropOffRate", Set.of(StatsMetric.AVG, StatsMetric.MIN, StatsMetric.MAX))
            .build();

    public VwSupplierProcurementFunnelService(
            VwSupplierProcurementFunnelRepository repository,
            VwSupplierProcurementFunnelMapper mapper
    ) {
        super(repository, VwSupplierProcurementFunnel.class, mapper::toDto,
                VwSupplierProcurementFunnel::getAnalyticsId);
    }

    @Override
    public StatsSupportMode getGroupByStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsFieldRegistry getStatsFieldRegistry() {
        return STATS_FIELDS;
    }
}
