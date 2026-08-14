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
                .hasMessage("SPRING_DATASOURCE_URL is required for operational migration");
    }

    @Test
    void rejectsUnknownBootstrapPolicyBeforeConnecting() {
        assertThatThrownBy(() -> OperationalDatasourceMigrator.migrate(Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://invalid-config-host.invalid/invalid",
                "SPRING_DATASOURCE_USERNAME", "fixture",
                "SPRING_DATASOURCE_PASSWORD", "fixture",
                OperationalDatasourceMigrator.RUNTIME_ROLE, "fixture",
                OperationalDatasourceMigrator.BOOTSTRAP_MODE, "automatic")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unsupported PRAXIS_OPERATIONAL_BOOTSTRAP_MODE: automatic");
    }

    @Test
    void requiresTheRuntimeRoleBeforeConnecting() {
        assertThatThrownBy(() -> OperationalDatasourceMigrator.migrate(Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://invalid-config-host.invalid/invalid",
                "SPRING_DATASOURCE_USERNAME", "migration",
                "SPRING_DATASOURCE_PASSWORD", "fixture")))
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
