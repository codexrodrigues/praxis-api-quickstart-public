package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

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

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("apiJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @MockBean
    private DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    @BeforeEach
    void seedTables() {
        when(workflowActionPolicyResolver.resolveAppliedPolicy(anyString())).thenReturn(Optional.empty());

        jdbcTemplate.execute("drop table if exists public.ameacas");
        jdbcTemplate.execute("""
                create table public.ameacas (
                    id integer primary key,
                    nome varchar(200) not null,
                    classe varchar(40),
                    planeta varchar(120),
                    nivel integer,
                    status varchar(40) not null,
                    recompensa numeric(19, 2)
                )
                """);
        jdbcTemplate.update("""
                insert into public.ameacas (id, nome, classe, planeta, nivel, status, recompensa)
                values (1, 'Galactic Raider', 'INVASAO', 'Terra', 9, 'LIVRE', 2500000.00)
                """);
        jdbcTemplate.update("""
                insert into public.ameacas (id, nome, classe, planeta, nivel, status, recompensa)
                values (2, 'Contained Syndicate', 'ORGANIZACAO', 'Marte', 5, 'CAPTURADO', 400000.00)
                """);
    }

    @Test
    void shouldExposeRiskIntelligenceCockpitSurfaces() throws Exception {
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
                "CHART",
                "Evolucao de incidentes"
        );
        assertRiskSurface(
                "operations.incidentes",
                "incident-investigation-board",
                "VIEW",
                "Mesa de investigacao de incidentes"
        );
    }

    @Test
    void shouldExposeAndExecuteThreatTriageWorkflowActions() throws Exception {
        JsonNode actions = body(restTemplate.getForEntity(
                "/schemas/actions?resource=risk-intelligence.ameacas",
                String.class
        ));
        assertEquals("risk-intelligence.ameacas", actions.path("resourceKey").asText());
        assertAction(actions, "mark-under-observation", "/api/risk-intelligence/ameacas/{id}/actions/mark-under-observation");
        assertAction(actions, "mark-captured", "/api/risk-intelligence/ameacas/{id}/actions/mark-captured");

        ResponseEntity<String> observationResponse = restTemplate.exchange(
                "/api/risk-intelligence/ameacas/1/actions/mark-under-observation",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Sinais anomalos exigem vigilancia ativa"
                        }
                        """),
                String.class
        );
        JsonNode observation = body(observationResponse);
        assertEquals("LIVRE", observation.path("data").path("statusAnterior").asText());
        assertEquals("EM_OBSERVACAO", observation.path("data").path("statusAtual").asText());
        assertThreatSelectable(1, true);

        ResponseEntity<String> capturedResponse = restTemplate.exchange(
                "/api/risk-intelligence/ameacas/1/actions/mark-captured",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Equipe confirmou captura e risco encerrado"
                        }
                        """),
                String.class
        );
        JsonNode captured = body(capturedResponse);
        assertEquals("EM_OBSERVACAO", captured.path("data").path("statusAnterior").asText());
        assertEquals("CAPTURADO", captured.path("data").path("statusAtual").asText());
        assertThreatSelectable(1, false);

        ResponseEntity<String> duplicateCapturedResponse = restTemplate.exchange(
                "/api/risk-intelligence/ameacas/1/actions/mark-captured",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "motivo": "Tentativa duplicada"
                        }
                        """),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, duplicateCapturedResponse.getStatusCode());
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

    private HttpEntity<String> authorizedJson(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("admin", "ADMIN"));
        return new HttpEntity<>(json, headers);
    }

    private void assertAction(JsonNode actionsCatalog, String actionId, String path) {
        JsonNode action = findById(actionsCatalog.path("actions"), actionId);
        assertNotNull(action);
        assertEquals("ITEM", action.path("scope").asText());
        assertEquals(path, action.path("path").asText());
    }

    private void assertThreatSelectable(int id, boolean expectedSelectable) throws Exception {
        JsonNode selectedThreat = objectMapper.readTree(restTemplate.getForObject(
                "/api/risk-intelligence/ameacas/option-sources/threat/options/by-ids?ids={id}",
                String.class,
                id
        ));
        assertEquals(expectedSelectable, selectedThreat.get(0).path("extra").path("selectable").asBoolean());
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
