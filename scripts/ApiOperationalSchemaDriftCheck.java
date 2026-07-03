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
