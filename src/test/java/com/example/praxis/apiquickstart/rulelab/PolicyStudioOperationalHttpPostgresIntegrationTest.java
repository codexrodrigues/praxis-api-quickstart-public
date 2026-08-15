package com.example.praxis.apiquickstart.rulelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHead;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHeadActivationType;
import org.praxisplatform.config.domain.DomainRuleChangeWorkspace;
import org.praxisplatform.config.domain.DomainRuleTestScenario;
import org.praxisplatform.config.repository.DomainRuleChangeWorkspaceRepository;
import org.praxisplatform.config.repository.DomainRuleTestRunRepository;
import org.praxisplatform.config.repository.DomainRuleTestRunResultRepository;
import org.praxisplatform.config.repository.DomainRuleTestScenarioRepository;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** PostgreSQL/HTTP proof of the governed four-result operational Test Run command. */
@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.banner-mode=off",
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.read-open=true",
                "app.security.write-disabled=false",
                "app.security.schemas-aggregator.enabled=false",
                "app.security.csrf.disable=true",
                "app.session.cookie-name=SESSION",
                "app.session.secure=false",
                "app.session.samesite=Lax",
                "praxis.openapi.prewarm.enabled=false",
                "praxis.rule-lab.snapshot.enabled=false",
                "praxis.rule-lab.policy-studio.seed.enabled=false",
                "praxis.rule-lab.authoritative-facts.enabled=true",
                "praxis.rule-lab.http-simulation-enabled=true",
                "praxis.rule-lab.authoritative-facts.organization-key=DEMO-ORG",
                "praxis.rule-lab.authoritative-facts.scope-hmac-key=0123456789abcdef0123456789abcdef",
                "praxis.ai.security.allow-default-tenant-in-corporate=true",
                "praxis.ai.security.server-default-tenant=desenv",
                "praxis.ai.security.server-default-environment=local",
                "praxis.ai.provider=mock",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none",
                "config.jpa.hibernate.ddl-auto=none"
        })
class PolicyStudioOperationalHttpPostgresIntegrationTest {
    private static final Instant EVALUATED_AT = Instant.parse("2026-07-13T12:00:00Z");
    private static final String IDEMPOTENCY_KEY = "quickstart:v59:four-result";
    private static final String ENDPOINT = "/api/human-resources/extraordinary-benefit-requests/actions/"
            + "run-policy-studio-operational-test";
    private static final EmbeddedPostgres OPERATIONAL = startPostgres(
            "/rule-lab/policy-studio-operational-v59-api-schema.sql");
    private static final EmbeddedPostgres CONFIG = startPostgres(
            "/rule-lab/policy-studio-operational-v59-config-schema.sql");

    @DynamicPropertySource
    static void databases(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> OPERATIONAL.getJdbcUrl("postgres", "postgres"));
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("config.datasource.url", () -> CONFIG.getJdbcUrl("postgres", "postgres"));
        properties.add("config.datasource.username", () -> "postgres");
        properties.add("config.datasource.password", () -> "");
        properties.add("config.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private DomainRuleChangeWorkspaceRepository workspaces;
    @Autowired private DomainRuleTestScenarioRepository scenarios;
    @Autowired private DomainRuleTestRunRepository runs;
    @Autowired private DomainRuleTestRunResultRepository results;
    @Autowired private ExtraordinaryGrantRuleSnapshotRuntime runtime;
    @Autowired @Qualifier("extraordinaryGrantRuleExecutorRegistry")
    private RuleBindingExecutorRegistry registry;
    @Autowired @Qualifier("apiJdbcTemplate") private JdbcTemplate api;
    @MockBean(name = "ragVectorStore") private VectorStore ragVectorStore;

    private UUID workspaceId;
    private UUID workspaceEtag;
    private Map<String, UUID> scenarioIds;

    @BeforeEach
    void seedGovernedWorkspaceAndActiveRuntime() throws Exception {
        results.deleteAllInBatch();
        runs.deleteAllInBatch();
        scenarios.deleteAllInBatch();
        workspaces.deleteAllInBatch();
        api.update("delete from extraordinary_benefit_grant_effect");
        api.update("delete from extraordinary_benefit_transformation_audit");
        api.update("delete from extraordinary_benefit_request");
        api.update("delete from rule_execution_observation_outbox");
        api.update("delete from praxis_resource_action_execution");

        workspaceId = UUID.randomUUID();
        workspaceEtag = UUID.randomUUID();
        workspaces.saveAndFlush(DomainRuleChangeWorkspace.builder()
                .id(workspaceId)
                .tenantId("desenv")
                .environment("local")
                .ruleKey("grant.amount-parameters")
                .baseDefinitionId(UUID.randomUUID())
                .baseDefinitionVersion(1)
                .baseDefinitionHash("A".repeat(64))
                .title("Remote operational compatibility proof")
                .status("OPEN")
                .draftCondition("{\"<=\":[{\"var\":\"request.requestedAmount\"},{\"var\":\"program.maxAmount\"}]}")
                .draftParameters("{}")
                .etag(workspaceEtag)
                .revision(1L)
                .createdBy("policy-proof-operator")
                .updatedBy("policy-proof-operator")
                .createdAt(EVALUATED_AT.minusSeconds(60))
                .updatedAt(EVALUATED_AT.minusSeconds(60))
                .rowVersion(0L)
                .build());
        scenarioIds = new LinkedHashMap<>();
        scenarioIds.put("create-allow", saveScenario("create-allow", "2500.00", "ALLOW"));
        scenarioIds.put("create-deny", saveScenario("create-deny", "6000.00", "DENY"));
        scenarioIds.put("update-allow", saveScenario("update-allow", "2000.00", "ALLOW"));
        scenarioIds.put("update-deny", saveScenario("update-deny", "6000.00", "DENY"));

        PublishedRuleSnapshot snapshot = snapshot();
        String contentHash = new PraxisRuleSnapshotCompiler(registry)
                .compile(snapshot, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION)
                .snapshotContentHash();
        runtime.activate(
                new PublishedRuleSnapshotHead(
                        snapshot, contentHash, "policy-v59-active-head", 1,
                        PublishedRuleSnapshotHeadActivationType.ACTIVE),
                "desenv", "local", EVALUATED_AT.minusSeconds(30));
    }

    @Test
    void recordsOneFourResultRunAndFailsClosedForReplayConflictAndUnauthorizedCaller()
            throws Exception {
        String request = requestBody("UPDATE");
        HttpHeaders operator = headers(workspaceEtag.toString(), IDEMPOTENCY_KEY, true);

        ResponseEntity<String> first = restTemplate.exchange(
                ENDPOINT, HttpMethod.POST, new HttpEntity<>(request, operator), String.class);

        assertEquals(HttpStatus.OK, first.getStatusCode(), first.getBody());
        JsonNode firstData = responseData(first);
        assertEquals(workspaceId.toString(), firstData.path("workspaceId").asText());
        assertEquals(4, firstData.path("results").size());
        Map<String, Boolean> mutationByScenario = Map.of(
                "create-allow", true,
                "create-deny", false,
                "update-allow", true,
                "update-deny", false);
        for (JsonNode result : firstData.path("results")) {
            JsonNode evidence = result.path("operationalEvidence");
            String scenarioKey = result.path("scenarioKey").asText();
            assertEquals(mutationByScenario.get(scenarioKey), evidence.path("mutationObserved").asBoolean());
            assertEquals(scenarioKey.startsWith("create") ? "CREATE" : "UPDATE",
                    evidence.path("operationMode").asText());
            assertTrue(evidence.path("cleanupVerified").asBoolean());
            assertEquals(0, evidence.path("baselineCallCount").asInt());
            assertEquals("MATCH", result.path("candidateBaselineComparison").asText());
        }
        assertEquals(1, runs.count());
        assertEquals(4, results.count());
        assertEquals(1, count("praxis_resource_action_execution"));
        assertOperationalTablesAreClean();

        ResponseEntity<String> replay = restTemplate.exchange(
                ENDPOINT, HttpMethod.POST, new HttpEntity<>(request, operator), String.class);
        assertEquals(HttpStatus.OK, replay.getStatusCode(), replay.getBody());
        assertEquals(firstData.path("runId").asText(), responseData(replay).path("runId").asText());
        assertEquals(1, runs.count());
        assertEquals(4, results.count());
        assertEquals(1, count("praxis_resource_action_execution"));
        assertOperationalTablesAreClean();

        ResponseEntity<String> conflictingReplay = restTemplate.exchange(
                ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(requestBody("CREATE"), operator),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflictingReplay.getStatusCode());
        assertEquals(1, runs.count());
        assertEquals(4, results.count());
        assertEquals(1, count("praxis_resource_action_execution"));
        assertOperationalTablesAreClean();

        ResponseEntity<String> stale = restTemplate.exchange(
                ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(request, headers("stale", "quickstart:v59:stale", true)),
                String.class);
        assertEquals(HttpStatus.PRECONDITION_FAILED, stale.getStatusCode());

        ResponseEntity<String> missingPrecondition = restTemplate.exchange(
                ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(request, headers(null, "quickstart:v59:missing-etag", true)),
                String.class);
        assertEquals(HttpStatus.PRECONDITION_REQUIRED, missingPrecondition.getStatusCode());

        ResponseEntity<String> forbidden = restTemplate.exchange(
                ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(request, headers(
                        workspaceEtag.toString(), "quickstart:v59:author-only", false)),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        assertEquals(1, runs.count());
        assertEquals(4, results.count());
        assertOperationalTablesAreClean();
    }

    private UUID saveScenario(String key, String amount, String expectedDecision) throws Exception {
        UUID id = UUID.randomUUID();
        scenarios.saveAndFlush(DomainRuleTestScenario.builder()
                .id(id)
                .workspaceId(workspaceId)
                .tenantId("desenv")
                .environment("local")
                .scenarioKey(key)
                .name(key)
                .facts(objectMapper.readTree("""
                        {
                          "actor":{"permissions":["benefit:request"]},
                          "worker":{"status":"ACTIVE"},
                          "grant":{"hasDuplicate":false},
                          "program":{"active":true,"maxAmount":5000.00},
                          "customer":{"additionalEligible":true},
                          "payment":{"requestedDate":"2026-07-20","allowedDates":["2026-07-20","2026-07-27"]},
                          "budget":{"availableAmount":25000.00},
                          "request":{"reasonCode":"FAMILY_HARDSHIP","eventDate":"2026-07-13","requestedAmount":%s}
                        }
                        """.formatted(amount)).toString())
                .expectedDecision(expectedDecision)
                .expectedReasonCodes("DENY".equals(expectedDecision)
                        ? "[\"REQUESTED_AMOUNT_EXCEEDS_PROGRAM_LIMIT\"]" : "[]")
                .expectedEffectIntents("ALLOW".equals(expectedDecision)
                        ? "[\"REGISTER_EXTRAORDINARY_GRANT\"]" : "[]")
                .status("ACTIVE")
                .etag(UUID.randomUUID())
                .revision(1L)
                .createdBy("policy-proof-operator")
                .updatedBy("policy-proof-operator")
                .createdAt(EVALUATED_AT.minusSeconds(30))
                .updatedAt(EVALUATED_AT.minusSeconds(30))
                .rowVersion(0L)
                .build());
        return id;
    }

    private String requestBody(String updateDenyOperation) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "workspaceId", workspaceId,
                "scenarios", List.of(
                        Map.of("scenarioId", scenarioIds.get("create-allow"), "operationMode", "CREATE"),
                        Map.of("scenarioId", scenarioIds.get("create-deny"), "operationMode", "CREATE"),
                        Map.of("scenarioId", scenarioIds.get("update-allow"), "operationMode", "UPDATE"),
                        Map.of("scenarioId", scenarioIds.get("update-deny"),
                                "operationMode", updateDenyOperation)),
                "userTimeZone", "America/Sao_Paulo",
                "evaluatedAtUtc", EVALUATED_AT));
    }

    private HttpHeaders headers(String etag, String idempotencyKey, boolean operationalAuthority) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (etag != null) headers.set(HttpHeaders.IF_MATCH, '"' + etag + '"');
        headers.set("Idempotency-Key", idempotencyKey);
        headers.set("X-Correlation-ID", idempotencyKey);
        headers.set("X-Tenant-ID", "desenv");
        headers.set("X-Env", "local");
        List<String> authorities = operationalAuthority
                ? List.of(RuleGovernanceAuthorities.OPERATIONAL_TEST_OPERATOR)
                : List.of(RuleGovernanceAuthorities.DEFINITION_AUTHOR);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate(
                "policy-proof-operator", "HUMAN", authorities));
        return headers;
    }

    private JsonNode responseData(ResponseEntity<String> response) throws Exception {
        assertNotNull(response.getBody());
        return objectMapper.readTree(response.getBody()).path("data");
    }

    private void assertOperationalTablesAreClean() {
        assertEquals(0, count("extraordinary_benefit_request"));
        assertEquals(0, count("extraordinary_benefit_transformation_audit"));
        assertEquals(0, count("extraordinary_benefit_grant_effect"));
    }

    private int count(String table) {
        return api.queryForObject("select count(*) from " + table, Integer.class);
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

    private static EmbeddedPostgres startPostgres(String schemaResource) {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                    .setCleanDataDirectory(true)
                    .setRegisterShutdownHook(true)
                    .start();
            try (Connection connection = postgres.getPostgresDatabase().getConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute(readResource(schemaResource));
            }
            return postgres;
        } catch (Exception failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = PolicyStudioOperationalHttpPostgresIntegrationTest.class
                .getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Missing PostgreSQL test schema " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
