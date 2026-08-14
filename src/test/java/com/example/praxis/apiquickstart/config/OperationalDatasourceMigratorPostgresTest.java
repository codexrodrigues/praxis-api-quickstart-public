package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** PostgreSQL proof that the deploy-time Flyway lane is scoped, repeatable and fail closed. */
@Testcontainers(disabledWithoutDocker = true)
class OperationalDatasourceMigratorPostgresTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void baselinesExistingOperationalSchemaMigratesOnceAndEnforcesTheEntityContract() throws Exception {
        resetSchema();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("create table existing_operational_fixture(id bigint primary key)");
        }

        var first = OperationalDatasourceMigrator.migrate(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var second = OperationalDatasourceMigrator.migrate(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        assertThat(first.migrationsExecuted).isEqualTo(2);
        assertThat(second.migrationsExecuted).isZero();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            assertThat(count(statement,
                    "select count(*) from public.praxis_api_schema_history where success"))
                    .isEqualTo(3L); // baseline + V20260813_001 + V20260814_001
            assertThat(count(statement, """
                    select count(*) from pg_indexes
                    where schemaname='public'
                      and indexname in (
                        'idx_rule_execution_observation_outbox_dispatch',
                        'idx_rule_execution_observation_outbox_expired_lease')
                    """)).isEqualTo(2L);
            assertThat(text(statement, """
                    select data_type from information_schema.columns
                    where table_schema='public'
                      and table_name='rule_execution_observation_outbox'
                      and column_name='snapshot_content_hash'
                    """)).isEqualTo("character varying");
            assertThatThrownBy(() -> statement.execute("""
                    insert into public.rule_execution_observation_outbox(
                        observation_id, tenant_id, environment, snapshot_key,
                        snapshot_content_hash, activation_revision, outcome, duration_micros,
                        observed_at, delivery_status, delivery_attempts, next_attempt_at, created_at)
                    values (gen_random_uuid(), 'tenant-a', 'prod', 'snapshot-v1',
                        'INVALID', 1, 'ALLOW', 10, now(), 'PENDING', 0, now(), now())
                    """)).hasMessageContaining("ck_rule_execution_observation_outbox_hash");
        }
    }

    @Test
    void bootstrapsTheVersionedHostedFixtureOnceAndMigratesItToTheCurrentSchema() throws Exception {
        resetSchema();

        var first = OperationalDatasourceMigrator.migrate(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(),
                OperationalDatasourceBootstrapMode.HOSTED_PUBLIC_DEMO_FIXTURE);
        var second = OperationalDatasourceMigrator.migrate(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(),
                OperationalDatasourceBootstrapMode.HOSTED_PUBLIC_DEMO_FIXTURE);

        assertThat(first.migrationsExecuted).isGreaterThan(10);
        assertThat(second.migrationsExecuted).isZero();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            assertThat(count(statement, """
                    select count(*) from public.praxis_demo_dataset_guard
                    where dataset_key='praxis-public-demo'
                      and dataset_fingerprint='praxis-public-demo-2026-07-15'
                    """)).isEqualTo(1L);
            assertThat(count(statement, "select count(*) from public.funcionarios")).isGreaterThan(0L);
            assertThat(count(statement, "select count(*) from public.enderecos")).isGreaterThan(0L);
            assertThat(count(statement, "select count(*) from public.praxis_api_schema_history where success"))
                    .isEqualTo(first.migrationsExecuted + 1L);
            assertThat(count(statement, """
                    select count(*) from information_schema.tables
                    where table_schema='public' and table_name in (
                      'funcionario_lotacoes_departamento',
                      'rule_lab_authoritative_benefit_facts',
                      'rule_execution_observation_outbox')
                    """)).isEqualTo(3L);
        }
    }

    @Test
    void serializesConcurrentHostedBootstrapAndLeavesOneVerifiedFixture() throws Exception {
        resetSchema();
        Callable<Integer> bootstrap = () -> OperationalDatasourceMigrator.migrate(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(),
                OperationalDatasourceBootstrapMode.HOSTED_PUBLIC_DEMO_FIXTURE).migrationsExecuted;

        try (var executor = Executors.newFixedThreadPool(2)) {
            var left = executor.submit(bootstrap);
            var right = executor.submit(bootstrap);
            assertThat(left.get() + right.get()).isGreaterThan(10);
        }
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            assertThat(count(statement, "select count(*) from public.praxis_demo_dataset_guard"))
                    .isEqualTo(1L);
            assertThat(count(statement, "select count(*) from public.praxis_api_schema_history where success"))
                    .isGreaterThan(10L);
        }
    }

    @Test
    void failsClosedWithoutChangingAnAmbiguousNonFixtureSchema() throws Exception {
        resetSchema();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("create table corporate_owned_data(id bigint primary key)");
        }

        assertThatThrownBy(() -> OperationalDatasourceMigrator.migrate(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(),
                OperationalDatasourceBootstrapMode.HOSTED_PUBLIC_DEMO_FIXTURE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires an empty schema or the exact governed fixture fingerprint");
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            assertThat(count(statement, "select count(*) from corporate_owned_data")).isZero();
            assertThat(count(statement, """
                    select count(*) from information_schema.tables
                    where table_schema='public' and table_name <> 'corporate_owned_data'
                    """)).isZero();
        }
    }

    private static void resetSchema() throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("drop schema public cascade");
            statement.execute("create schema public authorization current_user");
        }
    }

    private static long count(java.sql.Statement statement, String sql) throws Exception {
        try (var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }

    private static String text(java.sql.Statement statement, String sql) throws Exception {
        try (var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }
}
