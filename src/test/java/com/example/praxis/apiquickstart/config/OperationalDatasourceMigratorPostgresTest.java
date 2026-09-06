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
            createProcurementFunnelDependencies(statement);
            createAcordosRegulatoriosDependency(statement);
        }

        var first = OperationalDatasourceMigrator.migrate(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var second = OperationalDatasourceMigrator.migrate(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        assertThat(first.migrationsExecuted).isEqualTo(4);
        assertThat(second.migrationsExecuted).isZero();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            assertThat(count(statement,
                    "select count(*) from public.praxis_api_schema_history where success"))
                    .isEqualTo(5L); // baseline + V20260813_001 + V20260814_001 + V20260826_001 + V20260905_001
            assertThat(count(statement, """
                    select count(*) from information_schema.columns
                    where table_schema='public'
                      and table_name='acordos_regulatorios'
                      and column_name='version'
                      and data_type='bigint'
                      and is_nullable='NO'
                      and column_default like '0%'
                    """)).isEqualTo(1L);
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
    void acceptsAnEquivalentReevaluationConstraintFromAnExistingOperationalSchema() throws Exception {
        resetSchema();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("""
                    create table public.extraordinary_benefit_transformation_audit (
                        benefit_request_id bigint not null,
                        proposal_identity_digest varchar(64) not null,
                        facts_digest varchar(64) not null,
                        constraint uq_extraordinary_benefit_transformation_audit_proposal_facts
                            unique (benefit_request_id, proposal_identity_digest, facts_digest)
                    )
                    """);
            createProcurementFunnelDependencies(statement);
            createAcordosRegulatoriosDependency(statement);
        }

        var first = OperationalDatasourceMigrator.migrate(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var second = OperationalDatasourceMigrator.migrate(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        assertThat(first.migrationsExecuted).isEqualTo(4);
        assertThat(second.migrationsExecuted).isZero();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            assertThat(count(statement, """
                    select count(*)
                      from pg_constraint
                     where conrelid = 'public.extraordinary_benefit_transformation_audit'::regclass
                       and conname = 'uq_extraordinary_benefit_transformation_audit_proposal_facts'
                       and contype = 'u'
                    """)).isEqualTo(1L);
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
            assertThat(count(statement, "select count(*) from public.vw_supplier_procurement_funnel"))
                    .isEqualTo(42L);
            assertThat(count(statement, """
                    select count(*)
                    from public.vw_supplier_procurement_funnel current_stage
                    join public.vw_supplier_procurement_funnel previous_stage
                      on previous_stage.company_id = current_stage.company_id
                     and previous_stage.stage_order = current_stage.stage_order - 1
                    where current_stage.volume > previous_stage.volume
                    """)).isZero();
            assertThat(count(statement, """
                    select count(distinct company_id)
                    from public.vw_supplier_procurement_funnel
                    where stage_key = 'received' and volume > 0
                    """)).isGreaterThanOrEqualTo(2L);
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

    private static void createProcurementFunnelDependencies(java.sql.Statement statement) throws Exception {
        statement.execute("""
                create table public.procurement_companies (
                    id integer primary key,
                    legal_name varchar(255) not null
                )
                """);
        statement.execute("""
                create table public.procurement_suppliers (
                    id integer primary key,
                    company_id integer not null,
                    homologation_status varchar(255),
                    status varchar(255)
                )
                """);
        statement.execute("""
                create table public.procurement_contracts (
                    id integer primary key,
                    company_id integer not null,
                    supplier_id integer not null,
                    status varchar(255)
                )
                """);
        statement.execute("""
                create table public.procurement_purchase_orders (
                    id integer primary key,
                    company_id integer not null,
                    supplier_id integer not null,
                    contract_id integer,
                    status varchar(40),
                    received_at date
                )
                """);
    }

    private static void createAcordosRegulatoriosDependency(java.sql.Statement statement) throws Exception {
        statement.execute("""
                create table public.acordos_regulatorios (
                    id bigint primary key,
                    nome varchar(255) not null,
                    jurisdicao varchar(120) not null,
                    status varchar(40) not null,
                    descricao text
                )
                """);
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
