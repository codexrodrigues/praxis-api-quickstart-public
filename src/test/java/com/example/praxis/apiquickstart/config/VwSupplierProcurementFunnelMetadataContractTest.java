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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Prova HTTP da mesma projection governada consumida por chart, tabela e KPI no dashboard oficial. */
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
                "spring.datasource.url=jdbc:h2:mem:quickstart_procurement_funnel_metadata;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_procurement_funnel_metadata_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class VwSupplierProcurementFunnelMetadataContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @Test
    void shouldPublishOneProjectionForChartTableAndKpiWithoutChangingItsStatsContract() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=post&schemaType=response",
                String.class,
                "/api/procurement/vw-supplier-procurement-funnel/stats/group-by"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        JsonNode projections = objectMapper.readTree(response.getBody())
                .path("x-ui")
                .path("analytics")
                .path("projections");
        assertEquals(1, projections.size());

        JsonNode projection = projections.path(0);
        assertEquals("supplier-procurement-funnel", projection.path("id").asText());
        assertEquals("group-by", projection.path("source").path("operation").asText());
        assertEquals("stage", projection.path("bindings").path("primaryDimension").path("field").asText());
        assertEquals("volume", projection.path("bindings").path("primaryMetrics").path(0).path("field").asText());
        assertEquals("sum", projection.path("bindings").path("primaryMetrics").path(0).path("aggregation").asText());
        assertEquals(6, projection.path("defaults").path("limit").asInt());
        assertEquals("stage", projection.path("defaults").path("sort").path(0).path("field").asText());
        assertEquals(List.of("chart", "analytic-table", "kpi"), textValues(
                projection.path("presentationHints").path("preferredFamilies")));
    }

    private List<String> textValues(JsonNode values) {
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add(value.asText()));
        return result;
    }
}
