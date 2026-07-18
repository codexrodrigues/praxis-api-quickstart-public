package com.example.praxis.apiquickstart.rulelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.praxisplatform.config.dto.DomainRuleSnapshotActivationResponse;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

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
                "praxis.rule-lab.snapshot.tenant-id=desenv",
                "praxis.rule-lab.snapshot.environment=local",
                "praxis.rule-lab.shadow.timeout-ms=5000",
                "praxis.rule-lab.authoritative-facts.enabled=true",
                "praxis.rule-lab.authoritative-facts.organization-key=DEMO-ORG",
                "praxis.rule-lab.authoritative-facts.scope-hmac-key=0123456789abcdef0123456789abcdef",
                "praxis.rule-lab.outbox.maximum-attempts=2",
                "praxis.rule-lab.outbox.lease-ms=1000",
                "praxis.rule-lab.outbox.retry-base-ms=1",
                "praxis.rule-lab.outbox.reconciliation-retry-ms=1",
                "praxis.rule-lab.outbox.retention.delivered-days=30",
                "praxis.rule-lab.outbox.retention.batch-size=100",
                "praxis.rule-lab.outbox.replay.quarantine-ms=1000",
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
    private ApplicationContext applicationContext;
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
    private ExtraordinaryBenefitStatementCommandService statementCommandService;
    @Autowired
    private ExtraordinaryBenefitStatementOutboxDispatcher outboxDispatcher;
    @Autowired
    private ExtraordinaryBenefitStatementOutboxReconciler outboxReconciler;
    @Autowired
    private ExtraordinaryBenefitStatementOutboxOperations outboxOperations;
    @Autowired
    private ExtraordinaryBenefitStatementReplayService replayService;
    @Autowired
    private ExtraordinaryBenefitStatementReplayStore replayStore;
    @Autowired
    private ExtraordinaryBenefitStatementOutboxTelemetry outboxTelemetry;
    @Autowired
    @Qualifier("extraordinaryGrantRuleClock")
    private Clock ruleClock;
    @Autowired
    private RuleLabOperationScopeRegistry operationScopeRegistry;
    @SpyBean
    private ExtraordinaryBenefitStatementBarrier statementBarrier;
    @MockBean
    private ExtraordinaryBenefitStatementEventSink statementEventSink;
    @MockBean
    private ExtraordinaryBenefitStatementDeliveryProbe statementDeliveryProbe;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    @Qualifier("extraordinaryGrantRuleExecutorRegistry")
    private RuleBindingExecutorRegistry registry;

    private HttpServer externalConsumer;
    private ExecutorService externalConsumerExecutor;
    private JdbcTemplate externalInbox;
    private HttpExtraordinaryBenefitStatementEventSink httpEventSink;
    private final AtomicBoolean failAfterInboxCommitOnce = new AtomicBoolean();
    private final AtomicBoolean returnInvalidAcknowledgementOnce = new AtomicBoolean();
    private final AtomicInteger forcedDeliveryStatus = new AtomicInteger();

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @Test
    void enablesAuthoritativeFactAcquisitionExplicitlyForThePilot() {
        assertEquals(1, applicationContext.getBeansOfType(ExtraordinaryBenefitFactProvider.class).size());
        assertEquals(1, applicationContext
                .getBeansOfType(ExtraordinaryBenefitAuthoritativeEvaluationService.class)
                .size());
    }

    @BeforeEach
    void activateGovernedSnapshot() throws Exception {
        createOperationalSchema();
        reset(statementEventSink, statementDeliveryProbe);
        startExternalConsumer();
        doAnswer(invocation -> {
            httpEventSink.deliver(invocation.getArgument(0));
            return null;
        }).when(statementEventSink).deliver(any(ExtraordinaryBenefitStatementOutboxDelivery.class));
        doAnswer(invocation -> httpEventSink.findAcknowledgement(invocation.getArgument(0)))
                .when(statementDeliveryProbe).findAcknowledgement(any(UUID.class));
        jdbcTemplate.update("delete from public.praxis_resource_action_transition");
        jdbcTemplate.update("delete from public.extraordinary_benefit_statement_replay_audit");
        jdbcTemplate.update("delete from public.extraordinary_benefit_statement_outbox");
        jdbcTemplate.update("delete from public.praxis_resource_action_execution");
        jdbcTemplate.update("delete from public.extraordinary_benefit_grant_effect");
        jdbcTemplate.update("delete from public.extraordinary_benefit_transformation_audit");
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

    @AfterEach
    void stopExternalConsumer() {
        if (externalConsumer != null) {
            externalConsumer.stop(0);
        }
        if (externalConsumerExecutor != null) {
            externalConsumerExecutor.shutdownNow();
        }
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
        assertEquals(1, tableCount("extraordinary_benefit_transformation_audit"));
        Map<String, Object> transformationAudit = jdbcTemplate.queryForMap("""
                select proposal_key, binding_key, target_path, schema_ref,
                       proposal_identity_digest, before_digest, after_digest,
                       operation_cardinality, correlation_id
                  from public.extraordinary_benefit_transformation_audit
                """);
        assertEquals("grant.recommended-amount", transformationAudit.get("proposal_key"));
        assertEquals("grant.amount-transformation", transformationAudit.get("binding_key"));
        assertEquals("request.recommendedAmount", transformationAudit.get("target_path"));
        assertEquals("SINGLE_ITEM", transformationAudit.get("operation_cardinality"));
        assertTrue(transformationAudit.get("proposal_identity_digest").toString().matches("[A-F0-9]{64}"));
        assertTrue(transformationAudit.get("before_digest").toString().matches("[A-F0-9]{64}"));
        assertTrue(transformationAudit.get("after_digest").toString().matches("[A-F0-9]{64}"));
        assertFalse(transformationAudit.toString().contains("2500"));
        assertFalse(transformationAudit.toString().contains("BEN-2026-000184"));
        assertTrue(body(response).path("_links").isObject());

        ResponseEntity<String> replay = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate", command, String.class);
        assertEquals(HttpStatus.OK, replay.getStatusCode(), replay.getBody());
        assertEquals(data.path("resource").path("id"), body(replay).path("data").path("resource").path("id"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_request", Integer.class));
        assertEquals(1, tableCount("extraordinary_benefit_transformation_audit"));

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
        assertTrue(requestedAmount.path("description").asText().contains("Valor solicitado"));
        assertEquals("currency", requestedAmount.path("x-ui").path("controlType").asText());
        assertTrue(schema.path("required").toString().contains("requestReference"));
        assertTrue(schema.path("required").toString().contains("factReference"));
        assertFalse(schema.path("properties").has("workerStatus"));
        assertFalse(schema.path("properties").has("availableBudgetAmount"));

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
        assertEquals(0, tableCount("extraordinary_benefit_transformation_audit"));
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
        assertEquals("quickstart:extraordinary-benefit-authoritative-facts", jdbcTemplate.queryForObject(
                "select revalidation_provider_key from public.extraordinary_benefit_grant_effect where benefit_request_id = ?",
                String.class, id));
        assertEquals("extraordinary-grant-v1", jdbcTemplate.queryForObject(
                "select revalidation_snapshot_key from public.extraordinary_benefit_grant_effect where benefit_request_id = ?",
                String.class, id));
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
    void applyFailsClosedWhenAuthoritativeFactsChangeAfterApproval() throws Exception {
        JsonNode evaluation = body(restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate",
                authorizedJson(eligiblePayload(), "revalidation-evaluate", null), String.class));
        long id = evaluation.path("data").path("resource").path("id").asLong();
        String evaluatedEtag = restTemplate.exchange(
                "/api/human-resources/extraordinary-benefit-requests/" + id,
                HttpMethod.GET, HttpEntity.EMPTY, String.class).getHeaders().getETag();
        String transition = """
                {"justification":"Aprovacao anterior a mudanca autoritativa.","effectiveAt":"2026-07-13"}
                """;
        ResponseEntity<String> submit = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/submit",
                authorizedJson(transition, "revalidation-submit", evaluatedEtag), String.class);
        ResponseEntity<String> approve = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/approve",
                authorizedJson(transition, "revalidation-approve", submit.getHeaders().getETag()), String.class);

        jdbcTemplate.update("""
                update public.rule_lab_authoritative_benefit_facts
                   set duplicate_grant = true,
                       source_record_digest = 'BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB'
                 where fact_reference = 'QL10-FICTIONAL-001'
                """);

        ResponseEntity<String> apply = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/apply",
                authorizedJson(transition, "revalidation-apply", approve.getHeaders().getETag()), String.class);

        assertEquals(HttpStatus.PRECONDITION_FAILED, apply.getStatusCode(), apply.getBody());
        assertEquals("APPROVED", jdbcTemplate.queryForObject(
                "select lifecycle_status from public.extraordinary_benefit_request where id = ?", String.class, id));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_grant_effect where benefit_request_id = ?",
                Integer.class, id));
    }

    @Test
    void applyFailsClosedWhenFactsChangeButTheBusinessPlanWouldRemainEquivalent() throws Exception {
        JsonNode evaluation = body(restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate",
                authorizedJson(eligiblePayload(), "facts-digest-evaluate", null), String.class));
        long id = evaluation.path("data").path("resource").path("id").asLong();
        String evaluatedEtag = restTemplate.exchange(
                "/api/human-resources/extraordinary-benefit-requests/" + id,
                HttpMethod.GET, HttpEntity.EMPTY, String.class).getHeaders().getETag();
        String transition = """
                {"justification":"Aprovacao anterior a mudanca de fatos sem alteracao do plano.","effectiveAt":"2026-07-13"}
                """;
        ResponseEntity<String> submit = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/submit",
                authorizedJson(transition, "facts-digest-submit", evaluatedEtag), String.class);
        ResponseEntity<String> approve = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/approve",
                authorizedJson(transition, "facts-digest-approve", submit.getHeaders().getETag()), String.class);

        jdbcTemplate.update("""
                update public.rule_lab_authoritative_benefit_facts
                   set available_budget_amount = 99999.00,
                       source_record_digest = 'CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC'
                 where fact_reference = 'QL10-FICTIONAL-001'
                """);

        ResponseEntity<String> apply = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/apply",
                authorizedJson(transition, "facts-digest-apply", approve.getHeaders().getETag()), String.class);

        assertEquals(HttpStatus.PRECONDITION_FAILED, apply.getStatusCode(), apply.getBody());
        assertEquals("APPROVED", jdbcTemplate.queryForObject(
                "select lifecycle_status from public.extraordinary_benefit_request where id = ?", String.class, id));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_grant_effect where benefit_request_id = ?",
                Integer.class, id));
    }

    @Test
    void denyDoesNotPersistAndReusedKeyWithAnotherPayloadConflicts() throws Exception {
        String denied = eligiblePayload()
                .replace("BEN-2026-000184", "BEN-2026-DENIED")
                .replace("QL10-FICTIONAL-001", "QL10-FICTIONAL-DENIED");
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
                .replace("QL10-FICTIONAL-001", "QL10-FICTIONAL-DENIED");
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
        assertEquals("FAILED", jdbcTemplate.queryForObject(
                "select execution_status from public.praxis_resource_action_execution where idempotency_key = ?",
                String.class, "rollback-apply"));

        ResponseEntity<String> afterFailure = restTemplate.exchange(
                "/api/human-resources/extraordinary-benefit-requests/" + id,
                HttpMethod.GET, HttpEntity.EMPTY, String.class);
        assertEquals(HttpStatus.OK, afterFailure.getStatusCode(), afterFailure.getBody());
        assertEquals(approve.getHeaders().getETag(), afterFailure.getHeaders().getETag());
        assertEquals("APPROVED", body(afterFailure).path("data").path("lifecycleStatus").asText());
    }

    @Test
    void concurrentApplyCommitsExactlyOneEffect() throws Exception {
        JsonNode evaluation = body(restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/actions/evaluate",
                authorizedJson(eligiblePayload(), "concurrent-evaluate", null), String.class));
        long id = evaluation.path("data").path("resource").path("id").asLong();
        String etag = restTemplate.exchange(
                "/api/human-resources/extraordinary-benefit-requests/" + id,
                HttpMethod.GET, HttpEntity.EMPTY, String.class).getHeaders().getETag();
        String transition = """
                {"justification":"Prova concorrente do piloto QL-08.","effectiveAt":"2026-07-14"}
                """;
        ResponseEntity<String> submit = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/submit",
                authorizedJson(transition, "concurrent-submit", etag), String.class);
        ResponseEntity<String> approve = restTemplate.postForEntity(
                "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/approve",
                authorizedJson(transition, "concurrent-approve", submit.getHeaders().getETag()), String.class);
        assertEquals(HttpStatus.OK, approve.getStatusCode(), approve.getBody());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> restTemplate.postForEntity(
                    "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/apply",
                    authorizedJson(transition, "concurrent-apply-a", approve.getHeaders().getETag()), String.class));
            var second = executor.submit(() -> restTemplate.postForEntity(
                    "/api/human-resources/extraordinary-benefit-requests/" + id + "/actions/apply",
                    authorizedJson(transition, "concurrent-apply-b", approve.getHeaders().getETag()), String.class));

            var statuses = List.of(first.get().getStatusCode(), second.get().getStatusCode());
            assertEquals(1L, statuses.stream().filter(HttpStatus.OK::equals).count());
            assertTrue(statuses.stream().anyMatch(status -> status == HttpStatus.CONFLICT
                    || status == HttpStatus.PRECONDITION_FAILED));
        }

        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_grant_effect where benefit_request_id = ?",
                Integer.class, id));
        assertEquals("APPLIED", jdbcTemplate.queryForObject(
                "select lifecycle_status from public.extraordinary_benefit_request where id = ?", String.class, id));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.praxis_resource_action_transition where resource_id = ? and action_id = 'apply'",
                Integer.class, Long.toString(id)));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.praxis_resource_action_execution where idempotency_key in (?, ?) and execution_status = 'COMPLETED'",
                Integer.class, "concurrent-apply-a", "concurrent-apply-b"));
    }

    @Test
    void statementAtomicCommitsItemsOutboxAndReplayResultExactlyOnce() throws Exception {
        var items = List.of(
                eligibleRequest("BEN-2026-STMT-001"),
                eligibleRequest("BEN-2026-STMT-002"));
        var result = statementCommandService.execute(
                items,
                Set.of("benefit:request"),
                "rule-lab-admin",
                "ql08-statement-success",
                "ql08-statement-success");
        var replay = statementCommandService.execute(
                items,
                Set.of("benefit:request"),
                "rule-lab-admin",
                "ql08-statement-replay",
                "ql08-statement-success");

        assertEquals(RuleLabOperationCardinality.STATEMENT_ATOMIC, result.cardinality());
        assertEquals(RuleLabOperationBarrier.LOCAL_COMMITTED, result.barrier());
        assertEquals(result.operationId(), replay.operationId());
        assertEquals(result.outboxMessageId(), replay.outboxMessageId());
        assertEquals(2, result.itemCount());
        assertEquals(2, result.aggregateVisibleItemCount());
        assertEquals(List.of("BEN-2026-STMT-001", "BEN-2026-STMT-002"), result.items().stream()
                .map(item -> item.facts().requestReference()).toList());
        assertEquals(2, tableCount("extraordinary_benefit_request"));
        assertEquals(2, tableCount("extraordinary_benefit_transformation_audit"));
        assertEquals(2, jdbcTemplate.queryForObject("""
                select count(*) from public.extraordinary_benefit_transformation_audit
                 where operation_id = ? and operation_cardinality = 'STATEMENT_ATOMIC'
                   and correlation_id = 'ql08-statement-success'
                """, Integer.class, result.operationId()));
        assertEquals(0, tableCount("extraordinary_benefit_grant_effect"));
        assertEquals(1, tableCount("extraordinary_benefit_statement_outbox"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.praxis_resource_action_execution where execution_status = 'COMPLETED'",
                Integer.class));
        assertTrue(jdbcTemplate.queryForObject(
                "select resource_id from public.praxis_resource_action_execution",
                String.class).startsWith("scope:"));
        assertEquals("desenv", jdbcTemplate.queryForObject(
                "select tenant_id from public.extraordinary_benefit_statement_outbox",
                String.class));
        String payload = jdbcTemplate.queryForObject(
                "select cast(payload as varchar) from public.extraordinary_benefit_statement_outbox",
                String.class);
        assertFalse(payload.contains("BEN-2026-STMT"));
        assertEquals(0, operationScopeRegistry.activeCount());
    }

    @Test
    void statementAtomicRejectsFingerprintConflictWithoutDuplicatingWrites() throws Exception {
        statementCommandService.execute(
                List.of(eligibleRequest("BEN-2026-STMT-FP-001")),
                Set.of("benefit:request"), "rule-lab-admin", "ql08-fingerprint", "same-key");

        ResponseStatusException conflict = assertThrows(ResponseStatusException.class, () ->
                statementCommandService.execute(
                        List.of(eligibleRequest("BEN-2026-STMT-FP-002")),
                        Set.of("benefit:request"), "rule-lab-admin", "ql08-fingerprint", "same-key"));

        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertEquals(1, tableCount("extraordinary_benefit_request"));
        assertEquals(1, tableCount("extraordinary_benefit_statement_outbox"));
    }

    @Test
    void statementAtomicRollsBackEveryItemAndCleansContextOnFailure() throws Exception {
        doThrow(new IllegalStateException("aggregate barrier rejected the statement"))
                .when(statementBarrier)
                .verify(any(RuleLabOperationContext.class), anyList(), anyList());

        try {
            assertThrows(IllegalStateException.class, () -> statementCommandService.execute(
                    List.of(
                            eligibleRequest("BEN-2026-STMT-ROLLBACK-001"),
                            eligibleRequest("BEN-2026-STMT-ROLLBACK-002")),
                    Set.of("benefit:request"),
                    "rule-lab-admin",
                    "ql08-statement-rollback",
                    "ql08-statement-rollback"));
        } finally {
            reset(statementBarrier);
        }

        assertEquals(0, tableCount("extraordinary_benefit_request"));
        assertEquals(0, tableCount("extraordinary_benefit_grant_effect"));
        assertEquals(0, tableCount("extraordinary_benefit_statement_outbox"));
        assertEquals(0, tableCount("extraordinary_benefit_transformation_audit"));
        assertEquals("FAILED", jdbcTemplate.queryForObject(
                "select execution_status from public.praxis_resource_action_execution where idempotency_key = ?",
                String.class, "ql08-statement-rollback"));
        String failedResponsePayload = jdbcTemplate.queryForObject(
                "select cast(response_payload as varchar) from public.praxis_resource_action_execution where idempotency_key = ?",
                String.class, "ql08-statement-rollback");
        assertTrue(failedResponsePayload == null || "null".equalsIgnoreCase(failedResponsePayload));
        assertEquals(0, operationScopeRegistry.activeCount());
    }

    @Test
    void outboxRetriesSameMessageAndMovesToDeadLetterAfterBoundedFailures() throws Exception {
        double retryBefore = counter("praxis.rule.lab.outbox.dispatches", "RETRY_SCHEDULED");
        double deadLetterBefore = counter("praxis.rule.lab.outbox.dispatches", "DEAD_LETTERED");
        var result = statementCommandService.execute(
                List.of(eligibleRequest("BEN-2026-STMT-DLQ-001")),
                Set.of("benefit:request"), "rule-lab-admin", "ql08-dlq", "ql08-dlq");
        doThrow(new IllegalStateException("broker unavailable"))
                .when(statementEventSink).deliver(any(ExtraordinaryBenefitStatementOutboxDelivery.class));

        var first = outboxDispatcher.dispatchNext();
        jdbcTemplate.update(
                "update public.extraordinary_benefit_statement_outbox set next_attempt_at = ? where message_id = ?",
                Instant.now().minusSeconds(1), result.outboxMessageId());
        var second = outboxDispatcher.dispatchNext();

        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.RETRY_SCHEDULED, first.outcome());
        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.DEAD_LETTERED, second.outcome());
        assertEquals(result.outboxMessageId(), first.messageId());
        assertEquals(result.outboxMessageId(), second.messageId());
        assertEquals("DEAD_LETTER", jdbcTemplate.queryForObject(
                "select delivery_status from public.extraordinary_benefit_statement_outbox where message_id = ?",
                String.class, result.outboxMessageId()));
        assertEquals(2, jdbcTemplate.queryForObject(
                "select delivery_attempts from public.extraordinary_benefit_statement_outbox where message_id = ?",
                Integer.class, result.outboxMessageId()));
        ArgumentCaptor<ExtraordinaryBenefitStatementOutboxDelivery> deliveries =
                ArgumentCaptor.forClass(ExtraordinaryBenefitStatementOutboxDelivery.class);
        verify(statementEventSink, times(2)).deliver(deliveries.capture());
        assertEquals(deliveries.getAllValues().get(0).messageId(), deliveries.getAllValues().get(1).messageId());
        assertEquals(retryBefore + 1, counter("praxis.rule.lab.outbox.dispatches", "RETRY_SCHEDULED"));
        assertEquals(deadLetterBefore + 1, counter("praxis.rule.lab.outbox.dispatches", "DEAD_LETTERED"));
    }

    @Test
    void outboxReclaimsExpiredLeaseAndDoesNotRedeliverAfterAcknowledgement() throws Exception {
        var result = statementCommandService.execute(
                List.of(eligibleRequest("BEN-2026-STMT-LEASE-001")),
                Set.of("benefit:request"), "rule-lab-admin", "ql08-lease", "ql08-lease");
        jdbcTemplate.update("""
                        update public.extraordinary_benefit_statement_outbox
                        set delivery_status = 'PROCESSING', delivery_attempts = 1,
                            lease_token = ?, lease_until = ?
                        where message_id = ?
                        """,
                UUID.randomUUID(), Instant.now().minusSeconds(1), result.outboxMessageId());

        var delivered = outboxDispatcher.dispatchNext();
        var empty = outboxDispatcher.dispatchNext();

        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.DELIVERED, delivered.outcome());
        assertEquals(2, delivered.attempt());
        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.EMPTY, empty.outcome());
        assertEquals("DELIVERED", jdbcTemplate.queryForObject(
                "select delivery_status from public.extraordinary_benefit_statement_outbox where message_id = ?",
                String.class, result.outboxMessageId()));
        verify(statementEventSink, times(1)).deliver(any(ExtraordinaryBenefitStatementOutboxDelivery.class));
    }

    @Test
    void reconcilesAmbiguousHttpDeliveryFromIndependentIdempotentInboxWithoutRedelivery() throws Exception {
        double reconciliationBefore = counter("praxis.rule.lab.outbox.reconciliations", "RECONCILED");
        var result = statementCommandService.execute(
                List.of(eligibleRequest("BEN-2026-STMT-AMBIGUOUS-001")),
                Set.of("benefit:request"), "rule-lab-admin", "ql08-ambiguous", "ql08-ambiguous");
        failAfterInboxCommitOnce.set(true);

        var ambiguous = outboxDispatcher.dispatchNext();
        var reconciled = outboxReconciler.reconcileNext();
        var empty = outboxDispatcher.dispatchNext();

        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.RETRY_SCHEDULED, ambiguous.outcome());
        assertEquals(ExtraordinaryBenefitStatementReconciliationOutcome.RECONCILED, reconciled.outcome());
        assertEquals(result.outboxMessageId(), reconciled.messageId());
        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.EMPTY, empty.outcome());
        assertEquals(1, externalInbox.queryForObject(
                "select count(*) from statement_inbox where message_id = ?",
                Integer.class, result.outboxMessageId()));
        assertEquals("DELIVERED", jdbcTemplate.queryForObject(
                "select delivery_status from public.extraordinary_benefit_statement_outbox where message_id = ?",
                String.class, result.outboxMessageId()));
        verify(statementEventSink, times(1)).deliver(any(ExtraordinaryBenefitStatementOutboxDelivery.class));
        assertEquals(reconciliationBefore + 1,
                counter("praxis.rule.lab.outbox.reconciliations", "RECONCILED"));
    }

    @Test
    void reportsPayloadFreeBacklogAndPurgesOnlyExpiredDeliveredRows() {
        Instant now = Instant.now();
        UUID expiredDelivered = insertOutboxMessage(
                "DELIVERED", now.minusSeconds(40L * 24 * 60 * 60), now.minusSeconds(31L * 24 * 60 * 60));
        UUID retainedDelivered = insertOutboxMessage(
                "DELIVERED", now.minusSeconds(2L * 24 * 60 * 60), now.minusSeconds(24L * 60 * 60));
        UUID retainedDeadLetter = insertOutboxMessage(
                "DEAD_LETTER", now.minusSeconds(45L * 24 * 60 * 60), null);
        double deletedBefore = metricCounter("praxis.rule.lab.outbox.retention.deleted");

        var before = outboxOperations.snapshot();
        var retention = outboxOperations.purgeDelivered();
        var after = outboxOperations.snapshot();

        assertEquals(2, before.delivered());
        assertEquals(1, before.deadLetter());
        assertEquals(1, retention.deletedDeliveredRows());
        assertFalse(retention.batchLimitReached());
        assertEquals(1, after.delivered());
        assertEquals(1, after.deadLetter());
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_statement_outbox where message_id = ?",
                Integer.class, expiredDelivered));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_statement_outbox where message_id = ?",
                Integer.class, retainedDelivered));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_statement_outbox where message_id = ?",
                Integer.class, retainedDeadLetter));
        assertEquals(deletedBefore + 1,
                metricCounter("praxis.rule.lab.outbox.retention.deleted"));
    }

    @Test
    void httpOutboxAdapterRejectsPlainHttpUnlessLaboratoryOverrideIsExplicit() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () ->
                new HttpExtraordinaryBenefitStatementEventSink(
                        objectMapper, "http://consumer.internal", "", 1000, 1000, false));

        assertTrue(failure.getMessage().contains("HTTPS"));
    }

    @Test
    void permanentHttpAuthorizationFailureMovesDirectlyToDeadLetter() throws Exception {
        var result = statementCommandService.execute(
                List.of(eligibleRequest("BEN-2026-STMT-AUTH-001")),
                Set.of("benefit:request"), "rule-lab-admin", "ql08-auth", "ql08-auth");
        var unauthorizedSink = new HttpExtraordinaryBenefitStatementEventSink(
                objectMapper,
                "http://127.0.0.1:" + externalConsumer.getAddress().getPort(),
                "revoked-token",
                1000,
                2000,
                true);
        doAnswer(invocation -> {
            unauthorizedSink.deliver(invocation.getArgument(0));
            return null;
        }).when(statementEventSink).deliver(any(ExtraordinaryBenefitStatementOutboxDelivery.class));

        var dispatch = outboxDispatcher.dispatchNext();

        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.DEAD_LETTERED, dispatch.outcome());
        assertEquals(1, dispatch.attempt());
        assertEquals("HTTP_AUTHORIZATION_REJECTED", failureCode(result.outboxMessageId()));
    }

    @Test
    void permanentHttpContractFailureDoesNotConsumeRetryBudget() throws Exception {
        var result = statementCommandService.execute(
                List.of(eligibleRequest("BEN-2026-STMT-CONTRACT-001")),
                Set.of("benefit:request"), "rule-lab-admin", "ql08-contract", "ql08-contract");
        forcedDeliveryStatus.set(400);

        var dispatch = outboxDispatcher.dispatchNext();

        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.DEAD_LETTERED, dispatch.outcome());
        assertEquals(1, dispatch.attempt());
        assertEquals("HTTP_CONTRACT_REJECTED", failureCode(result.outboxMessageId()));
    }

    @Test
    void transientHttpThrottlingRemainsEligibleForBoundedRetry() throws Exception {
        var result = statementCommandService.execute(
                List.of(eligibleRequest("BEN-2026-STMT-THROTTLED-001")),
                Set.of("benefit:request"), "rule-lab-admin", "ql08-throttled", "ql08-throttled");
        forcedDeliveryStatus.set(429);

        var dispatch = outboxDispatcher.dispatchNext();

        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.RETRY_SCHEDULED, dispatch.outcome());
        assertEquals(1, dispatch.attempt());
        assertEquals("HTTP_THROTTLED", failureCode(result.outboxMessageId()));
        assertEquals("PENDING", jdbcTemplate.queryForObject(
                "select delivery_status from public.extraordinary_benefit_statement_outbox where message_id = ?",
                String.class, result.outboxMessageId()));
    }

    @Test
    void invalidAcknowledgementMovesToDeadLetterWithoutLosingIndependentReconciliation() throws Exception {
        var result = statementCommandService.execute(
                List.of(eligibleRequest("BEN-2026-STMT-ACK-001")),
                Set.of("benefit:request"), "rule-lab-admin", "ql08-ack", "ql08-ack");
        returnInvalidAcknowledgementOnce.set(true);

        var dispatch = outboxDispatcher.dispatchNext();

        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.DEAD_LETTERED, dispatch.outcome());
        assertEquals(1, dispatch.attempt());
        assertEquals("HTTP_ACK_CONTRACT_INVALID", failureCode(result.outboxMessageId()));

        var reconciliation = outboxReconciler.reconcileNext();

        assertEquals(ExtraordinaryBenefitStatementReconciliationOutcome.RECONCILED,
                reconciliation.outcome());
        assertEquals(result.outboxMessageId(), reconciliation.messageId());
        assertEquals("DELIVERED", jdbcTemplate.queryForObject(
                "select delivery_status from public.extraordinary_benefit_statement_outbox where message_id = ?",
                String.class, result.outboxMessageId()));
    }

    @Test
    void governedReplayResetsRetryCycleAndAppendsPayloadFreeAudit() throws Exception {
        var result = deadLetterByAuthorizationFailure("BEN-2026-STMT-REPLAY-001", "ql08-replay");
        ageLastFailure(result.outboxMessageId());
        double replayBefore = counter("praxis.rule.lab.outbox.replays", "REPLAY_SCHEDULED");

        var replay = replayService.requestReplay(replayCommand(
                result.outboxMessageId(), "HTTP_AUTHORIZATION_REJECTED", "Correção da credencial concluída."));

        assertEquals(ExtraordinaryBenefitStatementReplayOutcome.REPLAY_SCHEDULED, replay.outcome());
        assertEquals("PENDING", outboxStatus(result.outboxMessageId()));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select delivery_attempts from public.extraordinary_benefit_statement_outbox where message_id = ?",
                Integer.class, result.outboxMessageId()));
        assertEquals(1, replayAuditCount(result.outboxMessageId(), "REPLAY_SCHEDULED"));
        assertEquals(replayBefore + 1,
                counter("praxis.rule.lab.outbox.replays", "REPLAY_SCHEDULED"));
        assertEquals("rule-lab-operator", jdbcTemplate.queryForObject(
                "select actor_subject from public.extraordinary_benefit_statement_replay_audit where audit_id = ?",
                String.class, replay.auditId()));
        assertEquals(0, jdbcTemplate.queryForObject("""
                        select count(*) from information_schema.columns
                        where table_schema = 'public'
                          and table_name = 'extraordinary_benefit_statement_replay_audit'
                          and column_name in ('payload', 'response_body', 'bearer_token')
                        """, Integer.class));

        doAnswer(invocation -> {
            httpEventSink.deliver(invocation.getArgument(0));
            return null;
        }).when(statementEventSink).deliver(any(ExtraordinaryBenefitStatementOutboxDelivery.class));
        var delivered = outboxDispatcher.dispatchNext();
        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.DELIVERED, delivered.outcome());
    }

    @Test
    void governedReplayRejectsMessageStillInsideQuarantine() throws Exception {
        var result = deadLetterByAuthorizationFailure("BEN-2026-STMT-QUARANTINE-001", "ql08-quarantine");

        var replay = replayService.requestReplay(replayCommand(
                result.outboxMessageId(), "HTTP_AUTHORIZATION_REJECTED", "Credencial corrigida, aguardando quarentena."));

        assertEquals(ExtraordinaryBenefitStatementReplayOutcome.REJECTED_QUARANTINE, replay.outcome());
        assertEquals("DEAD_LETTER", outboxStatus(result.outboxMessageId()));
        assertEquals(1, replayAuditCount(result.outboxMessageId(), "REJECTED_QUARANTINE"));
    }

    @Test
    void governedReplayRejectsStaleFailureCodeBeforeProbingConsumer() throws Exception {
        var result = deadLetterByAuthorizationFailure("BEN-2026-STMT-STALE-001", "ql08-stale");
        ageLastFailure(result.outboxMessageId());

        var replay = replayService.requestReplay(replayCommand(
                result.outboxMessageId(), "HTTP_CONTRACT_REJECTED", "Operador observou uma falha já substituída."));

        assertEquals(ExtraordinaryBenefitStatementReplayOutcome.REJECTED_FAILURE_CHANGED, replay.outcome());
        assertEquals("DEAD_LETTER", outboxStatus(result.outboxMessageId()));
        assertEquals(1, replayAuditCount(result.outboxMessageId(), "REJECTED_FAILURE_CHANGED"));
    }

    @Test
    void governedReplayRejectsWhenDeploymentHasNoAcknowledgementProbe() throws Exception {
        var result = deadLetterByAuthorizationFailure("BEN-2026-STMT-NO-PROBE-001", "ql08-no-probe");
        ageLastFailure(result.outboxMessageId());
        var emptyFactory = new StaticListableBeanFactory();
        var serviceWithoutProbe = new ExtraordinaryBenefitStatementReplayService(
                replayStore,
                emptyFactory.getBeanProvider(ExtraordinaryBenefitStatementDeliveryProbe.class),
                outboxTelemetry,
                ruleClock,
                1000);

        var replay = serviceWithoutProbe.requestReplay(replayCommand(
                result.outboxMessageId(), "HTTP_AUTHORIZATION_REJECTED", "Probe obrigatório não está configurado."));

        assertEquals(ExtraordinaryBenefitStatementReplayOutcome.REJECTED_NO_PROBE, replay.outcome());
        assertEquals("DEAD_LETTER", outboxStatus(result.outboxMessageId()));
        assertEquals(1, replayAuditCount(result.outboxMessageId(), "REJECTED_NO_PROBE"));
    }

    @Test
    void governedReplayConvertsExistingExternalAcknowledgementWithoutRedelivery() throws Exception {
        var result = statementCommandService.execute(
                List.of(eligibleRequest("BEN-2026-STMT-REPLAY-ACK-001")),
                Set.of("benefit:request"), "rule-lab-admin", "ql08-replay-ack", "ql08-replay-ack");
        returnInvalidAcknowledgementOnce.set(true);
        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.DEAD_LETTERED,
                outboxDispatcher.dispatchNext().outcome());
        ageLastFailure(result.outboxMessageId());

        var replay = replayService.requestReplay(replayCommand(
                result.outboxMessageId(), "HTTP_ACK_CONTRACT_INVALID", "Reconciliação obrigatória antes do replay."));

        assertEquals(ExtraordinaryBenefitStatementReplayOutcome.ACKNOWLEDGED_NO_REPLAY, replay.outcome());
        assertEquals("DELIVERED", outboxStatus(result.outboxMessageId()));
        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.EMPTY, outboxDispatcher.dispatchNext().outcome());
        assertEquals(1, replayAuditCount(result.outboxMessageId(), "ACKNOWLEDGED_NO_REPLAY"));
    }

    private void startExternalConsumer() {
        try {
            var dataSource = new DriverManagerDataSource(
                    "jdbc:h2:mem:ql08_external_consumer;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                    "sa", "");
            dataSource.setDriverClassName("org.h2.Driver");
            externalInbox = new JdbcTemplate(dataSource);
            externalInbox.execute("""
                    create table if not exists statement_inbox (
                        message_id uuid primary key,
                        operation_id uuid not null,
                        payload_hash varchar(64) not null,
                        acknowledged_at_utc varchar(40) not null)
                    """);
            externalInbox.update("delete from statement_inbox");
            failAfterInboxCommitOnce.set(false);
            returnInvalidAcknowledgementOnce.set(false);
            forcedDeliveryStatus.set(0);
            externalConsumer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            externalConsumer.createContext("/inbox/extraordinary-benefit-statements", this::handleExternalInbox);
            externalConsumerExecutor = Executors.newCachedThreadPool();
            externalConsumer.setExecutor(externalConsumerExecutor);
            externalConsumer.start();
            httpEventSink = new HttpExtraordinaryBenefitStatementEventSink(
                    objectMapper,
                    "http://127.0.0.1:" + externalConsumer.getAddress().getPort(),
                    "ql08-laboratory-token",
                    1000,
                    2000,
                    true);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not start QL08 external consumer", failure);
        }
    }

    private void handleExternalInbox(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"Bearer ql08-laboratory-token".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                sendExternalResponse(exchange, 401, Map.of("error", "unauthorized"));
                return;
            }
            int forcedStatus = forcedDeliveryStatus.get();
            if ("POST".equals(exchange.getRequestMethod()) && forcedStatus != 0) {
                sendExternalResponse(exchange, forcedStatus, Map.of("error", "forced-test-response"));
                return;
            }
            String suffix = exchange.getRequestURI().getPath()
                    .substring("/inbox/extraordinary-benefit-statements".length());
            if ("POST".equals(exchange.getRequestMethod()) && suffix.isEmpty()) {
                JsonNode delivery = objectMapper.readTree(exchange.getRequestBody());
                UUID messageId = UUID.fromString(delivery.path("messageId").asText());
                if (!messageId.toString().equals(exchange.getRequestHeaders().getFirst("Idempotency-Key"))) {
                    sendExternalResponse(exchange, 400, Map.of("error", "invalid-idempotency-key"));
                    return;
                }
                boolean duplicate = externalInbox.queryForObject(
                        "select count(*) from statement_inbox where message_id = ?", Integer.class, messageId) > 0;
                String payloadHash = sha256(delivery.path("payload").toString());
                if (duplicate && !payloadHash.equals(externalInbox.queryForObject(
                        "select payload_hash from statement_inbox where message_id = ?",
                        String.class, messageId))) {
                    sendExternalResponse(exchange, 409, Map.of("error", "idempotency-fingerprint-conflict"));
                    return;
                }
                String acknowledgedAt = duplicate
                        ? externalInbox.queryForObject(
                                "select acknowledged_at_utc from statement_inbox where message_id = ?",
                                String.class, messageId)
                        : Instant.now().toString();
                if (!duplicate) {
                    externalInbox.update(
                            "insert into statement_inbox(message_id, operation_id, payload_hash, acknowledged_at_utc) values (?, ?, ?, ?)",
                            messageId, UUID.fromString(delivery.path("operationId").asText()), payloadHash, acknowledgedAt);
                }
                if (failAfterInboxCommitOnce.compareAndSet(true, false)) {
                    sendExternalResponse(exchange, 503, Map.of("error", "simulated-response-loss"));
                    return;
                }
                if (returnInvalidAcknowledgementOnce.compareAndSet(true, false)) {
                    sendExternalResponse(exchange, 200, Map.of("status", "PROCESSED"));
                    return;
                }
                sendExternalResponse(exchange, 200, acknowledgement(messageId, acknowledgedAt,
                        duplicate ? "DUPLICATE" : "PROCESSED"));
                return;
            }
            if ("GET".equals(exchange.getRequestMethod()) && suffix.startsWith("/")) {
                UUID messageId = UUID.fromString(suffix.substring(1));
                List<String> acknowledgedAt = externalInbox.query(
                        "select acknowledged_at_utc from statement_inbox where message_id = ?",
                        (resultSet, row) -> resultSet.getString(1), messageId);
                if (acknowledgedAt.isEmpty()) {
                    sendExternalResponse(exchange, 404, Map.of("error", "not-found"));
                } else {
                    sendExternalResponse(exchange, 200,
                            acknowledgement(messageId, acknowledgedAt.get(0), "PROCESSED"));
                }
                return;
            }
            sendExternalResponse(exchange, 404, Map.of("error", "not-found"));
        } catch (RuntimeException failure) {
            sendExternalResponse(exchange, 500, Map.of("error", "invalid-request"));
        }
    }

    private Map<String, String> acknowledgement(UUID messageId, String acknowledgedAt, String status) {
        return Map.of(
                "messageId", messageId.toString(),
                "acknowledgedAtUtc", acknowledgedAt,
                "status", status);
    }

    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 must be available", impossible);
        }
    }

    private void sendExternalResponse(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] response = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
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
                    facts_digest varchar(64) not null, fact_reference varchar(120),
                    fact_provider_key varchar(160), fact_source_record_digest varchar(64),
                    fact_source_version bigint, fact_source_recorded_at timestamp with time zone,
                    fact_scope_digest varchar(64), fact_as_of timestamp with time zone,
                    plan_digest varchar(64) not null,
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
                    revalidation_snapshot_key varchar(200), revalidation_snapshot_content_hash varchar(64),
                    revalidation_facts_digest varchar(64), revalidation_provider_key varchar(160),
                    revalidation_source_record_digest varchar(64), revalidation_source_version bigint,
                    revalidation_source_recorded_at timestamp with time zone,
                    revalidated_at timestamp with time zone, revalidation_scope_digest varchar(64),
                    foreign key (benefit_request_id) references public.extraordinary_benefit_request(id))
                """);
        jdbcTemplate.execute("""
                create table if not exists public.rule_lab_authoritative_benefit_facts (
                    tenant_id varchar(120) not null, environment varchar(80) not null,
                    organization_key varchar(120) not null, fact_reference varchar(120) not null,
                    source_system varchar(120) not null, source_record_digest varchar(64) not null,
                    source_version bigint not null, effective_from timestamp with time zone not null,
                    effective_to timestamp with time zone, worker_status varchar(20) not null,
                    duplicate_grant boolean not null, program_active boolean not null,
                    program_maximum_amount numeric(15,2) not null, customer_additional_eligible boolean,
                    available_budget_amount numeric(15,2) not null, recorded_at timestamp with time zone not null,
                    primary key (tenant_id, environment, organization_key, fact_reference, source_version))
                """);
        jdbcTemplate.execute("""
                create table if not exists public.rule_lab_authoritative_benefit_payment_date (
                    tenant_id varchar(120) not null, environment varchar(80) not null,
                    organization_key varchar(120) not null, fact_reference varchar(120) not null,
                    source_version bigint not null, allowed_payment_date date not null,
                    primary key (tenant_id, environment, organization_key, fact_reference, source_version, allowed_payment_date))
                """);
        jdbcTemplate.update("delete from public.rule_lab_authoritative_benefit_payment_date");
        jdbcTemplate.update("delete from public.rule_lab_authoritative_benefit_facts");
        jdbcTemplate.update("""
                insert into public.rule_lab_authoritative_benefit_facts(
                    tenant_id, environment, organization_key, fact_reference, source_system,
                    source_record_digest, source_version, effective_from, worker_status, duplicate_grant,
                    program_active, program_maximum_amount, customer_additional_eligible,
                    available_budget_amount, recorded_at)
                values ('desenv', 'local', 'DEMO-ORG', 'QL10-FICTIONAL-001',
                    'quickstart-fictional-hr-read-model',
                    'F8A520B6B03A57DE417F702EDE253622B794ADF72B31C814343887A3C629A995', 1,
                    timestamp with time zone '2026-01-01 00:00:00+00', 'ACTIVE', false, true,
                    5000.00, true, 25000.00, timestamp with time zone '2026-07-16 00:00:00+00')
                """);
        jdbcTemplate.update("""
                insert into public.rule_lab_authoritative_benefit_facts(
                    tenant_id, environment, organization_key, fact_reference, source_system,
                    source_record_digest, source_version, effective_from, worker_status, duplicate_grant,
                    program_active, program_maximum_amount, customer_additional_eligible,
                    available_budget_amount, recorded_at)
                values ('desenv', 'local', 'DEMO-ORG', 'QL10-FICTIONAL-DENIED',
                    'quickstart-fictional-hr-read-model',
                    'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 1,
                    timestamp with time zone '2026-01-01 00:00:00+00', 'ACTIVE', true, true,
                    5000.00, true, 25000.00, timestamp with time zone '2026-07-16 00:00:00+00')
                """);
        jdbcTemplate.update("""
                insert into public.rule_lab_authoritative_benefit_payment_date(
                    tenant_id, environment, organization_key, fact_reference, source_version, allowed_payment_date)
                values ('desenv', 'local', 'DEMO-ORG', 'QL10-FICTIONAL-001', 1, date '2026-07-20')
                """);
        jdbcTemplate.update("""
                insert into public.rule_lab_authoritative_benefit_payment_date(
                    tenant_id, environment, organization_key, fact_reference, source_version, allowed_payment_date)
                values ('desenv', 'local', 'DEMO-ORG', 'QL10-FICTIONAL-DENIED', 1, date '2026-07-20')
                """);
        jdbcTemplate.execute("""
                create table if not exists public.extraordinary_benefit_transformation_audit (
                    audit_id uuid primary key, benefit_request_id bigint not null,
                    operation_id uuid, operation_cardinality varchar(32) not null,
                    proposal_key varchar(200) not null, binding_key varchar(200) not null,
                    slot_key varchar(200) not null, target_path varchar(300) not null,
                    schema_ref varchar(500) not null, transformation_operation varchar(32) not null,
                    reason_code varchar(120) not null, proposal_identity_digest varchar(64) not null,
                    before_digest varchar(64) not null, after_digest varchar(64) not null,
                    snapshot_key varchar(200) not null, snapshot_content_hash varchar(64) not null,
                    snapshot_activation_revision bigint not null, rule_set_key varchar(200) not null,
                    rule_set_version integer not null, facts_digest varchar(64) not null,
                    plan_digest varchar(64) not null, correlation_id varchar(255) not null,
                    recorded_at timestamp with time zone not null,
                    unique(benefit_request_id, proposal_identity_digest),
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
                create table if not exists public.extraordinary_benefit_statement_outbox (
                    message_id uuid primary key, operation_id uuid not null unique,
                    event_type varchar(160) not null, tenant_id varchar(120) not null,
                    environment varchar(80) not null, correlation_id varchar(255) not null,
                    payload json not null, delivery_status varchar(32) not null,
                    delivery_attempts integer not null, next_attempt_at timestamp with time zone not null,
                    next_reconciliation_at timestamp with time zone not null,
                    lease_token uuid, lease_until timestamp with time zone,
                    created_at timestamp with time zone not null, delivered_at timestamp with time zone,
                    last_failure_code varchar(120), last_failure_message varchar(1000),
                    last_failure_at timestamp with time zone)
                """);
        jdbcTemplate.execute("""
                create table if not exists public.extraordinary_benefit_statement_replay_audit (
                    audit_id uuid primary key, message_id uuid not null,
                    requested_at timestamp with time zone not null,
                    actor_subject varchar(255) not null, justification varchar(1000) not null,
                    correlation_id varchar(255) not null, expected_failure_code varchar(120),
                    observed_failure_code varchar(120), replay_outcome varchar(80) not null,
                    acknowledged_at timestamp with time zone)
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
                  "factReference": "QL10-FICTIONAL-001",
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

    private UUID insertOutboxMessage(String status, Instant createdAt, Instant deliveredAt) {
        UUID messageId = UUID.randomUUID();
        jdbcTemplate.update("""
                        insert into public.extraordinary_benefit_statement_outbox(
                            message_id, operation_id, event_type, tenant_id, environment, correlation_id,
                            payload, delivery_status, delivery_attempts, next_attempt_at,
                            next_reconciliation_at, created_at, delivered_at,
                            last_failure_code, last_failure_message)
                        values (?, ?, 'extraordinary-benefit.statement-requested.v1', 'desenv', 'local', ?,
                            ?, ?, 1, ?, ?, ?, ?, ?, ?)
                        """,
                messageId, UUID.randomUUID(), "retention-" + messageId, "{}", status,
                createdAt, createdAt, createdAt, deliveredAt,
                "DEAD_LETTER".equals(status) ? "EVENT_SINK_FAILURE" : null,
                "DEAD_LETTER".equals(status) ? "ExternalDeliveryException" : null);
        return messageId;
    }

    private double counter(String name, String outcome) {
        var counter = meterRegistry.find(name).tag("outcome", outcome).counter();
        return counter == null ? 0 : counter.count();
    }

    private double metricCounter(String name) {
        var counter = meterRegistry.find(name).counter();
        return counter == null ? 0 : counter.count();
    }

    private String failureCode(UUID messageId) {
        return jdbcTemplate.queryForObject(
                "select last_failure_code from public.extraordinary_benefit_statement_outbox where message_id = ?",
                String.class,
                messageId);
    }

    private ExtraordinaryBenefitStatementOperationResult deadLetterByAuthorizationFailure(
            String requestReference,
            String idempotencyKey) throws Exception {
        var result = statementCommandService.execute(
                List.of(eligibleRequest(requestReference)),
                Set.of("benefit:request"), "rule-lab-admin", idempotencyKey, idempotencyKey);
        var unauthorizedSink = new HttpExtraordinaryBenefitStatementEventSink(
                objectMapper,
                "http://127.0.0.1:" + externalConsumer.getAddress().getPort(),
                "revoked-token",
                1000,
                2000,
                true);
        doAnswer(invocation -> {
            unauthorizedSink.deliver(invocation.getArgument(0));
            return null;
        }).when(statementEventSink).deliver(any(ExtraordinaryBenefitStatementOutboxDelivery.class));
        assertEquals(ExtraordinaryBenefitStatementDispatchOutcome.DEAD_LETTERED,
                outboxDispatcher.dispatchNext().outcome());
        return result;
    }

    private ExtraordinaryBenefitStatementReplayCommand replayCommand(
            UUID messageId,
            String failureCode,
            String justification) {
        return new ExtraordinaryBenefitStatementReplayCommand(
                messageId, failureCode, "rule-lab-operator", justification, "ql08-replay-correlation");
    }

    private void ageLastFailure(UUID messageId) {
        jdbcTemplate.update(
                "update public.extraordinary_benefit_statement_outbox set last_failure_at = ? where message_id = ?",
                Instant.now().minusSeconds(5), messageId);
    }

    private String outboxStatus(UUID messageId) {
        return jdbcTemplate.queryForObject(
                "select delivery_status from public.extraordinary_benefit_statement_outbox where message_id = ?",
                String.class, messageId);
    }

    private int replayAuditCount(UUID messageId, String outcome) {
        return jdbcTemplate.queryForObject(
                "select count(*) from public.extraordinary_benefit_statement_replay_audit where message_id = ? and replay_outcome = ?",
                Integer.class, messageId, outcome);
    }

    private ExtraordinaryBenefitEvaluationRequest eligibleRequest(String requestReference) throws Exception {
        return objectMapper.readValue(
                eligiblePayload().replace("BEN-2026-000184", requestReference),
                ExtraordinaryBenefitEvaluationRequest.class);
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
