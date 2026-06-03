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

public final class DomainKnowledgeProjectionValidation {
    private static final List<String> REQUIRED_TABLES = List.of(
            "domain_catalog_release",
            "domain_catalog_item",
            "domain_knowledge_concept",
            "domain_knowledge_alias",
            "domain_knowledge_binding",
            "domain_knowledge_relationship",
            "domain_knowledge_evidence"
    );

    private DomainKnowledgeProjectionValidation() {
    }

    public static void main(String[] args) throws Exception {
        Properties app = loadProperties(Path.of("src/main/resources/application.properties"));
        String url = resolve(
                List.of("CONFIG_DATASOURCE_URL", "SPRING_DATASOURCE_URL"),
                List.of("config.datasource.url", "spring.datasource.url"),
                app);
        String username = resolve(
                List.of("CONFIG_DATASOURCE_USERNAME", "SPRING_DATASOURCE_USERNAME"),
                List.of("config.datasource.username", "spring.datasource.username"),
                app);
        String password = resolve(
                List.of("CONFIG_DATASOURCE_PASSWORD", "SPRING_DATASOURCE_PASSWORD"),
                List.of("config.datasource.password", "spring.datasource.password"),
                app);
        String serviceKey = envOrDefault("DOMAIN_CATALOG_SERVICE_KEY", "praxis-service");
        String resourceKey = envOrDefault("RESOURCE_KEY", args.length > 0 ? args[0] : "human-resources.funcionarios");
        int minConcepts = intEnvOrDefault("MIN_CONCEPTS", 1);
        int minBindings = intEnvOrDefault("MIN_BINDINGS", 1);
        int minEvidence = intEnvOrDefault("MIN_EVIDENCE", 1);

        Class.forName("org.postgresql.Driver");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET TRANSACTION READ ONLY");
            }

            printDatabaseIdentity(connection);
            verifyTables(connection);

            Release release = latestRelease(connection, serviceKey, resourceKey);
            if (release == null) {
                throw new IllegalStateException(
                        "No domain catalog release found for serviceKey=" + serviceKey + ", resourceKey=" + resourceKey);
            }
            System.out.println("domain_catalog_release: release_key=" + release.releaseKey()
                    + " | service_key=" + release.serviceKey()
                    + " | item_count=" + release.itemCount());
            printItemTypeCounts(connection, release.id());

            Counts counts = projectionCounts(connection, release.id(), resourceKey);
            System.out.println("domain_knowledge_projection: concepts=" + counts.concepts()
                    + " | aliases=" + counts.aliases()
                    + " | bindings=" + counts.bindings()
                    + " | relationships=" + counts.relationships()
                    + " | evidence=" + counts.evidence());

            if (counts.concepts() < minConcepts) {
                throw new IllegalStateException("Expected at least " + minConcepts + " projected concept(s)");
            }
            if (counts.bindings() < minBindings) {
                throw new IllegalStateException("Expected at least " + minBindings + " projected binding(s)");
            }
            if (counts.evidence() < minEvidence) {
                throw new IllegalStateException("Expected at least " + minEvidence + " projected evidence item(s)");
            }
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

    private static void verifyTables(Connection connection) throws SQLException {
        for (String table : REQUIRED_TABLES) {
            if (!tableExists(connection, table)) {
                throw new IllegalStateException("Required table is missing: " + table);
            }
        }
        System.out.println("domain_knowledge_tables=present");
    }

    private static Release latestRelease(Connection connection, String serviceKey, String resourceKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select
                    r.id::text as id,
                    r.release_key,
                    r.service_key,
                    count(i.id) as item_count
                from domain_catalog_release r
                left join domain_catalog_item i on i.release_id = r.id
                where (? is null or r.service_key = ?)
                  and (? is null or r.release_key like '%' || ? || '%')
                group by r.id, r.release_key, r.service_key, r.generated_at, r.created_at
                order by r.generated_at desc nulls last, r.created_at desc
                limit 1
                """)) {
            bindNullable(statement, 1, serviceKey);
            bindNullable(statement, 2, serviceKey);
            bindNullable(statement, 3, resourceKey);
            bindNullable(statement, 4, resourceKey);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Release(
                        rs.getString("id"),
                        rs.getString("release_key"),
                        rs.getString("service_key"),
                        rs.getLong("item_count"));
            }
        }
    }

    private static Counts projectionCounts(Connection connection, String releaseId, String resourceKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select
                    (select count(*) from domain_knowledge_concept
                     where source_release_id = ?::uuid
                       and (? is null or resource_key = ?)) as concepts,
                    (select count(*) from domain_knowledge_alias a
                     join domain_knowledge_concept c on c.id = a.concept_id
                     where c.source_release_id = ?::uuid
                       and (? is null or c.resource_key = ?)) as aliases,
                    (select count(*) from domain_knowledge_binding
                     where source_release_id = ?::uuid
                       and (? is null or resource_key = ?)) as bindings,
                    (select count(*) from domain_knowledge_relationship r
                     join domain_knowledge_concept c on c.id = r.source_concept_id
                     where c.source_release_id = ?::uuid
                       and (? is null or c.resource_key = ?)) as relationships,
                    (select count(*) from domain_knowledge_evidence
                     where source_release_id = ?::uuid) as evidence
                """)) {
            statement.setString(1, releaseId);
            bindNullable(statement, 2, resourceKey);
            bindNullable(statement, 3, resourceKey);
            statement.setString(4, releaseId);
            bindNullable(statement, 5, resourceKey);
            bindNullable(statement, 6, resourceKey);
            statement.setString(7, releaseId);
            bindNullable(statement, 8, resourceKey);
            bindNullable(statement, 9, resourceKey);
            statement.setString(10, releaseId);
            bindNullable(statement, 11, resourceKey);
            bindNullable(statement, 12, resourceKey);
            statement.setString(13, releaseId);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Projection count query returned no rows");
                }
                return new Counts(
                        rs.getLong("concepts"),
                        rs.getLong("aliases"),
                        rs.getLong("bindings"),
                        rs.getLong("relationships"),
                        rs.getLong("evidence"));
            }
        }
    }

    private static void printItemTypeCounts(Connection connection, String releaseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select item_type, count(*) as item_count
                from domain_catalog_item
                where release_id = ?::uuid
                group by item_type
                order by item_type
                """)) {
            statement.setString(1, releaseId);
            try (ResultSet rs = statement.executeQuery()) {
                printResultSet("domain_catalog_item_type", rs);
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

    private static void bindNullable(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setString(index, null);
        } else {
            statement.setString(index, value);
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

    private static String resolve(List<String> envNames, List<String> propertyNames, Properties properties) {
        for (String envName : envNames) {
            String env = System.getenv(envName);
            if (env != null && !env.isBlank()) {
                return env;
            }
        }
        for (String propertyName : propertyNames) {
            String value = properties.getProperty(propertyName);
            if (value != null && !value.isBlank()) {
                if (value.startsWith("${") && value.endsWith("}")) {
                    int separator = value.indexOf(':');
                    if (separator > 2) {
                        return value.substring(separator + 1, value.length() - 1);
                    }
                    continue;
                }
                return value;
            }
        }
        throw new IllegalStateException("Missing " + envNames + " or " + propertyNames);
    }

    private static String envOrDefault(String envName, String defaultValue) {
        String value = System.getenv(envName);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int intEnvOrDefault(String envName, int defaultValue) {
        String value = System.getenv(envName);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    private record Release(String id, String releaseKey, String serviceKey, long itemCount) {
    }

    private record Counts(long concepts, long aliases, long bindings, long relationships, long evidence) {
    }
}
