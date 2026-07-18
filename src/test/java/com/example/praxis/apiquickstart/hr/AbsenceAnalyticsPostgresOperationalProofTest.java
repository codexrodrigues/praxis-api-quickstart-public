package com.example.praxis.apiquickstart.hr;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executes the operational PostgreSQL migrations against synthetic G2 data. */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AbsenceAnalyticsPostgresOperationalProofTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    void setUpOperationalSchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute(Files.readString(Path.of("src/test/resources/absence-analytics-lab/postgres-operational-schema.sql")));
            statement.execute(Files.readString(Path.of("db/operational-migrations/V20260714_001__historical_department_assignments.sql")));
            statement.execute(Files.readString(Path.of("src/test/resources/absence-analytics-lab/postgres-operational-data.sql")));
            statement.execute(Files.readString(Path.of("db/operational-migrations/V20260715_005__absence_analytics_unique_days_policy.sql")));
        }
    }

    @Test
    void shouldMaterializeUniqueDaysByEffectiveDepartmentAndFailClosedWithoutAssignment() throws Exception {
        Map<String, Integer> daysByAnalyticsId = new LinkedHashMap<>();
        Map<String, String> criticalityByAnalyticsId = new LinkedHashMap<>();
        Map<String, String> policyByAnalyticsId = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     select analytics_id, dias_afastado, criticality_level,
                            criticality_policy_id, criticality_policy_version
                     from public.vw_analytics_afastamentos
                     order by analytics_id
                     """)) {
            while (rows.next()) {
                daysByAnalyticsId.put(rows.getString("analytics_id"), rows.getInt("dias_afastado"));
                criticalityByAnalyticsId.put(rows.getString("analytics_id"), rows.getString("criticality_level"));
                policyByAnalyticsId.put(
                        rows.getString("analytics_id"),
                        rows.getString("criticality_policy_id") + ":" + rows.getString("criticality_policy_version")
                );
            }
        }

        assertEquals(Map.of(
                "100:1:202607", 20,
                "101:1:202607", 2,
                "101:2:202607", 2
        ), daysByAnalyticsId);
        assertEquals("CRITICAL", criticalityByAnalyticsId.get("100:1:202607"));
        assertEquals("hr-absence-criticality-v1:2026-07-15", policyByAnalyticsId.get("100:1:202607"));
        assertFalse(daysByAnalyticsId.containsKey("102:1:202607"));
    }

    @Test
    void shouldPublishPolicyFunctionAndRuntimeGrants() throws Exception {
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     select
                        public.hr_absence_criticality_level(6) as standard_level,
                        public.hr_absence_criticality_level(7) as attention_level,
                        public.hr_absence_criticality_level(15) as critical_level,
                        has_function_privilege('praxis_service_user', 'public.hr_absence_criticality_level(bigint)', 'EXECUTE') as function_grant,
                        has_table_privilege('praxis_service_user', 'public.vw_analytics_afastamentos', 'SELECT') as view_grant
                     """)) {
            assertTrue(result.next());
            assertEquals("STANDARD", result.getString("standard_level"));
            assertEquals("ATTENTION", result.getString("attention_level"));
            assertEquals("CRITICAL", result.getString("critical_level"));
            assertTrue(result.getBoolean("function_grant"));
            assertTrue(result.getBoolean("view_grant"));
        }
    }
}
