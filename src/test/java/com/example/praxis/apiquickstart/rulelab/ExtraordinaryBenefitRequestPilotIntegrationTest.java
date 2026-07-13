package com.example.praxis.apiquickstart.rulelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.dto.DomainRuleSnapshotActivationResponse;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;
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

@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.read-open=true",
                "app.security.write-disabled=false",
                "app.security.demo-allow-bulk-actions=false",
                "app.security.schemas-aggregator.enabled=true",
                "app.security.csrf.disable=true",
                "app.session.cookie-name=SESSION",
                "app.session.secure=false",
                "app.session.samesite=Lax",
                "praxis.rule-lab.snapshot.enabled=false",
                "praxis.ai.provider=mock",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:quickstart_ql04_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_ql04_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        })
class ExtraordinaryBenefitRequestPilotIntegrationTest {
    private static final Instant ACTIVATION_TIME = Instant.parse("2026-07-13T12:00:00Z");

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private ExtraordinaryGrantRuleSnapshotRuntime runtime;
    @Autowired
    @Qualifier("extraordinaryGrantRuleExecutorRegistry")
    private RuleBindingExecutorRegistry registry;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @BeforeEach
    void activateGovernedSnapshot() {
        PublishedRuleSnapshot snapshot = snapshot();
        String contentHash = new PraxisRuleSnapshotCompiler(registry)
                .compile(snapshot, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION)
                .snapshotContentHash();
        runtime.activate(
                new DomainRuleSnapshotActivationResponse(snapshot, contentHash, "ql04-head-1", 1, "ACTIVE"),
                "desenv",
                "local",
                ACTIVATION_TIME);
    }

    @Test
    void evaluatesBusinessRequestThroughAuthenticatedHttpWithoutPersistenceOrEffects() throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate",
                authorizedJson(eligiblePayload()),
                String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode data = body(response).path("data");
        assertEquals("BEN-2026-000184", data.path("requestReference").asText());
        assertEquals("ALLOW", data.path("outcome").asText());
        assertEquals("extraordinary-grant-v1", data.path("snapshotKey").asText());
        assertEquals("2500", data.path("recommendedAmount").decimalValue().stripTrailingZeros().toPlainString());
        assertEquals("PLANNED_NOT_EXECUTED", data.path("plannedEffectStatus").asText());
        assertFalse(data.path("persisted").asBoolean());
        assertFalse(data.path("effectExecuted").asBoolean());
        assertTrue(data.path("factsDigest").asText().matches("[A-F0-9]{64}"));
        assertFalse(body(response).path("_links").path("actions").isMissingNode());
        assertFalse(body(response).path("_links").path("capabilities").isMissingNode());
    }

    @Test
    void publishesActionCapabilitiesAndRequestSchemaFromCanonicalDiscovery() throws Exception {
        JsonNode actions = body(restTemplate.getForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions", String.class));
        JsonNode evaluateAction = findById(actions.path("actions"), "evaluate");
        assertNotNull(evaluateAction);
        assertEquals("COLLECTION", evaluateAction.path("scope").asText());

        JsonNode capabilities = body(restTemplate.getForEntity(
                "/api/human-resources/extraordinary-benefit-requests/capabilities", String.class));
        assertNotNull(findById(capabilities.path("actions"), "evaluate"));
        assertFalse(capabilities.path("canonicalOperations").path("create").asBoolean());

        JsonNode schema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/human-resources/extraordinary-benefit-requests/actions/evaluate&operation=post&schemaType=request",
                String.class));
        JsonNode requestedAmount = schema.path("properties").path("requestedAmount");
        assertTrue(requestedAmount.path("description").asText().contains("Valor monetario solicitado"));
        assertEquals("currency", requestedAmount.path("x-ui").path("controlType").asText());
        assertTrue(schema.path("required").toString().contains("requestReference"));

        JsonNode globalActions = body(restTemplate.getForEntity(
                "/schemas/actions?resource=human-resources.extraordinary-benefit-requests",
                String.class));
        assertNotNull(findById(globalActions.path("actions"), "evaluate"));

        ResponseEntity<String> surfaces = restTemplate.getForEntity(
                "/schemas/surfaces?resource=human-resources.extraordinary-benefit-requests",
                String.class);
        assertEquals(HttpStatus.NOT_FOUND, surfaces.getStatusCode(),
                "command-only resources must remain outside the surface registry");
    }

    @Test
    void rejectsUnauthenticatedBusinessEvaluationEvenWhenReadDiscoveryIsOpen() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate",
                new HttpEntity<>(eligiblePayload(), headers),
                String.class);

        assertTrue(response.getStatusCode() == HttpStatus.UNAUTHORIZED
                || response.getStatusCode() == HttpStatus.FORBIDDEN);
    }

    @Test
    void rejectsUnknownIanaTimeZoneAsCanonicalValidationFailure() throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate",
                authorizedJson(eligiblePayload().replace("America/Sao_Paulo", "Invalid/Zone")),
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), response.getBody());
        JsonNode body = objectMapper.readTree(response.getBody());
        JsonNode problem = body.path("errors").path(0);
        assertEquals("VALIDATION_FAILED", problem.path("outcome").asText());
        assertEquals("VALIDATION_FAILED", problem.path("code").asText());
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        assertNotNull(response.getBody());
        return objectMapper.readTree(response.getBody());
    }

    private HttpEntity<String> authorizedJson(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("rule-lab-admin", "ADMIN"));
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

    private String eligiblePayload() {
        return """
                {
                  "requestReference": "BEN-2026-000184",
                  "reasonCode": "FAMILY_HARDSHIP",
                  "eventDate": "2026-07-13",
                  "requestedAmount": 2500.00,
                  "workerStatus": "ACTIVE",
                  "duplicateGrant": false,
                  "programActive": true,
                  "programMaximumAmount": 5000.00,
                  "customerAdditionalEligible": true,
                  "requestedPaymentDate": "2026-07-20",
                  "allowedPaymentDates": ["2026-07-20", "2026-08-05"],
                  "availableBudgetAmount": 100000.00,
                  "userTimeZone": "America/Sao_Paulo"
                }
                """;
    }

    private PublishedRuleSnapshot snapshot() {
        return new PublishedRuleSnapshot(
                PublishedRuleSnapshot.SNAPSHOT_CONTRACT_VERSION,
                "extraordinary-grant-v1",
                "desenv",
                "local",
                ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY,
                1,
                "2026-07-13T11:00:00Z",
                null,
                ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION,
                "2026-01-01T00:00:00Z",
                null,
                List.of(
                        new RuleSnapshotSource("definition-1", "grant:eligibility", 1, "A".repeat(64)),
                        new RuleSnapshotSource("definition-2", "grant:amount", 1, "B".repeat(64))),
                List.of(
                        new RuleSnapshotApproval(
                                "approval-1", "RULE_DEFINITION_APPROVER", "approver-a",
                                "2026-07-13T10:00:00Z", "A".repeat(64)),
                        new RuleSnapshotApproval(
                                "approval-2", "RULE_DEFINITION_APPROVER", "approver-b",
                                "2026-07-13T10:05:00Z", "B".repeat(64))),
                ExtraordinaryGrantRuleSetFactory.definition());
    }
}
