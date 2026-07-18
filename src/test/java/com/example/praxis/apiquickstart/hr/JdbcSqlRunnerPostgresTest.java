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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves PostgreSQL syntax and invocation-wide rollback through the documented runner process. */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JdbcSqlRunnerPostgresTest {

    private static final String CONFIG_DATABASE = "jdbc_runner_config_probe";
    private static final String PASSWORD_MARKER = "runner-secret-password";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withPassword(PASSWORD_MARKER);

    @BeforeAll
    void createSeparateConfigDatabase() throws Exception {
        try (Connection connection = operationalConnection(); Statement statement = connection.createStatement()) {
            statement.execute("drop database if exists " + CONFIG_DATABASE);
            statement.execute("create database " + CONFIG_DATABASE);
        }
    }

    @Test
    void shouldExecuteDollarQuotedScriptsAndFilesInOrderWithoutTouchingConfigDatasource() throws Exception {
        Path first = script("""
                create table public.jdbc_runner_events (
                    sequence_number integer primary key,
                    detail text not null
                );

                do $$
                begin
                    insert into public.jdbc_runner_events values (1, 'plain-do');
                end
                $$;

                create or replace function public.jdbc_runner_detail(prefix text)
                returns text
                language plpgsql
                as $function$
                begin
                    return prefix || ';function';
                end
                $function$;

                insert into public.jdbc_runner_events
                values (2, public.jdbc_runner_detail('ordered'));
                """);
        Path second = script("""
                do $guard$
                begin
                    if not exists (
                        select 1 from public.jdbc_runner_events where sequence_number = 2
                    ) then
                        raise exception 'RUNNER_ORDER_VIOLATION';
                    end if;
                    insert into public.jdbc_runner_events values (3, 'named-dollar-quote');
                end
                $guard$;
                """);

        JdbcSqlRunnerProcess.Execution execution = JdbcSqlRunnerProcess.run(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(),
                configJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(),
                first, second);

        assertEquals(0, execution.exitCode(), execution.output());
        assertTrue(execution.output().contains(first.toAbsolutePath().toString()), execution.output());
        assertTrue(execution.output().contains(second.toAbsolutePath().toString()), execution.output());
        assertFalse(execution.output().contains(POSTGRES.getJdbcUrl()), execution.output());
        assertFalse(execution.output().contains(PASSWORD_MARKER), execution.output());

        try (Connection connection = operationalConnection(); Statement statement = connection.createStatement()) {
            assertEquals(3, scalarLong(statement, "select count(*) from public.jdbc_runner_events"));
            assertEquals("ordered;function", scalarText(statement,
                    "select detail from public.jdbc_runner_events where sequence_number = 2"));
            assertEquals("named-dollar-quote", scalarText(statement,
                    "select detail from public.jdbc_runner_events where sequence_number = 3"));
        }
        try (Connection connection = configConnection(); Statement statement = connection.createStatement()) {
            assertNull(scalarText(statement, "select to_regclass('public.jdbc_runner_events')::text"));
        }
    }

    @Test
    void shouldRollbackEveryFileAndReturnNonZeroWhenLaterScriptFails() throws Exception {
        Path first = script("""
                create table public.jdbc_runner_rollback_probe (id integer primary key);
                insert into public.jdbc_runner_rollback_probe values (1);
                """);
        Path second = script("""
                insert into public.jdbc_runner_rollback_probe values (2);
                do $$
                begin
                    raise exception 'RUNNER_ROLLBACK_PROBE';
                end
                $$;
                insert into public.jdbc_runner_rollback_probe values (3);
                """);

        JdbcSqlRunnerProcess.Execution execution = JdbcSqlRunnerProcess.run(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), first, second);

        assertNotEquals(0, execution.exitCode(), execution.output());
        assertTrue(execution.output().contains("RUNNER_ROLLBACK_PROBE"), execution.output());
        assertFalse(execution.output().contains(POSTGRES.getJdbcUrl()), execution.output());
        assertFalse(execution.output().contains(PASSWORD_MARKER), execution.output());
        try (Connection connection = operationalConnection(); Statement statement = connection.createStatement()) {
            assertNull(scalarText(statement,
                    "select to_regclass('public.jdbc_runner_rollback_probe')::text"));
        }
    }

    private static Path script(String sql) throws Exception {
        Path path = Files.createTempFile("jdbc-sql-runner-proof-", ".sql");
        Files.writeString(path, sql);
        return path;
    }

    private static Connection operationalConnection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Connection configConnection() throws Exception {
        return DriverManager.getConnection(configJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static String configJdbcUrl() {
        String jdbcUrl = POSTGRES.getJdbcUrl();
        int queryStart = jdbcUrl.indexOf('?');
        int databaseEnd = queryStart < 0 ? jdbcUrl.length() : queryStart;
        int databaseStart = jdbcUrl.lastIndexOf('/', databaseEnd - 1) + 1;
        return jdbcUrl.substring(0, databaseStart) + CONFIG_DATABASE + jdbcUrl.substring(databaseEnd);
    }

    private static long scalarLong(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static String scalarText(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }
}
