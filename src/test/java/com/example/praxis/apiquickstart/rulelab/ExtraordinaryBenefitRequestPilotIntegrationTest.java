package com.example.praxis.apiquickstart.rulelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpMethod;

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
                "praxis.resource-version.etag.secret=test-secret-resource-version",
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
    @Qualifier("apiJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ExtraordinaryGrantRuleSnapshotRuntime runtime;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    @Qualifier("extraordinaryGrantRuleExecutorRegistry")
    private RuleBindingExecutorRegistry registry;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @BeforeEach
    void activateGovernedSnapshot() {
        createOperationalSchema();
        jdbcTemplate.update("delete from public.praxis_resource_action_transition");
        jdbcTemplate.update("delete from public.praxis_resource_action_execution");
        jdbcTemplate.update("delete from public.extraordinary_benefit_grant_effect");
        jdbcTemplate.update("delete from public.extraordinary_benefit_request");
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
    void persistsAllowedRequestWithoutExecutingEffectAndReplaysIdempotently() throws Exception {
        HttpEntity<String> command = authorizedJson(eligiblePayload());
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate",
                command,
                String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode data = body(response).path("data");
        JsonNode evaluation = data.path("evaluation");
        assertEquals("BEN-2026-000184", evaluation.path("requestReference").asText());
        assertEquals("ALLOW", evaluation.path("outcome").asText());
        assertEquals("extraordinary-grant-v1", evaluation.path("snapshotKey").asText());
        assertEquals("2500", evaluation.path("recommendedAmount").decimalValue().stripTrailingZeros().toPlainString());
        assertEquals("PLANNED_NOT_EXECUTED", evaluation.path("plannedEffectStatus").asText());
        assertTrue(evaluation.path("persisted").asBoolean());
        assertFalse(evaluation.path("effectExecuted").asBoolean());
        assertTrue(evaluation.path("factsDigest").asText().matches("[A-F0-9]{64}"));
        assertEquals("EVALUATED", data.path("resource").path("lifecycleStatus").asText());
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_request", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_grant_effect", Integer.class));
        assertTrue(body(response).path("_links").isObject());

        ResponseEntity<String> replay = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate", command, String.class);
        assertEquals(HttpStatus.OK, replay.getStatusCode(), replay.getBody());
        assertEquals(data.path("resource").path("id"), body(replay).path("data").path("resource").path("id"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_request", Integer.class));

        JsonNode persisted = body(restTemplate.getForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + data.path("resource").path("id").asLong(),
                String.class)).path("data");
        assertEquals(evaluation.path("businessMessage"), persisted.path("evaluation").path("businessMessage"));
        assertEquals(evaluation.path("reasonCodes"), persisted.path("evaluation").path("reasonCodes"));
    }

    @Test
    void publishesActionCapabilitiesAndRequestSchemaFromCanonicalDiscovery() throws Exception {
        JsonNode actions = body(restTemplate.getForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions", String.class));
        JsonNode evaluateAction = findById(actions.path("actions"), "evaluate");
        assertNotNull(evaluateAction);
        assertEquals("COLLECTION", evaluateAction.path("scope").asText());
        assertEquals("COLLECTION", findById(actions.path("actions"), "evaluate-batch").path("scope").asText());
        assertEquals("COLLECTION", findById(actions.path("actions"), "shadow-compare").path("scope").asText());

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

        JsonNode shadowResponseSchema = body(restTemplate.getForEntity(
                "/schemas/filtered?path=/api/human-resources/extraordinary-benefit-requests/actions/shadow-compare&operation=post&schemaType=response",
                String.class));
        assertTrue(shadowResponseSchema.path("description").asText().contains("sanitizada"));
        assertTrue(shadowResponseSchema.path("properties").has("comparisonStatus"));
        assertFalse(shadowResponseSchema.path("properties").has("requestReference"));
        assertFalse(shadowResponseSchema.path("properties").has("recommendedAmount"));

        JsonNode globalActions = body(restTemplate.getForEntity(
                "/schemas/actions?resource=human-resources.extraordinary-benefit-requests",
                String.class));
        assertNotNull(findById(globalActions.path("actions"), "evaluate"));
        assertNotNull(findById(globalActions.path("actions"), "shadow-compare"));
        assertEquals("ITEM", findById(globalActions.path("actions"), "submit").path("scope").asText());
        assertEquals("ITEM", findById(globalActions.path("actions"), "approve").path("scope").asText());
        assertEquals("ITEM", findById(globalActions.path("actions"), "apply").path("scope").asText());

        ResponseEntity<String> surfaces = restTemplate.getForEntity(
                "/schemas/surfaces?resource=human-resources.extraordinary-benefit-requests",
                String.class);
        assertEquals(HttpStatus.OK, surfaces.getStatusCode(),
                "persistent read-only resources must participate in canonical discovery");
    }

    @Test
    void shadowComparesAllowAndDenyWithoutPersistingAnyOperationalLedger() throws Exception {
        double beforeMatches = counter("match");
        ResponseEntity<String> allowResponse = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/shadow-compare",
                authorizedShadowJson(eligiblePayload()), String.class);
        JsonNode allow = body(allowResponse).path("data");

        assertEquals("MATCH", allow.path("comparisonStatus").asText());
        assertEquals("ALLOW", allow.path("baselineOutcome").asText());
        assertEquals("ALLOW", allow.path("candidateOutcome").asText());
        assertTrue(allow.path("sanitized").asBoolean());
        assertFalse(allow.path("persisted").asBoolean());
        assertFalse(allow.path("effectExecuted").asBoolean());
        assertFalse(allowResponse.getBody().contains("BEN-2026-000184"));
        assertFalse(allow.has("requestReference"));
        assertFalse(allow.has("recommendedAmount"));

        String deniedPayload = eligiblePayload().replace("\"duplicateGrant\": false", "\"duplicateGrant\": true");
        JsonNode deny = body(restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/shadow-compare",
                authorizedShadowJson(deniedPayload), String.class)).path("data");
        assertEquals("MATCH", deny.path("comparisonStatus").asText());
        assertEquals("DENY", deny.path("baselineOutcome").asText());
        assertEquals("DENY", deny.path("candidateOutcome").asText());

        assertEquals(0, tableCount("extraordinary_benefit_request"));
        assertEquals(0, tableCount("extraordinary_benefit_grant_effect"));
        assertEquals(0, tableCount("praxis_resource_action_execution"));
        assertEquals(0, tableCount("praxis_resource_action_transition"));
        assertEquals(beforeMatches + 2.0, counter("match"));
    }

    @Test
    void rejectsUnauthenticatedShadowComparisonWithoutRunningOrPersistingIt() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/shadow-compare",
                new HttpEntity<>(eligiblePayload(), headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(0, tableCount("extraordinary_benefit_request"));
        assertEquals(0, tableCount("extraordinary_benefit_grant_effect"));
        assertEquals(0, tableCount("praxis_resource_action_execution"));
        assertEquals(0, tableCount("praxis_resource_action_transition"));
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

    @Test
    void enforcesEtagLifecycleAndExecutesTheEffectExactlyOnce() throws Exception {
        JsonNode evaluation = body(restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate",
                authorizedJson(eligiblePayload(), "lifecycle-evaluate", null), String.class));
        long id = evaluation.path("data").path("resource").path("id").asLong();

        ResponseEntity<String> detail = restTemplate.exchange(
                "/api/human-resources/extraordinary-benefit-requests/" + id,
                HttpMethod.GET, HttpEntity.EMPTY, String.class);
        assertEquals(HttpStatus.OK, detail.getStatusCode(), detail.getBody());
        String evaluatedEtag = detail.getHeaders().getETag();
        assertNotNull(evaluatedEtag);

        String transition = """
                {"justification":"Aprovacao documentada no piloto QL-05.","effectiveAt":"2026-07-13"}
                """;
        ResponseEntity<String> missingPrecondition = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/submit",
                authorizedJson(transition, "submit-missing-etag", null), String.class);
        assertEquals(HttpStatus.PRECONDITION_REQUIRED, missingPrecondition.getStatusCode(), missingPrecondition.getBody());

        ResponseEntity<String> submit = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/submit",
                authorizedJson(transition, "submit-1", evaluatedEtag), String.class);
        assertEquals(HttpStatus.OK, submit.getStatusCode(), submit.getBody());
        String submittedEtag = submit.getHeaders().getETag();

        ResponseEntity<String> staleApprove = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/approve",
                authorizedJson(transition, "approve-stale", evaluatedEtag), String.class);
        assertEquals(HttpStatus.PRECONDITION_FAILED, staleApprove.getStatusCode(), staleApprove.getBody());

        ResponseEntity<String> approve = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/approve",
                authorizedJson(transition, "approve-1", submittedEtag), String.class);
        assertEquals(HttpStatus.OK, approve.getStatusCode(), approve.getBody());
        String approvedEtag = approve.getHeaders().getETag();

        HttpEntity<String> applyCommand = authorizedJson(transition, "apply-1", approvedEtag);
        ResponseEntity<String> apply = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/apply",
                applyCommand, String.class);
        assertEquals(HttpStatus.OK, apply.getStatusCode(), apply.getBody());
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_grant_effect where benefit_request_id = ?",
                Integer.class, id));
        assertEquals("APPLIED", jdbcTemplate.queryForObject(
                "select lifecycle_status from public.extraordinary_benefit_request where id = ?", String.class, id));

        ResponseEntity<String> replay = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/apply",
                applyCommand, String.class);
        assertEquals(HttpStatus.OK, replay.getStatusCode(), replay.getBody());
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_grant_effect where benefit_request_id = ?",
                Integer.class, id));
        assertEquals(3, jdbcTemplate.queryForObject(
                "select count(*) from public.praxis_resource_action_transition where resource_id = ?",
                Integer.class, Long.toString(id)));
    }

    @Test
    void denyDoesNotPersistAndReusedKeyWithAnotherPayloadConflicts() throws Exception {
        String denied = eligiblePayload()
                .replace("BEN-2026-000184", "BEN-2026-DENIED")
                .replace("\"duplicateGrant\": false", "\"duplicateGrant\": true");
        JsonNode denial = body(restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate",
                authorizedJson(denied, "deny-1", null), String.class));
        assertEquals("DENY", denial.path("data").path("evaluation").path("outcome").asText());
        assertTrue(denial.path("data").path("resource").isNull());
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_request", Integer.class));

        ResponseEntity<String> conflict = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate",
                authorizedJson(eligiblePayload(), "deny-1", null), String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode(), conflict.getBody());
    }

    @Test
    void batchKeepsOrderAndCommitsSuccessfulItemsIndependently() throws Exception {
        String allowed = eligiblePayload().replace("BEN-2026-000184", "BEN-2026-BATCH-ALLOW");
        String denied = eligiblePayload()
                .replace("BEN-2026-000184", "BEN-2026-BATCH-DENY")
                .replace("\"duplicateGrant\": false", "\"duplicateGrant\": true");
        String batch = "{\"requests\":[" + allowed + "," + denied + "," + allowed + "]}";

        JsonNode response = body(restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate-batch",
                authorizedJson(batch, "batch-mixed-1", null), String.class));
        JsonNode data = response.path("data");
        assertFalse(data.path("atomic").asBoolean());
        assertEquals(3, data.path("total").asInt());
        assertEquals(1, data.path("persisted").asInt());
        assertEquals("PERSISTED", data.path("items").path(0).path("code").asText());
        assertEquals("DENY", data.path("items").path(1).path("code").asText());
        assertEquals("DUPLICATE_REFERENCE", data.path("items").path(2).path("code").asText());
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_request", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_grant_effect", Integer.class));
    }

    @Test
    void effectConflictRollsBackApplyAndLeavesRequestApproved() throws Exception {
        JsonNode evaluation = body(restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate",
                authorizedJson(eligiblePayload(), "rollback-evaluate", null), String.class));
        long id = evaluation.path("data").path("resource").path("id").asLong();
        String etag = restTemplate.exchange(
                "/api/human-resources/extraordinary-benefit-requests/" + id,
                HttpMethod.GET, HttpEntity.EMPTY, String.class).getHeaders().getETag();
        String transition = """
                {"justification":"Prova de rollback transacional QL-05.","effectiveAt":"2026-07-13"}
                """;
        ResponseEntity<String> submit = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/submit",
                authorizedJson(transition, "rollback-submit", etag), String.class);
        ResponseEntity<String> approve = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/approve",
                authorizedJson(transition, "rollback-approve", submit.getHeaders().getETag()), String.class);
        assertEquals(HttpStatus.OK, approve.getStatusCode(), approve.getBody());

        jdbcTemplate.update("""
                insert into public.extraordinary_benefit_grant_effect
                    (effect_execution_id, benefit_request_id, request_reference, intent_type,
                     amount, currency, executed_at, executed_by)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), id, "BEN-2026-000184", "GRANT_EXTRAORDINARY_BENEFIT",
                2500, "BRL", Instant.parse("2026-07-13T12:00:00Z"), "rollback-fixture");

        ResponseEntity<String> apply = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/apply",
                authorizedJson(transition, "rollback-apply", approve.getHeaders().getETag()), String.class);
        assertEquals(HttpStatus.CONFLICT, apply.getStatusCode(), apply.getBody());
        assertEquals("APPROVED", jdbcTemplate.queryForObject(
                "select lifecycle_status from public.extraordinary_benefit_request where id = ?", String.class, id));
        assertEquals("PLANNED", jdbcTemplate.queryForObject(
                "select effect_status from public.extraordinary_benefit_request where id = ?", String.class, id));
        assertEquals(2, jdbcTemplate.queryForObject(
                "select count(*) from public.praxis_resource_action_transition where resource_id = ?",
                Integer.class, Long.toString(id)));
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        assertNotNull(response.getBody());
        return objectMapper.readTree(response.getBody());
    }

    private HttpEntity<String> authorizedJson(String json) {
        return authorizedJson(json, "test-" + Integer.toUnsignedString(json.hashCode()), null);
    }

    private HttpEntity<String> authorizedShadowJson(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("rule-lab-admin", "ADMIN"));
        return new HttpEntity<>(json, headers);
    }

    private int tableCount(String table) {
        return jdbcTemplate.queryForObject("select count(*) from public." + table, Integer.class);
    }

    private double counter(String result) {
        var counter = meterRegistry.find("praxis.rule.shadow.comparisons")
                .tag("result", result)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }

    private HttpEntity<String> authorizedJson(String json, String idempotencyKey, String ifMatch) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idempotencyKey);
        if (ifMatch != null) headers.set(HttpHeaders.IF_MATCH, ifMatch);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("rule-lab-admin", "ADMIN"));
        return new HttpEntity<>(json, headers);
    }

    private void createOperationalSchema() {
        jdbcTemplate.execute("create schema if not exists public");
        jdbcTemplate.execute("""
                create table if not exists public.extraordinary_benefit_request (
                    id bigint generated by default as identity primary key, request_reference varchar(80) not null unique,
                    reason_code varchar(40) not null, event_date date not null, requested_amount numeric(15,2) not null,
                    worker_status varchar(20) not null, duplicate_grant boolean not null, program_active boolean not null,
                    program_maximum_amount numeric(15,2) not null, customer_additional_eligible boolean,
                    requested_payment_date date not null, allowed_payment_dates varchar(1000) not null,
                    available_budget_amount numeric(15,2) not null, user_time_zone varchar(80) not null,
                    lifecycle_status varchar(20) not null, recommended_amount numeric(15,2) not null,
                    currency varchar(3) not null, snapshot_key varchar(200) not null,
                    snapshot_content_hash varchar(64) not null, snapshot_activation_revision bigint not null,
                    rule_set_key varchar(200) not null, rule_set_version integer not null,
                    facts_digest varchar(64) not null, plan_digest varchar(64) not null,
                    planned_effect_intent varchar(120) not null, evaluation_business_message varchar(1000) not null,
                    evaluation_reason_codes varchar(1000) not null, effect_status varchar(20) not null,
                    evaluated_at timestamp with time zone not null, submitted_at timestamp with time zone,
                    approved_at timestamp with time zone, applied_at timestamp with time zone,
                    created_by varchar(255) not null, last_transition_by varchar(255) not null,
                    version bigint not null default 0)
                """);
        jdbcTemplate.execute("""
                create table if not exists public.extraordinary_benefit_grant_effect (
                    id bigint generated by default as identity primary key, effect_execution_id uuid not null unique,
                    benefit_request_id bigint not null unique, request_reference varchar(80) not null,
                    intent_type varchar(120) not null, amount numeric(15,2) not null, currency varchar(3) not null,
                    executed_at timestamp with time zone not null, executed_by varchar(255) not null,
                    foreign key (benefit_request_id) references public.extraordinary_benefit_request(id))
                """);
        jdbcTemplate.execute("""
                create table if not exists public.praxis_resource_action_execution (
                    execution_id uuid primary key, resource_key varchar(200) not null, resource_id varchar(128) not null,
                    action_id varchar(120) not null, action_scope varchar(32) not null,
                    idempotency_key varchar(255) not null, request_hash varchar(128) not null,
                    execution_status varchar(32) not null, response_payload json, correlation_id varchar(255) not null,
                    request_id varchar(255), actor_subject varchar(255) not null, actor_authorities varchar(1000),
                    started_at timestamp with time zone not null, completed_at timestamp with time zone,
                    failure_code varchar(120), failure_message varchar(1000),
                    unique(resource_key, resource_id, action_id, actor_subject, idempotency_key))
                """);
        jdbcTemplate.execute("""
                create table if not exists public.praxis_resource_action_transition (
                    transition_id uuid primary key, resource_key varchar(200) not null, resource_id varchar(128) not null,
                    action_id varchar(120) not null, action_scope varchar(32) not null, previous_state varchar(120),
                    resulting_state varchar(120), reason_code varchar(120), comment varchar(1000),
                    effective_at date not null, performed_at timestamp with time zone not null,
                    actor_subject varchar(255) not null, actor_authorities varchar(1000), correlation_id varchar(255) not null,
                    request_id varchar(255), idempotency_key varchar(255), version_before bigint, version_after bigint)
                """);
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
