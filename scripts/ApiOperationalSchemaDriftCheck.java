import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class ApiOperationalSchemaDriftCheck {
    private ApiOperationalSchemaDriftCheck() {
    }

    public static void main(String[] args) throws Exception {
        Properties app = loadProperties(Path.of("src/main/resources/application.properties"));
        String url = resolve("SPRING_DATASOURCE_URL", "spring.datasource.url", app);
        String username = resolve("SPRING_DATASOURCE_USERNAME", "spring.datasource.username", app);
        String password = resolve("SPRING_DATASOURCE_PASSWORD", "spring.datasource.password", app);

        Class.forName("org.postgresql.Driver");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            List<String> failures = new ArrayList<>();
            checkTable(connection, "public", "legacy_pay_codes", failures);
            checkColumn(connection, "public", "legacy_pay_codes", "code", failures);
            checkColumn(connection, "public", "legacy_pay_codes", "description", failures);
            checkColumn(connection, "public", "legacy_pay_codes", "payroll_category", failures);
            checkColumn(connection, "public", "legacy_pay_codes", "status", failures);
            checkColumn(connection, "public", "legacy_pay_codes", "active", failures);
            checkColumn(connection, "public", "eventos_folha", "status", failures);
            checkColumn(connection, "public", "eventos_folha", "version", failures);
            checkColumn(connection, "public", "funcionarios", "version", failures);
            checkTable(connection, "public", "praxis_resource_action_transition", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "transition_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "resource_key", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "resource_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "action_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "performed_at", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "actor_subject", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "correlation_id", failures);
            checkTable(connection, "public", "praxis_resource_action_execution", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "execution_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "resource_key", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "resource_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "action_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "action_scope", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "idempotency_key", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "request_hash", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "execution_status", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "response_payload", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "correlation_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "actor_subject", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "started_at", failures);
            checkTable(connection, "public", "procurement_purchase_orders", failures);
            checkColumn(connection, "public", "procurement_purchase_orders", "status", failures);
            checkColumn(connection, "public", "procurement_purchase_orders", "disabled_reason", failures);
            checkColumn(connection, "public", "procurement_purchase_orders", "approved_at", failures);
            checkColumn(connection, "public", "procurement_purchase_orders", "cancelled_at", failures);
            checkColumn(connection, "public", "procurement_purchase_orders", "received_at", failures);
            checkTable(connection, "public", "extraordinary_benefit_request", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "request_reference", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "lifecycle_status", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "effect_status", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "snapshot_content_hash", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "evaluation_reason_codes", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "version", failures);
            checkTable(connection, "public", "extraordinary_benefit_grant_effect", failures);
            checkColumn(connection, "public", "extraordinary_benefit_grant_effect", "effect_execution_id", failures);
            checkColumn(connection, "public", "extraordinary_benefit_grant_effect", "benefit_request_id", failures);
            checkDistinctCount(
                    connection,
                    "public.procurement_purchase_orders",
                    "status",
                    2,
                    "Expected procurement purchase orders to expose more than one lifecycle status for cockpit charts.",
                    failures
            );

            if (!failures.isEmpty()) {
                System.err.println("API operational datasource drift detected:");
                for (String failure : failures) {
                    System.err.println("- " + failure);
                }
                System.exit(1);
            }

            System.out.println("API operational datasource drift check passed.");
        }
    }

    private static void checkTable(Connection connection, String schema, String table, List<String> failures)
            throws SQLException {
        if (!exists(connection, """
                select 1
                from information_schema.tables
                where table_schema = ?
                  and table_name = ?
                """, schema, table)) {
            failures.add("Missing table " + schema + "." + table);
        }
    }

    private static void checkColumn(Connection connection, String schema, String table, String column,
            List<String> failures) throws SQLException {
        if (!exists(connection, """
                select 1
                from information_schema.columns
                where table_schema = ?
                  and table_name = ?
                  and column_name = ?
                """, schema, table, column)) {
            failures.add("Missing column " + schema + "." + table + "." + column);
        }
    }

    private static boolean exists(Connection connection, String sql, String... args) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setString(i + 1, args[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void checkDistinctCount(
            Connection connection,
            String table,
            String column,
            int minimum,
            String message,
            List<String> failures
    ) throws SQLException {
        String sql = "select count(distinct " + column + ") from " + table;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next() && resultSet.getInt(1) < minimum) {
                failures.add(message);
            }
        }
    }

    private static Properties loadProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static String resolve(String envName, String propertyName, Properties properties) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }

        String value = properties.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + envName + " or " + propertyName);
        }

        if (value.startsWith("${") && value.endsWith("}")) {
            int separator = value.indexOf(':');
            if (separator > 2) {
                return value.substring(separator + 1, value.length() - 1);
            }
        }
        return value;
    }
}
