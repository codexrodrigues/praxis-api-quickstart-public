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

/** Executes the payroll attribution migration against deterministic PostgreSQL facts. */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PayrollAnalyticsPostgresOperationalProofTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    void setUpOperationalSchema() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(Files.readString(Path.of(
                    "src/test/resources/payroll-analytics-lab/postgres-operational-schema.sql")));
            statement.execute(Files.readString(Path.of(
                    "src/test/resources/payroll-analytics-lab/postgres-legacy-payroll-view.sql")));
            statement.execute(Files.readString(Path.of(
                    "db/operational-migrations/V20260714_001__historical_department_assignments.sql")));
            statement.execute(Files.readString(Path.of(
                    "src/test/resources/payroll-analytics-lab/postgres-operational-data.sql")));

            try (ResultSet legacy = statement.executeQuery("""
                    select departamento_id
                    from public.vw_analytics_folha_pagamento
                    where folha_pagamento_id = 101
                    """)) {
                assertTrue(legacy.next());
                assertEquals(2, legacy.getInt(1), "The fixture must reproduce current-department attribution");
            }

            statement.execute(Files.readString(Path.of(
                    "db/operational-migrations/V20260716_003__payroll_analytics_effective_department.sql")));
        }
    }

    @Test
    void shouldAttributePayrollAtCompetenceBoundaryWithoutCurrentDepartmentFallback() throws Exception {
        Map<Integer, Integer> departmentByPayroll = new LinkedHashMap<>();
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     select folha_pagamento_id, departamento_id
                     from public.vw_analytics_folha_pagamento
                     order by folha_pagamento_id
                     """)) {
            while (rows.next()) {
                departmentByPayroll.put(rows.getInt(1), rows.getInt(2));
            }
        }

        assertEquals(Map.of(
                101, 1,
                102, 1,
                103, 3,
                201, 2,
                202, 1,
                204, 4
        ), departmentByPayroll);
        assertFalse(departmentByPayroll.containsKey(205));
    }

    @Test
    void shouldExposeExactMonthlyMassAndPreserveOneBucketPerPayrollFact() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertMass(statement, "2026-06-01", 1, "Operacoes Legadas", "3000.00", "300.00", "2700.00");
            assertMass(statement, "2026-07-01", 1, "Operacoes Legadas", "2100.00", "210.00", "1890.00");
            assertMass(statement, "2026-07-01", 2, "Operacoes Atuais", "1100.00", "110.00", "990.00");
            assertMass(statement, "2026-06-01", 3, "Projeto Encerrado", "3000.00", "300.00", "2700.00");
            assertMass(statement, "2026-07-01", 4, "Nova Unidade", "4000.00", "400.00", "3600.00");

            try (ResultSet result = statement.executeQuery("""
                    select count(*) as rows, count(distinct folha_pagamento_id) as facts
                    from public.vw_analytics_folha_pagamento
                    """)) {
                assertTrue(result.next());
                assertEquals(6, result.getInt("rows"));
                assertEquals(6, result.getInt("facts"));
            }
        }
    }

    private void assertMass(
            Statement statement,
            String competence,
            int departmentId,
            String department,
            String gross,
            String discounts,
            String net
    ) throws Exception {
        try (ResultSet result = statement.executeQuery("""
                select departamento, sum(salario_bruto), sum(total_descontos), sum(salario_liquido)
                from public.vw_analytics_folha_pagamento
                where competencia = date '%s' and departamento_id = %d
                group by departamento
                """.formatted(competence, departmentId))) {
            assertTrue(result.next(), "Missing payroll mass for department " + departmentId + " at " + competence);
            assertEquals(department, result.getString(1));
            assertEquals(gross, result.getBigDecimal(2).toPlainString());
            assertEquals(discounts, result.getBigDecimal(3).toPlainString());
            assertEquals(net, result.getBigDecimal(4).toPlainString());
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
