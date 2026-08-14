package com.example.praxis.apiquickstart.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.postgresql.PGConnection;

/**
 * Restores the versioned public-demo dump only for an explicitly requested, empty hosted fixture.
 *
 * <p>The public dump remains the single baseline source. This adapter removes only pg_dump's
 * database-switching commands and maps its four documented local roles to the authenticated
 * migration role. PostgreSQL COPY blocks are streamed through the driver. The complete restore is
 * protected by a transaction-scoped advisory lock, so a failed or concurrent attempt cannot leave
 * a partially accepted fixture.</p>
 */
final class PublicDemoOperationalBootstrap {
    static final String RESOURCE = "db/operational-bootstrap/public-demo-seed.sql";
    static final String DATASET_KEY = "praxis-public-demo";
    static final String DATASET_FINGERPRINT = "praxis-public-demo-2026-07-15";
    private static final long ADVISORY_LOCK = 0x5052415849534442L;
    private static final Pattern COPY = Pattern.compile("^COPY\\s+.+\\s+FROM\\s+stdin;$", Pattern.CASE_INSENSITIVE);
    private static final Pattern OWNER = Pattern.compile(
            "^(ALTER\\s+.+\\s+OWNER\\s+TO\\s+)([a-zA-Z_][a-zA-Z0-9_]*)(;\\s*)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern GRANT_ROLE = Pattern.compile(
            "^(GRANT\\s+.+\\s+TO\\s+)([a-zA-Z_][a-zA-Z0-9_]*)(.*;\\s*)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DEFAULT_PRIVILEGES = Pattern.compile(
            "^(ALTER\\s+DEFAULT\\s+PRIVILEGES\\s+FOR\\s+ROLE\\s+)([a-zA-Z_][a-zA-Z0-9_]*)(.*\\s+TO\\s+)([a-zA-Z_][a-zA-Z0-9_]*)(.*;\\s*)$",
            Pattern.CASE_INSENSITIVE);
    private static final List<String> DUMP_ROLES = List.of(
            "praxis_demo_owner", "praxis_service_user", "praxis_demo_superuser", "cloud_admin");

    private PublicDemoOperationalBootstrap() {}

    static void bootstrap(String url, String username, String password) {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            try {
                lock(connection);
                FixtureState state = inspect(connection);
                if (state == FixtureState.RECOGNIZED) {
                    connection.commit();
                    return;
                }
                if (state != FixtureState.EMPTY) {
                    throw new IllegalStateException(
                            "Hosted public-demo bootstrap requires an empty schema or the exact governed fixture fingerprint");
                }
                restore(connection, username, resource());
                verify(connection);
                connection.commit();
            } catch (Exception failure) {
                rollback(connection, failure);
                if (failure instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException("Could not bootstrap hosted public-demo fixture", failure);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not connect to operational datasource for hosted fixture bootstrap", exception);
        }
    }

    static void restore(Connection connection, String migrationRole, InputStream dump) throws Exception {
        Objects.requireNonNull(connection, "connection");
        String quotedRole = quoteIdentifier(migrationRole);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(dump, "dump"), StandardCharsets.UTF_8))) {
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\\")) {
                    if (!isSupportedMetaCommand(line)) {
                        throw new IllegalStateException("Unsupported pg_dump meta-command in operational baseline");
                    }
                    continue;
                }
                if (isDatabaseStatement(line)) {
                    continue;
                }
                if (isUnsupportedPortableSessionSetting(line)) {
                    continue;
                }
                if (COPY.matcher(line).matches()) {
                    String copyCommand = line;
                    executeSql(connection, sql);
                    StringBuilder data = new StringBuilder();
                    boolean terminated = false;
                    while ((line = reader.readLine()) != null) {
                        if ("\\.".equals(line)) {
                            terminated = true;
                            break;
                        }
                        data.append(line).append('\n');
                    }
                    if (!terminated) {
                        throw new IllegalStateException("Unterminated COPY block in operational baseline");
                    }
                    connection.unwrap(PGConnection.class).getCopyAPI()
                            .copyIn(copyCommand, new StringReader(data.toString()));
                    continue;
                }
                sql.append(rewriteRole(line, quotedRole)).append('\n');
            }
            executeSql(connection, sql);
        }
    }

    private static InputStream resource() {
        InputStream stream = PublicDemoOperationalBootstrap.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing packaged operational baseline: " + RESOURCE);
        }
        return stream;
    }

    private static boolean isSupportedMetaCommand(String line) {
        return line.startsWith("\\restrict ")
                || line.startsWith("\\unrestrict ")
                || line.equals("\\connect praxis_demo");
    }

    private static boolean isDatabaseStatement(String line) {
        String normalized = line.stripLeading().toUpperCase();
        return normalized.startsWith("DROP DATABASE ")
                || normalized.startsWith("CREATE DATABASE ")
                || normalized.startsWith("ALTER DATABASE ")
                || (normalized.startsWith("GRANT ") && normalized.contains(" ON DATABASE "));
    }

    /**
     * PostgreSQL 17 pg_dump emits this session setting, but the hosted fixture also supports the
     * PostgreSQL 14 baseline used by the real-process CI gate. It has no effect on restored data.
     * Keep this as an exact allowlist rather than accepting arbitrary SET statements from a dump.
     */
    static boolean isUnsupportedPortableSessionSetting(String line) {
        return "SET transaction_timeout = 0;".equals(line.strip());
    }

    static String rewriteRole(String line, String quotedMigrationRole) {
        Matcher defaults = DEFAULT_PRIVILEGES.matcher(line);
        if (defaults.matches()) {
            requireKnownRole(defaults.group(2));
            requireKnownRole(defaults.group(4));
            return defaults.group(1) + quotedMigrationRole + defaults.group(3)
                    + quotedMigrationRole + defaults.group(5);
        }
        Matcher owner = OWNER.matcher(line);
        if (owner.matches()) {
            requireKnownRole(owner.group(2));
            return owner.group(1) + quotedMigrationRole + owner.group(3);
        }
        Matcher grant = GRANT_ROLE.matcher(line);
        if (grant.matches()) {
            requireKnownRole(grant.group(2));
            return grant.group(1) + quotedMigrationRole + grant.group(3);
        }
        return line;
    }

    private static void requireKnownRole(String role) {
        if (DUMP_ROLES.stream().noneMatch(candidate -> candidate.equalsIgnoreCase(role))) {
            throw new IllegalStateException("Public-demo dump references an ungoverned role");
        }
    }

    private static String quoteIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank() || identifier.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("migration role is required");
        }
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    private static void executeSql(Connection connection, StringBuilder sql) throws SQLException {
        if (sql.toString().isBlank()) {
            sql.setLength(0);
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql.toString());
        }
        sql.setLength(0);
    }

    private static void lock(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("select pg_advisory_xact_lock(?)")) {
            statement.setLong(1, ADVISORY_LOCK);
            statement.execute();
        }
    }

    private static FixtureState inspect(Connection connection) throws SQLException {
        if (relationExists(connection, "praxis_demo_dataset_guard")) {
            verify(connection);
            return FixtureState.RECOGNIZED;
        }
        try (var statement = connection.prepareStatement("""
                select count(*) from pg_class c
                join pg_namespace n on n.oid=c.relnamespace
                where n.nspname='public' and c.relkind in ('r','p','v','m','S','f')
                """); var result = statement.executeQuery()) {
            result.next();
            return result.getLong(1) == 0 ? FixtureState.EMPTY : FixtureState.AMBIGUOUS;
        }
    }

    private static void verify(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("""
                select count(*) from public.praxis_demo_dataset_guard
                where dataset_key=? and dataset_fingerprint=?
                """)) {
            statement.setString(1, DATASET_KEY);
            statement.setString(2, DATASET_FINGERPRINT);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                if (result.getLong(1) != 1) {
                    throw new IllegalStateException("Hosted public-demo fingerprint does not match the versioned fixture");
                }
            }
        }
        for (String relation : List.of("acordos_regulatorios", "funcionarios", "enderecos", "folhas_pagamento")) {
            if (!relationExists(connection, relation)) {
                throw new IllegalStateException("Hosted public-demo baseline is incomplete");
            }
        }
    }

    private static boolean relationExists(Connection connection, String name) throws SQLException {
        try (var statement = connection.prepareStatement("select to_regclass(?) is not null")) {
            statement.setString(1, "public." + name);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private static void rollback(Connection connection, Exception failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    private enum FixtureState { EMPTY, RECOGNIZED, AMBIGUOUS }
}
