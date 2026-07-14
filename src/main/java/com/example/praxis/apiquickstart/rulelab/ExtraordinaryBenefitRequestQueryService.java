package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartReadOnlyService;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestFilter;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestResponse;
import java.util.Optional;
import java.util.OptionalLong;
import org.praxisplatform.uischema.capability.ResourceStateSnapshot;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

@Service
public class ExtraordinaryBenefitRequestQueryService extends AbstractQuickstartReadOnlyService<
        ExtraordinaryBenefitRequestEntity, ExtraordinaryBenefitRequestResponse, Long,
        ExtraordinaryBenefitRequestFilter> {
    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("lifecycleStatus", "lifecycleStatus", java.util.Set.of(StatsMetric.COUNT))
            .numericHistogramMeasureField("recommendedAmount", "recommendedAmount")
            .temporalTimeSeriesField("evaluatedAt", "evaluatedAt")
            .build();

    private final ExtraordinaryBenefitRequestRepository repository;

    public ExtraordinaryBenefitRequestQueryService(
            ExtraordinaryBenefitRequestRepository repository,
            ExtraordinaryBenefitRequestMapper mapper) {
        super(repository, ExtraordinaryBenefitRequestEntity.class, mapper::toResponse,
                ExtraordinaryBenefitRequestEntity::getId);
        this.repository = repository;
    }

    public Optional<ResourceStateSnapshot> resolveStateSnapshot(Object resourceId) {
        Long id;
        try {
            id = resourceId instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(resourceId));
        } catch (RuntimeException invalidId) {
            return Optional.empty();
        }
        return repository.findById(id)
                .map(ExtraordinaryBenefitRequestEntity::getLifecycleStatus)
                .map(Enum::name)
                .map(ResourceStateSnapshot::of);
    }

    @Override
    public OptionalLong getResourceVersion(Long id) {
        return repository.findById(id)
                .map(ExtraordinaryBenefitRequestEntity::getVersion)
                .map(OptionalLong::of)
                .orElseGet(OptionalLong::empty);
    }

    @Override public StatsSupportMode getGroupByStatsSupportMode() { return StatsSupportMode.AUTO; }
    @Override public StatsSupportMode getDistributionStatsSupportMode() { return StatsSupportMode.AUTO; }
    @Override public StatsSupportMode getTimeSeriesStatsSupportMode() { return StatsSupportMode.AUTO; }
    @Override public StatsFieldRegistry getStatsFieldRegistry() { return STATS_FIELDS; }
}
