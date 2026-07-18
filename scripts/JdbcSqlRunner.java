import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class JdbcSqlRunner {
    private JdbcSqlRunner() {
    }

    public static void main(String[] args) {
        try {
            execute(args);
        } catch (Exception exception) {
            System.err.println("JdbcSqlRunner failed: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void execute(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: JdbcSqlRunner <sql-file> [<sql-file>...]");
        }

        Properties app = loadProperties(Path.of("src/main/resources/application.properties"));
        String url = resolve("SPRING_DATASOURCE_URL", "spring.datasource.url", app);
        String username = resolve("SPRING_DATASOURCE_USERNAME", "spring.datasource.username", app);
        String password = resolve("SPRING_DATASOURCE_PASSWORD", "spring.datasource.password", app);

        Class.forName("org.postgresql.Driver");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            try {
                for (String arg : args) {
                    runFile(connection, Path.of(arg));
                }
                connection.commit();
            } catch (Exception exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    private static void runFile(Connection connection, Path path) throws IOException, SQLException {
        String sql = Files.readString(path, StandardCharsets.UTF_8);
        try (Statement statement = connection.createStatement()) {
            consumeResults(statement, statement.execute(sql));
        } catch (SQLException exception) {
            throw new SQLException("Failed to execute SQL file " + path + ": " + exception.getMessage(),
                    exception.getSQLState(), exception.getErrorCode(), exception);
        }
        System.out.println("Executed SQL file " + path);
    }

    private static void consumeResults(Statement statement, boolean resultSetAvailable) throws SQLException {
        while (true) {
            if (resultSetAvailable) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    while (resultSet.next()) {
                        // Drain results without logging data returned by operational scripts.
                    }
                }
            } else if (statement.getUpdateCount() == -1) {
                return;
            }
            resultSetAvailable = statement.getMoreResults(Statement.CLOSE_CURRENT_RESULT);
        }
    }

    private static void rollback(Connection connection, Exception failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
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
