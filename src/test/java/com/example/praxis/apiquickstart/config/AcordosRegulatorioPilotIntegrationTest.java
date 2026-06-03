package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                "spring.datasource.url=jdbc:h2:mem:quickstart_acordos_regulatorios_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_acordos_regulatorios_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class AcordosRegulatorioPilotIntegrationTest {

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

    @BeforeEach
    void seedTables() {
        jdbcTemplate.execute("drop table if exists public.acordos_regulatorios");
        jdbcTemplate.execute("""
                create table public.acordos_regulatorios (
                    id integer primary key,
                    nome varchar(200) not null,
                    jurisdicao varchar(200) not null,
                    status varchar(20) not null,
                    descricao varchar(4000)
                )
                """);
        jdbcTemplate.execute("""
                create unique index ux_acordos_nome_juris
                    on public.acordos_regulatorios (nome, jurisdicao)
                """);

        jdbcTemplate.update("""
                insert into public.acordos_regulatorios (id, nome, jurisdicao, status, descricao)
                values (1, 'Acordo Atlantico', 'Brasil', 'VIGENTE', 'Acordo vigente para operacao costeira')
                """);
        jdbcTemplate.update("""
                insert into public.acordos_regulatorios (id, nome, jurisdicao, status, descricao)
                values (2, 'Acordo Pacifico', 'Chile', 'SUSPENSO', 'Acordo suspenso para reavaliacao')
                """);
        jdbcTemplate.update("""
                insert into public.acordos_regulatorios (id, nome, jurisdicao, status, descricao)
                values (3, 'Acordo Artico', 'Canada', 'REVOGADO', 'Acordo encerrado')
                """);
    }

    @Test
    void shouldExposeReviewSurfaceAndItemWorkflowActionsForAcordosRegulatorios() throws Exception {
        JsonNode surfacesCatalog = body(restTemplate.getForEntity(
                "/schemas/surfaces?resource=operations.acordos-regulatorios",
                String.class
        ));
        assertEquals("operations.acordos-regulatorios", surfacesCatalog.path("resourceKey").asText());
        JsonNode reviewSurface = findById(surfacesCatalog.path("surfaces"), "review");
        assertNotNull(reviewSurface);
        assertEquals("ITEM", reviewSurface.path("scope").asText());
        assertEquals("PATCH", reviewSurface.path("method").asText());
        assertEquals("/api/operations/acordos-regulatorios/{id}/review", reviewSurface.path("path").asText());
        assertFalse(reviewSurface.path("availability").path("allowed").asBoolean());
        assertEquals("resource-context-required", reviewSurface.path("availability").path("reason").asText());

        JsonNode actionsCatalog = body(restTemplate.getForEntity(
                "/schemas/actions?resource=operations.acordos-regulatorios",
                String.class
        ));
        assertEquals(3, actionsCatalog.path("actions").size());
        assertNotNull(findById(actionsCatalog.path("actions"), "suspend"));
        assertNotNull(findById(actionsCatalog.path("actions"), "reinstate"));
        assertNotNull(findById(actionsCatalog.path("actions"), "revoke"));

        JsonNode vigenteActions = body(restTemplate.getForEntity(
                "/api/operations/acordos-regulatorios/1/actions",
                String.class
        ));
        JsonNode suspend = findById(vigenteActions.path("actions"), "suspend");
        JsonNode reinstate = findById(vigenteActions.path("actions"), "reinstate");
        JsonNode revoke = findById(vigenteActions.path("actions"), "revoke");
        assertTrue(suspend.path("availability").path("allowed").asBoolean());
        assertFalse(reinstate.path("availability").path("allowed").asBoolean());
        assertEquals("resource-state-blocked", reinstate.path("availability").path("reason").asText());
        assertTrue(revoke.path("availability").path("allowed").asBoolean());

        JsonNode suspendedActions = body(restTemplate.getForEntity(
                "/api/operations/acordos-regulatorios/2/actions",
                String.class
        ));
        assertTrue(findById(suspendedActions.path("actions"), "reinstate").path("availability").path("allowed").asBoolean());
        assertFalse(findById(suspendedActions.path("actions"), "suspend").path("availability").path("allowed").asBoolean());

        JsonNode revokedSurfaces = body(restTemplate.getForEntity(
                "/api/operations/acordos-regulatorios/3/surfaces",
                String.class
        ));
        JsonNode revokedReview = findById(revokedSurfaces.path("surfaces"), "review");
        assertFalse(revokedReview.path("availability").path("allowed").asBoolean());
        assertEquals("resource-state-blocked", revokedReview.path("availability").path("reason").asText());

        JsonNode itemCapabilities = body(restTemplate.getForEntity(
                "/api/operations/acordos-regulatorios/1/capabilities",
                String.class
        ));
        assertNotNull(findById(itemCapabilities.path("surfaces"), "review"));
        assertEquals(3, itemCapabilities.path("actions").size());

        JsonNode itemEnvelope = body(restTemplate.getForEntity(
                "/api/operations/acordos-regulatorios/1",
                String.class
        ));
        assertNotNull(findLinkHref(itemEnvelope, "surfaces"));
        assertNotNull(findLinkHref(itemEnvelope, "actions"));
        assertNotNull(findLinkHref(itemEnvelope, "capabilities"));
    }

    @Test
    void shouldExecuteReviewAndWorkflowTransitionsForAcordosRegulatorios() throws Exception {
        ResponseEntity<String> reviewResponse = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/review",
                HttpMethod.PATCH,
                authorizedJson("""
                        {
                          "jurisdicao": "Uniao Europeia",
                          "descricao": "Acordo revisado para operacao internacional"
                        }
                        """),
                String.class
        );
        JsonNode reviewBody = body(reviewResponse);
        assertEquals("Uniao Europeia", reviewBody.path("data").path("jurisdicao").asText());
        assertEquals("Acordo revisado para operacao internacional", reviewBody.path("data").path("descricao").asText());

        ResponseEntity<String> suspendResponse = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions/suspend",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Auditoria operacional em curso"
                        }
                        """),
                String.class
        );
        JsonNode suspendBody = body(suspendResponse);
        assertEquals("VIGENTE", suspendBody.path("data").path("statusAnterior").asText());
        assertEquals("SUSPENSO", suspendBody.path("data").path("statusAtual").asText());
        assertEquals("SUSPENSO", jdbcTemplate.queryForObject(
                "select status from public.acordos_regulatorios where id = 1",
                String.class
        ));

        ResponseEntity<String> conflictResponse = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions/suspend",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Tentativa duplicada"
                        }
                        """),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, conflictResponse.getStatusCode());
        JsonNode conflictBody = objectMapper.readTree(conflictResponse.getBody());
        assertEquals("failure", conflictBody.path("status").asText());

        ResponseEntity<String> reinstateResponse = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions/reinstate",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Auditoria concluida"
                        }
                        """),
                String.class
        );
        JsonNode reinstateBody = body(reinstateResponse);
        assertEquals("SUSPENSO", reinstateBody.path("data").path("statusAnterior").asText());
        assertEquals("VIGENTE", reinstateBody.path("data").path("statusAtual").asText());

        ResponseEntity<String> revokeResponse = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/2/actions/revoke",
                HttpMethod.POST,
                authorizedJson("""
                        {
                          "justificativa": "Descumprimento regulatorio grave"
                        }
                        """),
                String.class
        );
        JsonNode revokeBody = body(revokeResponse);
        assertEquals("SUSPENSO", revokeBody.path("data").path("statusAnterior").asText());
        assertEquals("REVOGADO", revokeBody.path("data").path("statusAtual").asText());
        assertEquals("REVOGADO", jdbcTemplate.queryForObject(
                "select status from public.acordos_regulatorios where id = 2",
                String.class
        ));
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return objectMapper.readTree(response.getBody());
    }

    private HttpEntity<String> authorizedJson(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("admin", "ADMIN"));
        return new HttpEntity<>(json, headers);
    }

    private JsonNode findById(JsonNode items, String id) {
        for (JsonNode item : items) {
            if (id.equals(item.path("id").asText())) {
                return item;
            }
        }
        return null;
    }

    private String findLinkHref(JsonNode envelope, String rel) {
        JsonNode links = envelope.path("_links");
        if (!links.isObject()) {
            return null;
        }

        JsonNode halLink = links.path(rel);
        return halLink.isObject() ? halLink.path("href").asText(null) : null;
    }
}
