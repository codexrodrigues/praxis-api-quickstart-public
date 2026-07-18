package com.example.praxis.apiquickstart.hr.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AbsenceAnalyticsE2eDatabaseFixtureTest {

    private static final Path GOLDEN_PATH = Path.of(
            "src/test/resources/absence-analytics-lab/absence-analytics-semantic-golden-suite.json");

    @Test
    void derivesTheOperationalFixtureFromCanonicalResourceIdentities() throws Exception {
        AbsenceAnalyticsE2eDatabaseFixture.FixtureScenario scenario =
                AbsenceAnalyticsE2eDatabaseFixture.loadScenario(GOLDEN_PATH, "ALG-01");

        assertThat(scenario.employees().get("E-001").resourceId()).isEqualTo(101);
        assertThat(scenario.employees().get("E-004").resourceId()).isEqualTo(104);
        assertThat(scenario.departments().get("D-ENG").resourceId()).isEqualTo(201);
        assertThat(scenario.departments().get("D-OPS").resourceId()).isEqualTo(202);
        assertThat(scenario.assignments()).hasSize(7);
        assertThat(scenario.absences()).hasSize(11);
    }

    @Test
    void removesOnlyTheTwoProductionRuntimeRoleGrants() {
        String migration = """
                create view public.vw_analytics_afastamentos as select 1;
                grant execute on function public.hr_absence_criticality_level(bigint) to praxis_service_user;
                grant select on public.vw_analytics_afastamentos to praxis_service_user;
                grant select on public.some_other_view to another_role;
                """;

        String fixtureMigration =
                AbsenceAnalyticsE2eDatabaseFixture.withoutRuntimeRoleGrants(migration);

        assertThat(fixtureMigration)
                .contains("create view public.vw_analytics_afastamentos")
                .contains("grant select on public.some_other_view to another_role")
                .doesNotContain("hr_absence_criticality_level(bigint) to praxis_service_user")
                .doesNotContain("vw_analytics_afastamentos to praxis_service_user");
    }

    @Test
    void changesOnlyTheDatabaseSegmentOfTheJdbcUrl() {
        String target = AbsenceAnalyticsE2eDatabaseFixture.replaceDatabaseName(
                "jdbc:postgresql://db.example:5432/shared?sslmode=require",
                "praxis_e2e_absence_abc123");

        assertThat(target).isEqualTo(
                "jdbc:postgresql://db.example:5432/praxis_e2e_absence_abc123?sslmode=require");
        assertThatThrownBy(() -> AbsenceAnalyticsE2eDatabaseFixture.replaceDatabaseName(
                "jdbc:postgresql://db.example/shared",
                "shared"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsafe disposable database name");
    }
}
