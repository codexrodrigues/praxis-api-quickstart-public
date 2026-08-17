package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class OperationalDatasourceMigratorTest {
    @Test
    void refusesToRunWithoutAnExplicitOperationalIdentity() {
        assertThatThrownBy(() -> OperationalDatasourceMigrator.migrate(Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "OPERATIONAL_MIGRATION_DATASOURCE_URL is required for operational migration");
    }

    @Test
    void doesNotReuseTheRuntimeDatasourceAsTheMigrationIdentity() {
        assertThatThrownBy(() -> OperationalDatasourceMigrator.migrate(Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://runtime.invalid/runtime",
                "SPRING_DATASOURCE_USERNAME", "runtime",
                "SPRING_DATASOURCE_PASSWORD", "fixture",
                OperationalDatasourceMigrator.RUNTIME_ROLE, "runtime")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "OPERATIONAL_MIGRATION_DATASOURCE_URL is required for operational migration");
    }

    @Test
    void rejectsUnknownBootstrapPolicyBeforeConnecting() {
        assertThatThrownBy(() -> OperationalDatasourceMigrator.migrate(Map.of(
                OperationalDatasourceMigrator.MIGRATION_URL,
                "jdbc:postgresql://invalid-config-host.invalid/invalid",
                OperationalDatasourceMigrator.MIGRATION_USERNAME, "fixture",
                OperationalDatasourceMigrator.MIGRATION_PASSWORD, "fixture",
                OperationalDatasourceMigrator.RUNTIME_ROLE, "fixture",
                OperationalDatasourceMigrator.BOOTSTRAP_MODE, "automatic")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unsupported PRAXIS_OPERATIONAL_BOOTSTRAP_MODE: automatic");
    }

    @Test
    void requiresTheRuntimeRoleBeforeConnecting() {
        assertThatThrownBy(() -> OperationalDatasourceMigrator.migrate(Map.of(
                OperationalDatasourceMigrator.MIGRATION_URL,
                "jdbc:postgresql://invalid-config-host.invalid/invalid",
                OperationalDatasourceMigrator.MIGRATION_USERNAME, "migration",
                OperationalDatasourceMigrator.MIGRATION_PASSWORD, "fixture")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OPERATIONAL_RUNTIME_ROLE is required for operational migration");
    }

    @Test
    void quotesTheRuntimeRoleAsAnIdentifierRatherThanSql() {
        assertThat(OperationalDatasourceMigrator.quoteIdentifier("runtime-role"))
                .isEqualTo("\"runtime-role\"");
        assertThat(OperationalDatasourceMigrator.quoteIdentifier("runtime\"role"))
                .isEqualTo("\"runtime\"\"role\"");
        assertThatThrownBy(() -> OperationalDatasourceMigrator.quoteIdentifier("runtime\0role"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
