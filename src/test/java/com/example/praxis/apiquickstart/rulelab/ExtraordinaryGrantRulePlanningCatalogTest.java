package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.praxisplatform.config.service.DomainRuleImplementationScope;

class ExtraordinaryGrantRulePlanningCatalogTest {

    @Test
    void planningCatalogMatchesEveryExecutableHostCoordinate() {
        ExtraordinaryGrantRuleLabConfiguration configuration = new ExtraordinaryGrantRuleLabConfiguration();
        var executable = configuration.extraordinaryGrantRuleExecutorRegistry();
        var catalog = configuration.extraordinaryGrantRuleImplementationCatalog("desenv", "local");
        var planning = catalog.allowedImplementations(new DomainRuleImplementationScope(
                "desenv", "local", ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY));

        assertThat(planning).allSatisfy(ref ->
                assertThat(executable.isCompatible(ref.implementationKey(), ref.implementationVersion())).isTrue());
        assertThat(planning).hasSize(3);
        assertThat(catalog.allowedImplementations(new DomainRuleImplementationScope(
                "other", "local", ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY))).isEmpty();
        assertThat(catalog.allowedImplementations(new DomainRuleImplementationScope(
                "desenv", "local", "other-service"))).isEmpty();
    }

    @Test
    void recoveryPublicationChangesOnlyTheImmutableRuleSetVersion() {
        var baseline = ExtraordinaryGrantRuleSetFactory.definition();
        var recovered = ExtraordinaryGrantRuleSetFactory.definition(2);

        assertThat(recovered.ref().version()).isEqualTo(2);
        assertThat(recovered.ref().ruleSetKey()).isEqualTo(baseline.ref().ruleSetKey());
        assertThat(recovered.slots()).isEqualTo(baseline.slots());
        assertThat(recovered.bindings()).isEqualTo(baseline.bindings());
        assertThatThrownBy(() -> ExtraordinaryGrantRuleSetFactory.definition(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }
}
