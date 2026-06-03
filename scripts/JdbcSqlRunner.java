import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class JdbcSqlRunner {
    private JdbcSqlRunner() {
    }

    public static void main(String[] args) throws Exception {
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
            for (String arg : args) {
                runFile(connection, Path.of(arg));
            }
            connection.commit();
        }
    }

    private static void runFile(Connection connection, Path path) throws IOException, SQLException {
        String sql = Files.readString(path, StandardCharsets.UTF_8);
        int count = 0;
        try (Statement statement = connection.createStatement()) {
            for (String command : splitCommands(sql)) {
                if (!command.isBlank()) {
                    boolean hasResultSet = statement.execute(command);
                    if (hasResultSet) {
                        printResultSet(statement.getResultSet());
                    }
                    count++;
                }
            }
        }
        System.out.println("Executed " + count + " command(s) from " + path);
    }

    private static void printResultSet(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        while (resultSet.next()) {
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    row.append(" | ");
                }
                row.append(metadata.getColumnLabel(i)).append('=').append(resultSet.getString(i));
            }
            System.out.println(row);
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

    private static List<String> splitCommands(String sql) {
        List<String> commands = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inLineComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (ch == '\n') {
                    inLineComment = false;
                    current.append(ch);
                }
                continue;
            }

            if (!inSingleQuote && ch == '-' && next == '-') {
                inLineComment = true;
                i++;
                continue;
            }

            if (ch == '\'' && !inLineComment) {
                current.append(ch);
                if (inSingleQuote && next == '\'') {
                    current.append(next);
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (!inSingleQuote && ch == ';') {
                addIfSql(commands, current);
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        addIfSql(commands, current);
        return commands;
    }

    private static void addIfSql(List<String> commands, StringBuilder current) {
        String command = current.toString().trim();
        if (command.isBlank() || command.startsWith("\\")) {
            return;
        }
        commands.add(command);
    }
}
