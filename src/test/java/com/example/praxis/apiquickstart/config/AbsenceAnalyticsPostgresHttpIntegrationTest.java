package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.hr.security.HrAnalyticsAuthorities;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Proves the comparison endpoint against the PostgreSQL operational migrations. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.read-open=true",
                "app.security.write-disabled=false",
                "app.security.schemas-aggregator.enabled=true",
                "app.security.csrf.disable=true",
                "app.hr.analytics.demo-department-scopes=aggregate-only=*",
                "app.session.cookie-name=SESSION",
                "app.session.secure=false",
                "app.session.samesite=Lax",
                "praxis.stats.enabled=true",
                "praxis.ai.provider=mock",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "config.datasource.url=jdbc:h2:mem:quickstart_absence_analytics_postgres_http_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class AbsenceAnalyticsPostgresHttpIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    @Qualifier("apiJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @DynamicPropertySource
    static void apiDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @BeforeEach
    void seedOperationalSchema() throws Exception {
        jdbcTemplate.execute("drop view if exists public.vw_analytics_afastamentos");
        jdbcTemplate.execute("drop function if exists public.hr_absence_criticality_level(bigint)");
        jdbcTemplate.execute("drop table if exists public.funcionario_lotacoes_departamento cascade");
        jdbcTemplate.execute("drop table if exists public.ferias_afastamentos cascade");
        jdbcTemplate.execute("drop table if exists public.funcionarios cascade");
        jdbcTemplate.execute("drop table if exists public.departamentos cascade");
        jdbcTemplate.execute(Files.readString(Path.of("src/test/resources/absence-analytics-lab/postgres-operational-schema.sql")));
        jdbcTemplate.execute(Files.readString(Path.of("db/operational-migrations/V20260714_001__historical_department_assignments.sql")));
        jdbcTemplate.execute(Files.readString(Path.of("src/test/resources/absence-analytics-lab/postgres-operational-data.sql")));
        jdbcTemplate.execute(Files.readString(Path.of("db/operational-migrations/V20260715_005__absence_analytics_unique_days_policy.sql")));
    }

    @Test
    void shouldExposePostgresMaterializedAbsencesThroughTheAuthorizedComparisonEndpoint() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate(
                "aggregate-only", "USER", List.of(HrAnalyticsAuthorities.AGGREGATE_READ)
        ));
        HttpEntity<String> request = new HttpEntity<>("""
                {
                  "filter": {},
                  "field": "departamento",
                  "periodField": "competencia",
                  "period": {
                    "from": "2026-07-01",
                    "to": "2026-07-31",
                    "timezone": "America/Sao_Paulo",
                    "mode": "PREVIOUS_ALIGNED"
                  },
                  "metrics": [
                    { "operation": "DISTINCT_COUNT", "field": "funcionarioId", "alias": "colaboradores" },
                    { "operation": "SUM", "field": "diasAfastado", "alias": "dias" }
                  ],
                  "orderBy": "VALUE_DESC",
                  "limit": 10
                }
                """, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/human-resources/vw-analytics-afastamentos/stats/comparison", request, String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        JsonNode buckets = objectMapper.readTree(response.getBody()).path("data").path("buckets");
        assertBucket(buckets, 1, "Operacoes", 2, 22);
        assertBucket(buckets, 2, "Recursos Humanos", 1, 2);
    }

    private void assertBucket(JsonNode buckets, int key, String label, int employees, int days) {
        JsonNode bucket = findBucket(buckets, key);
        assertNotNull(bucket, "Missing department bucket " + key);
        assertEquals(label, bucket.path("label").asText());
        assertEquals(employees, bucket.path("values").path("colaboradores").path("current").asInt());
        assertEquals(days, bucket.path("values").path("dias").path("current").asInt());
        assertEquals(0, bucket.path("values").path("colaboradores").path("previous").asInt());
        assertEquals(0, bucket.path("values").path("dias").path("previous").asInt());
    }

    private JsonNode findBucket(JsonNode buckets, int key) {
        for (JsonNode bucket : buckets) {
            if (bucket.path("key").asInt() == key) {
                return bucket;
            }
        }
        return null;
    }
}
