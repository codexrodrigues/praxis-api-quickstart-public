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
                "praxis.resource-version.etag.secret=test-secret-resource-version",
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
        jdbcTemplate.execute("drop table if exists public.praxis_resource_action_execution");
        jdbcTemplate.execute("""
                create table public.acordos_regulatorios (
                    id integer primary key,
                    nome varchar(200) not null,
                    jurisdicao varchar(200) not null,
                    status varchar(20) not null,
                    descricao varchar(4000),
                    version bigint not null default 0
                )
                """);
        jdbcTemplate.execute("""
                create table public.praxis_resource_action_execution (
                    execution_id uuid primary key,
                    resource_key varchar(200) not null,
                    resource_id varchar(128) not null,
                    action_id varchar(120) not null,
                    action_scope varchar(32) not null,
                    idempotency_key varchar(255) not null,
                    request_hash varchar(128) not null,
                    execution_status varchar(32) not null,
                    response_payload json,
                    correlation_id varchar(255) not null,
                    request_id varchar(255),
                    actor_subject varchar(255) not null,
                    actor_authorities varchar(1000),
                    started_at timestamp with time zone not null,
                    completed_at timestamp with time zone,
                    failure_code varchar(120),
                    failure_message varchar(1000),
                    unique(resource_key, resource_id, action_id, actor_subject, idempotency_key)
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

        JsonNode vigenteActions = body(restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions",
                HttpMethod.GET,
                authorizedJson(null),
                String.class
        ));
        JsonNode suspend = findById(vigenteActions.path("actions"), "suspend");
        JsonNode reinstate = findById(vigenteActions.path("actions"), "reinstate");
        JsonNode revoke = findById(vigenteActions.path("actions"), "revoke");
        assertTrue(suspend.path("availability").path("allowed").asBoolean());
        assertEquals("HIGH", suspend.path("execution").path("interaction").path("riskLevel").asText());
        assertTrue(suspend.path("execution").path("interaction").path("confirmationRequired").asBoolean());
        assertTrue(suspend.path("execution").path("interaction").path("reversible").asBoolean());
        assertEquals("REQUIRED", suspend.path("execution").path("preconditions").path("idempotencyKey").asText());
        assertEquals("REQUIRED", suspend.path("execution").path("preconditions").path("resourceVersion").asText());
        assertEquals("IF_MATCH", suspend.path("execution").path("preconditions").path("resourceVersionTransport").asText());
        assertEquals("resourceVersion", suspend.path("execution").path("preconditions").path("resourceVersionField").asText());
        assertFalse(reinstate.path("availability").path("allowed").asBoolean());
        assertEquals("resource-state-blocked", reinstate.path("availability").path("reason").asText());
        assertTrue(revoke.path("availability").path("allowed").asBoolean());

        JsonNode suspendedActions = body(restTemplate.exchange(
                "/api/operations/acordos-regulatorios/2/actions",
                HttpMethod.GET,
                authorizedJson(null),
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
        assertFalse(itemCapabilities.path("capabilities").path("operations").path("delete").asBoolean());

        JsonNode openApi = objectMapper.readTree(restTemplate.getForObject(
                "/v3/api-docs/api-operations-acordos-regulatorios",
                String.class
        ));
        JsonNode itemPath = openApi.path("paths").path("/api/operations/acordos-regulatorios/{id}");
        assertTrue(itemPath.has("put"));
        assertFalse(itemPath.has("delete"));
        assertFalse(openApi.path("paths").has("/api/operations/acordos-regulatorios/batch"));

        JsonNode itemEnvelope = body(restTemplate.getForEntity(
                "/api/operations/acordos-regulatorios/1",
                String.class
        ));
        assertTrue(itemEnvelope.path("data").path("resourceVersion").asText().startsWith("\""));
        assertNotNull(findLinkHref(itemEnvelope, "surfaces"));
        assertNotNull(findLinkHref(itemEnvelope, "actions"));
        assertNotNull(findLinkHref(itemEnvelope, "capabilities"));

        JsonNode updateSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path={path}&operation=put&schemaType=request",
                String.class,
                "/api/operations/acordos-regulatorios/{id}"
        ));
        assertTrue(updateSchema.path("properties").has("nome"));
        assertTrue(updateSchema.path("properties").has("jurisdicao"));
        assertTrue(updateSchema.path("properties").has("descricao"));
        assertFalse(updateSchema.path("properties").has("status"));
        assertFalse(updateSchema.path("properties").has("resourceVersion"));
    }

    @Test
    void shouldExecuteReviewAndWorkflowTransitionsForAcordosRegulatorios() throws Exception {
        String reviewEtag = currentEtag(1);
        ResponseEntity<String> reviewResponse = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/review",
                HttpMethod.PATCH,
                authorizedVersionedJson("""
                        {
                          "jurisdicao": "Uniao Europeia",
                          "descricao": "Acordo revisado para operacao internacional"
                        }
                        """, reviewEtag),
                String.class
        );
        JsonNode reviewBody = body(reviewResponse);
        assertEquals("Uniao Europeia", reviewBody.path("data").path("jurisdicao").asText());
        assertEquals("Acordo revisado para operacao internacional", reviewBody.path("data").path("descricao").asText());
        assertNotNull(reviewResponse.getHeaders().getETag());
        assertFalse(reviewEtag.equals(reviewResponse.getHeaders().getETag()));

        String vigenteEtag = currentEtag(1);
        String suspendCommand = """
                {
                  "justificativa": "Auditoria operacional em curso"
                }
                """;

        ResponseEntity<String> suspendResponse = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions/suspend",
                HttpMethod.POST,
                authorizedCommand(suspendCommand, vigenteEtag, "agreement-suspend-success"),
                String.class
        );
        JsonNode suspendBody = body(suspendResponse);
        assertEquals("VIGENTE", suspendBody.path("data").path("statusAnterior").asText());
        assertEquals("SUSPENSO", suspendBody.path("data").path("statusAtual").asText());
        assertFalse(suspendBody.path("_links").path("schema").isMissingNode(), suspendBody.toPrettyString());
        assertEquals("SUSPENSO", jdbcTemplate.queryForObject(
                "select status from public.acordos_regulatorios where id = 1",
                String.class
        ));
        String suspendedEtag = suspendResponse.getHeaders().getETag();
        assertNotNull(suspendedEtag);
        assertFalse(vigenteEtag.equals(suspendedEtag));

        ResponseEntity<String> replayResponse = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions/suspend",
                HttpMethod.POST,
                authorizedCommand(suspendCommand, vigenteEtag, "agreement-suspend-success"),
                String.class
        );
        assertEquals(HttpStatus.OK, replayResponse.getStatusCode());
        assertEquals(suspendBody.path("data"), body(replayResponse).path("data"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.praxis_resource_action_execution where action_id = 'suspend' and idempotency_key = 'agreement-suspend-success'",
                Integer.class
        ));

        ResponseEntity<String> replayWithoutEtag = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions/suspend",
                HttpMethod.POST,
                authorizedCommand(suspendCommand, null, "agreement-suspend-success"),
                String.class
        );
        assertEquals(HttpStatus.PRECONDITION_REQUIRED, replayWithoutEtag.getStatusCode());

        ResponseEntity<String> conflictingReplay = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions/suspend",
                HttpMethod.POST,
                authorizedCommand("{\"justificativa\":\"Outro command\"}", vigenteEtag,
                        "agreement-suspend-success"),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, conflictingReplay.getStatusCode());

        ResponseEntity<String> conflictResponse = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions/suspend",
                HttpMethod.POST,
                authorizedCommand("""
                        {
                          "justificativa": "Tentativa duplicada"
                        }
                        """, suspendedEtag, "agreement-suspend-invalid-state"),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, conflictResponse.getStatusCode());
        JsonNode conflictBody = objectMapper.readTree(conflictResponse.getBody());
        assertEquals("failure", conflictBody.path("status").asText());
        assertEquals("CONFLICT_DEPENDENCY", conflictBody.path("errors").get(0).path("outcome").asText());

        ResponseEntity<String> reinstateResponse = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions/reinstate",
                HttpMethod.POST,
                authorizedCommand("""
                        {
                          "justificativa": "Auditoria concluida"
                        }
                        """, suspendedEtag, "agreement-reinstate-success"),
                String.class
        );
        JsonNode reinstateBody = body(reinstateResponse);
        assertEquals("SUSPENSO", reinstateBody.path("data").path("statusAnterior").asText());
        assertEquals("VIGENTE", reinstateBody.path("data").path("statusAtual").asText());
        assertFalse(reinstateBody.path("_links").path("schema").isMissingNode(), reinstateBody.toPrettyString());

        String revokeEtag = currentEtag(2);
        ResponseEntity<String> revokeResponse = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/2/actions/revoke",
                HttpMethod.POST,
                authorizedCommand("""
                        {
                          "justificativa": "Descumprimento regulatorio grave"
                        }
                        """, revokeEtag, "agreement-revoke-success"),
                String.class
        );
        JsonNode revokeBody = body(revokeResponse);
        assertEquals("SUSPENSO", revokeBody.path("data").path("statusAnterior").asText());
        assertEquals("REVOGADO", revokeBody.path("data").path("statusAtual").asText());
        assertFalse(revokeBody.path("_links").path("schema").isMissingNode(), revokeBody.toPrettyString());
        assertEquals("REVOGADO", jdbcTemplate.queryForObject(
                "select status from public.acordos_regulatorios where id = 2",
                String.class
        ));
    }

    @Test
    void shouldRejectInvalidOrStaleEtagWithoutConsumingIdempotencyKey() {
        String command = "{\"justificativa\":\"Teste de precondicao\"}";

        ResponseEntity<String> missing = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions/suspend",
                HttpMethod.POST,
                authorizedCommand(command, null, "agreement-missing-etag"),
                String.class
        );
        assertEquals(HttpStatus.PRECONDITION_REQUIRED, missing.getStatusCode());

        String currentEtag = currentEtag(1);
        ResponseEntity<String> weak = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions/suspend",
                HttpMethod.POST,
                authorizedCommand(command, "W/" + currentEtag, "agreement-weak-etag"),
                String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, weak.getStatusCode());

        jdbcTemplate.update("update public.acordos_regulatorios set version = version + 1 where id = 1");
        ResponseEntity<String> stale = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/1/actions/suspend",
                HttpMethod.POST,
                authorizedCommand(command, currentEtag, "agreement-stale-etag"),
                String.class
        );
        assertEquals(HttpStatus.PRECONDITION_FAILED, stale.getStatusCode());
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from public.praxis_resource_action_execution where idempotency_key in ('agreement-missing-etag', 'agreement-weak-etag', 'agreement-stale-etag')",
                Integer.class
        ));
    }

    @Test
    void shouldProtectOrdinaryAndPartialUpdatesWithoutLettingThemBypassWorkflowStatus() throws Exception {
        String currentEtag = currentEtag(3);

        ResponseEntity<String> missingPutPrecondition = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/3",
                HttpMethod.PUT,
                authorizedJson("""
                        {
                          "nome": "Acordo Artico revisado",
                          "jurisdicao": "Canada",
                          "status": "VIGENTE",
                          "descricao": "Metadados atualizados"
                        }
                        """),
                String.class
        );
        assertEquals(HttpStatus.PRECONDITION_REQUIRED, missingPutPrecondition.getStatusCode());

        ResponseEntity<String> missingReviewPrecondition = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/3/review",
                HttpMethod.PATCH,
                authorizedJson("""
                        {
                          "jurisdicao": "Canada",
                          "descricao": "Revisao sem versao"
                        }
                        """),
                String.class
        );
        assertEquals(HttpStatus.PRECONDITION_REQUIRED, missingReviewPrecondition.getStatusCode());

        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/api/operations/acordos-regulatorios/3",
                HttpMethod.PUT,
                authorizedVersionedJson("""
                        {
                          "nome": "Acordo Artico revisado",
                          "jurisdicao": "Canada",
                          "status": "VIGENTE",
                          "descricao": "Metadados atualizados"
                        }
                        """, currentEtag),
                String.class
        );
        JsonNode updated = body(updateResponse).path("data");
        assertEquals("Acordo Artico revisado", updated.path("nome").asText());
        assertEquals("REVOGADO", updated.path("status").asText());
        assertNotNull(updateResponse.getHeaders().getETag());
        assertFalse(currentEtag.equals(updateResponse.getHeaders().getETag()));
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

    private HttpEntity<String> authorizedCommand(String json, String etag, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("admin", "ADMIN"));
        if (etag != null) {
            headers.setIfMatch(etag);
        }
        headers.set("Idempotency-Key", idempotencyKey);
        headers.set("X-Correlation-ID", "acordos-pilot-" + idempotencyKey);
        return new HttpEntity<>(json, headers);
    }

    private HttpEntity<String> authorizedVersionedJson(String json, String etag) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("admin", "ADMIN"));
        if (etag != null) {
            headers.setIfMatch(etag);
        }
        return new HttpEntity<>(json, headers);
    }

    private String currentEtag(Integer id) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/operations/acordos-regulatorios/" + id,
                String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().getETag());
        return response.getHeaders().getETag();
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
