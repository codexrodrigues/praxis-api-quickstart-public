package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.DriverManager;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** Real-process PostgreSQL gate for the complete hosted fixture baseline and Flyway reconciliation. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OperationalDatasourceHostedBootstrapEmbeddedPostgresTest {
    private EmbeddedPostgres postgres;
    private String jdbcUrl;

    @BeforeAll
    void startPostgres() throws Exception {
        postgres = EmbeddedPostgres.builder()
                .setCleanDataDirectory(true)
                .setRegisterShutdownHook(false)
                .start();
        jdbcUrl = postgres.getJdbcUrl("postgres", "postgres");
    }

    @AfterAll
    void stopPostgres() throws Exception {
        if (postgres != null) {
            postgres.close();
        }
    }

    @BeforeEach
    void resetSchema() throws Exception {
        try (var connection = DriverManager.getConnection(jdbcUrl, "postgres", "");
                var statement = connection.createStatement()) {
            statement.execute("drop schema public cascade");
            statement.execute("create schema public authorization current_user");
        }
    }

    @Test
    void restoresOnceAndLeavesTheHostedRuntimeSchemaReadyWithoutHibernateDdl() throws Exception {
        var first = OperationalDatasourceMigrator.migrate(
                jdbcUrl, "postgres", "", OperationalDatasourceBootstrapMode.HOSTED_PUBLIC_DEMO_FIXTURE);
        var second = OperationalDatasourceMigrator.migrate(
                jdbcUrl, "postgres", "", OperationalDatasourceBootstrapMode.HOSTED_PUBLIC_DEMO_FIXTURE);

        assertThat(first.migrationsExecuted).isGreaterThan(10);
        assertThat(second.migrationsExecuted).isZero();
        try (var connection = DriverManager.getConnection(jdbcUrl, "postgres", "");
                var statement = connection.createStatement()) {
            assertThat(count(statement, """
                    select count(*) from public.praxis_demo_dataset_guard
                    where dataset_key='praxis-public-demo'
                      and dataset_fingerprint='praxis-public-demo-2026-07-15'
                    """)).isEqualTo(1L);
            assertThat(count(statement, """
                    select count(*) from information_schema.tables
                    where table_schema='public' and table_name in (
                      'funcionarios','enderecos','folhas_pagamento',
                      'funcionario_lotacoes_departamento',
                      'rule_lab_authoritative_benefit_facts',
                      'rule_execution_observation_outbox')
                    """)).isEqualTo(6L);
            assertThat(count(statement,
                    "select count(*) from public.praxis_api_schema_history where success"))
                    .isEqualTo(first.migrationsExecuted + 1L);
            assertThat(booleanValue(statement, """
                    select has_function_privilege(
                      'postgres', 'public.hr_absence_criticality_level(bigint)', 'EXECUTE')
                    """)).isTrue();
            assertThat(booleanValue(statement, """
                    select has_table_privilege(
                      'postgres', 'public.vw_analytics_afastamentos', 'SELECT')
                    """)).isTrue();
        }
    }

    @Test
    void serializesConcurrentBootstrapWithoutAcceptingAPartialDump() throws Exception {
        Callable<Integer> bootstrap = () -> OperationalDatasourceMigrator.migrate(
                jdbcUrl, "postgres", "", OperationalDatasourceBootstrapMode.HOSTED_PUBLIC_DEMO_FIXTURE)
                .migrationsExecuted;

        try (var executor = Executors.newFixedThreadPool(2)) {
            var left = executor.submit(bootstrap);
            var right = executor.submit(bootstrap);
            assertThat(left.get() + right.get()).isGreaterThan(10);
        }
        try (var connection = DriverManager.getConnection(jdbcUrl, "postgres", "");
                var statement = connection.createStatement()) {
            assertThat(count(statement, "select count(*) from public.praxis_demo_dataset_guard"))
                    .isEqualTo(1L);
            assertThat(count(statement, "select count(*) from public.praxis_api_schema_history where success"))
                    .isGreaterThan(10L);
        }
    }

    @Test
    void rejectsANonemptySchemaWithoutTheExactFixtureFingerprint() throws Exception {
        try (var connection = DriverManager.getConnection(jdbcUrl, "postgres", "");
                var statement = connection.createStatement()) {
            statement.execute("create table corporate_owned_data(id bigint primary key)");
        }

        assertThatThrownBy(() -> OperationalDatasourceMigrator.migrate(
                jdbcUrl, "postgres", "", OperationalDatasourceBootstrapMode.HOSTED_PUBLIC_DEMO_FIXTURE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires an empty schema or the exact governed fixture fingerprint");
        try (var connection = DriverManager.getConnection(jdbcUrl, "postgres", "");
                var statement = connection.createStatement()) {
            assertThat(count(statement, "select count(*) from corporate_owned_data")).isZero();
            assertThat(count(statement, """
                    select count(*) from information_schema.tables
                    where table_schema='public' and table_name <> 'corporate_owned_data'
                    """)).isZero();
        }
    }

    private static long count(java.sql.Statement statement, String sql) throws Exception {
        try (var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }

    private static boolean booleanValue(java.sql.Statement statement, String sql) throws Exception {
        try (var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getBoolean(1);
        }
    }
}
