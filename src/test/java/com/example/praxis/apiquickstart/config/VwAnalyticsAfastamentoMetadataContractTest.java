package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Keeps the published analytics metadata proof isolated from the operational HTTP fixture. */
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
                "praxis.ai.provider=mock",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:quickstart_absence_analytics_metadata;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_absence_analytics_metadata_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class VwAnalyticsAfastamentoMetadataContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @Test
    void shouldPublishComparisonProjectionWithCanonicalRecordOpenTarget() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=response",
                String.class,
                "/api/human-resources/vw-analytics-afastamentos/stats/comparison"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        JsonNode projection = objectMapper.readTree(response.getBody())
                .path("x-ui")
                .path("analytics")
                .path("projections")
                .path(0);
        assertEquals("departamentoIdsIn", projection.path("bindings").path("primaryDimension").path("keyFilterField").asText());
        JsonNode recordOpen = projection.path("interactions").path("recordOpen");
        assertEquals("funcionarioId", recordOpen.path("sourceIdentityField").asText());
        assertEquals("human-resources.funcionarios", recordOpen.path("target").path("resourceKey").asText());
        assertEquals("hero-profile", recordOpen.path("target").path("surfaceId").asText());
        assertFalse(recordOpen.path("target").has("path"));
        assertFalse(projection.path("interactions").has("drillDown"));
    }
}
