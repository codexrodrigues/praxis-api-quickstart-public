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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
                "spring.datasource.url=jdbc:h2:mem:quickstart_risk_intelligence_cockpit_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_risk_intelligence_cockpit_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class RiskIntelligenceCockpitIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @MockBean
    private DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    @Test
    void shouldExposeRiskIntelligenceCockpitSurfaces() throws Exception {
        when(workflowActionPolicyResolver.resolveAppliedPolicy(anyString())).thenReturn(Optional.empty());

        assertRiskSurface(
                "risk-intelligence.ameacas",
                "threat-monitoring-board",
                "VIEW",
                "Monitoramento de ameacas"
        );
        assertRiskSurface(
                "risk-intelligence.vw-indicadores-incidentes",
                "incident-intelligence-board",
                "VIEW",
                "Inteligencia de incidentes"
        );
        assertRiskSurface(
                "risk-intelligence.vw-indicadores-incidentes",
                "incident-trend-chart",
                "READ_PROJECTION",
                "Evolucao de incidentes"
        );
    }

    private void assertRiskSurface(String resourceKey, String surfaceId, String kind, String title) throws Exception {
        JsonNode surfacesCatalog = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource={resourceKey}",
                String.class,
                resourceKey
        ));
        assertEquals(resourceKey, surfacesCatalog.path("resourceKey").asText());
        JsonNode surface = findById(surfacesCatalog.path("surfaces"), surfaceId);
        assertNotNull(surface);
        assertEquals(kind, surface.path("kind").asText());
        assertEquals("COLLECTION", surface.path("scope").asText());
        assertEquals(title, surface.path("title").asText());
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        assertNotNull(response.getBody());
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode findById(JsonNode items, String id) {
        for (JsonNode item : items) {
            if (id.equals(item.path("id").asText())) {
                return item;
            }
        }
        return null;
    }
}
