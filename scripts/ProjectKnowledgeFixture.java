import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class ProjectKnowledgeFixture {
    private static final String RELEASE_KEY_PREFIX = "page-builder-e2e-project-knowledge";
    private static final String CONCEPT_KEY = "page-builder.e2e.project-knowledge.identity-card";
    private static final String EVIDENCE_KEY = "page-builder:e2e:project-knowledge:active-fixture";

    private ProjectKnowledgeFixture() {
    }

    public static void main(String[] args) throws Exception {
        String command = args.length > 0 ? args[0] : "seed";
        String tenantId = value(args, 1, envOrDefault("PRAXIS_E2E_TENANT_ID", "desenv"));
        String environment = value(args, 2, envOrDefault("PRAXIS_E2E_ENV", "local"));
        String contextKey = value(args, 3, "human-resources");
        String resourceKey = value(args, 4, "human-resources.funcionarios");

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

        Class.forName("org.postgresql.Driver");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            if ("cleanup".equalsIgnoreCase(command)) {
                cleanup(connection, tenantId, environment);
                connection.commit();
                System.out.println("project_knowledge_fixture=cleaned");
                return;
            }
            if ("assert-vector-document-count".equalsIgnoreCase(command)) {
                String conceptKey = value(args, 3, CONCEPT_KEY);
                String evidenceKey = value(args, 4, EVIDENCE_KEY + ":" + tenantId + ":" + environment);
                int expectedCount = Integer.parseInt(value(args, 5, "1"));
                int actualCount = countVectorDocuments(connection, tenantId, environment, conceptKey, evidenceKey);
                if (actualCount != expectedCount) {
                    throw new IllegalStateException("Unexpected Project Knowledge vector document count"
                            + " tenant_id=" + tenantId
                            + " environment=" + environment
                            + " concept_key=" + conceptKey
                            + " evidence_key=" + evidenceKey
                            + " expected=" + expectedCount
                            + " actual=" + actualCount);
                }
                System.out.println("{"
                        + "\"status\":\"project-knowledge-vector-document-count-ready\","
                        + "\"tenantId\":\"" + escape(tenantId) + "\","
                        + "\"environment\":\"" + escape(environment) + "\","
                        + "\"conceptKey\":\"" + escape(conceptKey) + "\","
                        + "\"evidenceKey\":\"" + escape(evidenceKey) + "\","
                        + "\"expectedCount\":" + expectedCount + ","
                        + "\"actualCount\":" + actualCount
                        + "}");
                return;
            }
            boolean includeEvidence = !"seed-concept".equalsIgnoreCase(command);
            seed(connection, tenantId, environment, contextKey, resourceKey, includeEvidence);
            connection.commit();
            System.out.println("project_knowledge_fixture=seeded"
                    + " tenant_id=" + tenantId
                    + " environment=" + environment
                    + " context_key=" + contextKey
                    + " resource_key=" + resourceKey
                    + " concept_key=" + CONCEPT_KEY
                    + " active_evidence=" + includeEvidence);
        }
    }

    private static int countVectorDocuments(
            Connection connection,
            String tenantId,
            String environment,
            String conceptKey,
            String evidenceKey) throws SQLException {
        try (PreparedStatement tableExists = connection.prepareStatement("select to_regclass('public.vector_store')")) {
            try (ResultSet resultSet = tableExists.executeQuery()) {
                if (!resultSet.next() || resultSet.getString(1) == null) {
                    return 0;
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                select count(*)
                from vector_store
                where metadata ->> 'resourceType' = 'project_knowledge'
                  and metadata ->> 'tenantId' = ?
                  and metadata ->> 'environment' = ?
                  and metadata ->> 'domainKnowledgeConceptKey' = ?
                  and metadata ->> 'domainKnowledgeEvidenceKey' = ?
                  and metadata ->> 'domainKnowledgeEvidenceStatus' = 'active'
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, environment);
            statement.setString(3, conceptKey);
            statement.setString(4, evidenceKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private static void seed(
            Connection connection,
            String tenantId,
            String environment,
            String contextKey,
            String resourceKey,
            boolean includeEvidence) throws SQLException {
        UUID releaseId = ensureRelease(connection, tenantId, environment);
        cleanup(connection, tenantId, environment);
        UUID conceptId = UUID.randomUUID();
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into domain_knowledge_concept (
                    id,
                    tenant_id,
                    environment,
                    concept_key,
                    context_key,
                    resource_key,
                    node_type,
                    label,
                    description,
                    lifecycle,
                    curation_status,
                    ai_visibility,
                    source_release_id,
                    payload,
                    created_at,
                    updated_at
                ) values (
                    ?::uuid,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    'concept',
                    'Page Builder E2E project preference',
                    'Safe governed knowledge fixture for Page Builder authoring E2E.',
                    'active',
                    'approved',
                    'allow',
                    ?::uuid,
                    ?::jsonb,
                    ?,
                    ?
                )
                """)) {
            OffsetDateTime now = OffsetDateTime.now();
            statement.setString(1, conceptId.toString());
            statement.setString(2, tenantId);
            statement.setString(3, environment);
            statement.setString(4, CONCEPT_KEY);
            statement.setString(5, contextKey);
            statement.setString(6, resourceKey);
            statement.setString(7, releaseId.toString());
            statement.setString(8, """
                    {
                      "kind": "project_preference",
                      "safeSummary": "Prefer compact employee identity cards when authoring funcionario pages.",
                      "sourceSummary": "page-builder-e2e-fixture",
                      "influence": "layout_preference"
                    }
                    """);
            statement.setObject(9, now);
            statement.setObject(10, now);
            statement.executeUpdate();
        }
        if (includeEvidence) {
            insertActiveEvidence(connection, tenantId, environment, conceptId, releaseId);
        }
    }

    private static void insertActiveEvidence(
            Connection connection,
            String tenantId,
            String environment,
            UUID conceptId,
            UUID releaseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into domain_knowledge_evidence (
                    id,
                    tenant_id,
                    environment,
                    evidence_key,
                    subject_type,
                    subject_id,
                    evidence_type,
                    source_release_id,
                    source_uri,
                    source_pointer,
                    status,
                    confidence,
                    payload,
                    created_at
                ) values (
                    ?::uuid,
                    ?,
                    ?,
                    ?,
                    'concept',
                    ?::uuid,
                    'llm_proposal',
                    ?::uuid,
                    'praxis-e2e://project-knowledge-fixture',
                    '/projectKnowledge/fixture',
                    'active',
                    0.91,
                    ?::jsonb,
                    ?
                )
                """)) {
            OffsetDateTime now = OffsetDateTime.now();
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, tenantId);
            statement.setString(3, environment);
            statement.setString(4, EVIDENCE_KEY + ":" + tenantId + ":" + environment);
            statement.setString(5, conceptId.toString());
            statement.setString(6, releaseId.toString());
            statement.setString(7, """
                    {
                      "evidenceKey": "page-builder:e2e:project-knowledge:active-fixture",
                      "evidenceType": "llm_proposal",
                      "summary": "Reviewed fixture evidence keeps Project Knowledge active for local browser E2E.",
                      "aiVisibility": "summarize_only",
                      "evidenceSafety": "reviewed"
                    }
                    """);
            statement.setObject(8, now);
            statement.executeUpdate();
        }
    }

    private static UUID ensureRelease(Connection connection, String tenantId, String environment) throws SQLException {
        String releaseKey = RELEASE_KEY_PREFIX + ":" + tenantId + ":" + environment;
        try (PreparedStatement select = connection.prepareStatement("""
                select id
                from domain_catalog_release
                where release_key = ?
                limit 1
                """)) {
            select.setString(1, releaseKey);
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    return UUID.fromString(resultSet.getString("id"));
                }
            }
        }

        UUID releaseId = UUID.randomUUID();
        try (PreparedStatement insert = connection.prepareStatement("""
                insert into domain_catalog_release (
                    id,
                    release_key,
                    schema_version,
                    service_key,
                    service_name,
                    service_version,
                    generated_at,
                    tenant_id,
                    environment,
                    raw_payload,
                    created_at
                ) values (
                    ?::uuid,
                    ?,
                    'praxis.domain-catalog/v0.2',
                    'praxis-page-builder-e2e',
                    'Page Builder E2E',
                    'local',
                    ?,
                    ?,
                    ?,
                    '{}'::jsonb,
                    ?
                )
                """)) {
            OffsetDateTime now = OffsetDateTime.now();
            insert.setString(1, releaseId.toString());
            insert.setString(2, releaseKey);
            insert.setObject(3, now);
            insert.setString(4, tenantId);
            insert.setString(5, environment);
            insert.setObject(6, now);
            insert.executeUpdate();
        }
        return releaseId;
    }

    private static void cleanup(Connection connection, String tenantId, String environment) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                delete from domain_knowledge_evidence
                where subject_type = 'concept'
                  and subject_id in (
                      select id
                      from domain_knowledge_concept
                      where tenant_id = ?
                        and environment = ?
                        and concept_key = ?
                  )
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, environment);
            statement.setString(3, CONCEPT_KEY);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                delete from domain_knowledge_concept
                where tenant_id = ?
                  and environment = ?
                  and concept_key = ?
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, environment);
            statement.setString(3, CONCEPT_KEY);
            statement.executeUpdate();
        }
    }

    private static String value(String[] args, int index, String fallback) {
        return args.length > index && args[index] != null && !args[index].isBlank()
                ? args[index]
                : fallback;
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Properties loadProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static String resolve(
            List<String> envNames,
            List<String> propertyNames,
            Properties properties) {
        for (String envName : envNames) {
            String env = System.getenv(envName);
            if (env != null && !env.isBlank()) {
                return env;
            }
        }

        for (String propertyName : propertyNames) {
            String value = properties.getProperty(propertyName);
            if (value == null || value.isBlank()) {
                continue;
            }
            if (value.startsWith("${") && value.endsWith("}")) {
                int separator = value.indexOf(':');
                if (separator > 2) {
                    return value.substring(separator + 1, value.length() - 1);
                }
            }
            return value;
        }

        throw new IllegalStateException("Missing datasource configuration for project knowledge fixture.");
    }
}
