import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

public final class DomainKnowledgeV18Readiness {
    private static final List<String> DOMAIN_KNOWLEDGE_TABLES = List.of(
            "domain_knowledge_concept",
            "domain_knowledge_alias",
            "domain_knowledge_binding",
            "domain_knowledge_relationship",
            "domain_knowledge_evidence",
            "domain_knowledge_change_set"
    );

    private DomainKnowledgeV18Readiness() {
    }

    public static void main(String[] args) throws Exception {
        Properties app = loadProperties(Path.of("src/main/resources/application.properties"));
        String url = resolve("SPRING_DATASOURCE_URL", "spring.datasource.url", app);
        String username = resolve("SPRING_DATASOURCE_USERNAME", "spring.datasource.username", app);
        String password = resolve("SPRING_DATASOURCE_PASSWORD", "spring.datasource.password", app);

        Class.forName("org.postgresql.Driver");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET TRANSACTION READ ONLY");
            }
            printDatabaseIdentity(connection);
            printFlywayHistory(connection);
            printTablePresence(connection);
            connection.rollback();
        }
    }

    private static void printDatabaseIdentity(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("""
                        select
                            current_database() as database_name,
                            current_user as current_user,
                            current_schema() as current_schema
                        """)) {
            printResultSet("database_identity", rs);
        }
    }

    private static void printFlywayHistory(Connection connection) throws SQLException {
        if (!tableExists(connection, "flyway_schema_history")) {
            System.out.println("flyway_schema_history=<missing>");
            return;
        }
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("""
                        select
                            installed_rank,
                            version,
                            description,
                            script,
                            success,
                            installed_on
                        from flyway_schema_history
                        where version is not null
                        order by installed_rank desc
                        limit 8
                        """)) {
            printResultSet("flyway_history_latest", rs);
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                select
                    version,
                    description,
                    script,
                    success,
                    installed_on
                from flyway_schema_history
                where version in ('16', '17', '18')
                order by installed_rank
                """);
                ResultSet rs = statement.executeQuery()) {
            printResultSet("flyway_history_16_17_18", rs);
        }
    }

    private static void printTablePresence(Connection connection) throws SQLException {
        for (String table : List.of("domain_catalog_release", "domain_catalog_item")) {
            System.out.println("foundation_table." + table + "=" + tableExists(connection, table));
        }
        for (String table : DOMAIN_KNOWLEDGE_TABLES) {
            boolean exists = tableExists(connection, table);
            System.out.println("domain_knowledge_table." + table + "=" + exists);
            if (exists) {
                printColumnCount(connection, table);
            }
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select to_regclass(?) is not null")) {
            statement.setString(1, "public." + tableName);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private static void printColumnCount(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select count(*) as column_count
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = ?
                """)) {
            statement.setString(1, tableName);
            try (ResultSet rs = statement.executeQuery()) {
                printResultSet("columns." + tableName, rs);
            }
        }
    }

    private static void printResultSet(String label, ResultSet resultSet) throws SQLException {
        int columns = resultSet.getMetaData().getColumnCount();
        boolean any = false;
        while (resultSet.next()) {
            any = true;
            StringBuilder row = new StringBuilder(label).append(": ");
            for (int i = 1; i <= columns; i++) {
                if (i > 1) {
                    row.append(" | ");
                }
                row.append(resultSet.getMetaData().getColumnLabel(i)).append('=').append(resultSet.getString(i));
            }
            System.out.println(row);
        }
        if (!any) {
            System.out.println(label + ": <empty>");
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
