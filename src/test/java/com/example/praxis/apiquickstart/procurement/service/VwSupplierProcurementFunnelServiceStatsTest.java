package com.example.praxis.apiquickstart.procurement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.example.praxis.apiquickstart.procurement.mapper.VwSupplierProcurementFunnelMapper;
import com.example.praxis.apiquickstart.procurement.repository.VwSupplierProcurementFunnelRepository;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;

class VwSupplierProcurementFunnelServiceStatsTest {

    @Test
    void publishesTheCumulativeStageAndMetricContract() {
        VwSupplierProcurementFunnelService service = new VwSupplierProcurementFunnelService(
                mock(VwSupplierProcurementFunnelRepository.class),
                mock(VwSupplierProcurementFunnelMapper.class));

        assertEquals(StatsSupportMode.AUTO, service.getGroupByStatsSupportMode());
        var stage = service.getStatsFieldRegistry().resolve("stage").orElseThrow();
        assertTrue(stage.groupByEligible());
        assertEquals("stageOrder", stage.keyPropertyPath());
        assertEquals("stageLabel", stage.labelPropertyPath());
        assertTrue(stage.supports(StatsMetric.SUM));
        assertTrue(service.getStatsFieldRegistry().resolve("volume").orElseThrow().supports(StatsMetric.SUM));
        assertTrue(service.getStatsFieldRegistry().resolve("dropOffRate").orElseThrow().supports(StatsMetric.AVG));
    }
}
