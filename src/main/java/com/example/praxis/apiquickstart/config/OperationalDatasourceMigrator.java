package com.example.praxis.apiquickstart.config;

import java.util.Map;
import java.util.Objects;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

/**
 * Deploy-time migration entry point for the Quickstart-owned operational datasource.
 *
 * <p>This command is deliberately separate from Spring Boot startup and from the Config Starter's
 * {@code spring.flyway.*} instance. Production deployments should invoke it as a pre-deploy step
 * with a migration identity; the application runtime therefore does not gain an implicit DDL
 * capability.</p>
 */
public final class OperationalDatasourceMigrator {
    static final String LOCATION = "classpath:db/operational-runtime-migrations";
    static final String HISTORY_TABLE = "praxis_api_schema_history";
    static final String EXISTING_SCHEMA_BASELINE_VERSION = "20260813";
    static final String HOSTED_FIXTURE_BASELINE_VERSION = "20260701";
    static final String BOOTSTRAP_MODE = "PRAXIS_OPERATIONAL_BOOTSTRAP_MODE";
    static final String RUNTIME_ROLE = "OPERATIONAL_RUNTIME_ROLE";
    static final String MIGRATION_URL = "OPERATIONAL_MIGRATION_DATASOURCE_URL";
    static final String MIGRATION_USERNAME = "OPERATIONAL_MIGRATION_DATASOURCE_USERNAME";
    static final String MIGRATION_PASSWORD = "OPERATIONAL_MIGRATION_DATASOURCE_PASSWORD";

    private OperationalDatasourceMigrator() {}

    public static void main(String[] args) {
        MigrateResult result = migrate(System.getenv());
        System.out.printf(
                "Operational datasource migrations complete: initial=%s target=%s executed=%d%n",
                result.initialSchemaVersion,
                result.targetSchemaVersion,
                result.migrationsExecuted);
    }

    static MigrateResult migrate(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        String url = required(environment, MIGRATION_URL);
        String username = required(environment, MIGRATION_USERNAME);
        String password = required(environment, MIGRATION_PASSWORD);
        OperationalDatasourceBootstrapMode mode = OperationalDatasourceBootstrapMode.parse(
                environment.get(BOOTSTRAP_MODE));
        String runtimeRole = required(environment, RUNTIME_ROLE);
        return migrate(url, username, password, runtimeRole, mode);
    }

    static MigrateResult migrate(String url, String username, String password) {
        return migrate(url, username, password, OperationalDatasourceBootstrapMode.EXISTING_SCHEMA);
    }

    static MigrateResult migrate(
            String url,
            String username,
            String password,
            OperationalDatasourceBootstrapMode mode) {
        return migrate(url, username, password, username, mode);
    }

    static MigrateResult migrate(
            String url,
            String username,
            String password,
            String runtimeRole,
            OperationalDatasourceBootstrapMode mode) {
        Objects.requireNonNull(mode, "mode");
        String requiredUrl = required(url, "url");
        String requiredUsername = required(username, "username");
        String requiredPassword = Objects.requireNonNull(password, "password");
        String requiredRuntimeRole = required(runtimeRole, "runtimeRole");
        if (mode == OperationalDatasourceBootstrapMode.HOSTED_PUBLIC_DEMO_FIXTURE) {
            PublicDemoOperationalBootstrap.bootstrap(requiredUrl, requiredUsername, requiredPassword);
        }
        return Flyway.configure()
                .dataSource(requiredUrl, requiredUsername, requiredPassword)
                .locations(LOCATION)
                .defaultSchema("public")
                .schemas("public")
                .table(HISTORY_TABLE)
                .placeholders(Map.of("OPERATIONAL_RUNTIME_ROLE", quoteIdentifier(requiredRuntimeRole)))
                .baselineOnMigrate(true)
                .baselineVersion(mode == OperationalDatasourceBootstrapMode.HOSTED_PUBLIC_DEMO_FIXTURE
                        ? HOSTED_FIXTURE_BASELINE_VERSION
                        : EXISTING_SCHEMA_BASELINE_VERSION)
                .validateMigrationNaming(true)
                .cleanDisabled(true)
                .connectRetries(3)
                .load()
                .migrate();
    }

    static String quoteIdentifier(String identifier) {
        if (identifier.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("runtimeRole contains an invalid identifier character");
        }
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for operational migration");
        }
        return value.trim();
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
