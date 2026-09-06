package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.ai.authoring.AgenticAuthoringPlanRequest;
import org.praxisplatform.config.ai.authoring.AgenticAuthoringSemanticDecision;
import org.praxisplatform.config.ai.authoring.AgenticAuthoringUiCompositionPlanProvider;
import org.praxisplatform.config.ai.authoring.AgenticAuthoringUiCompositionPlanResult;
import org.praxisplatform.config.domain.AiThread;
import org.praxisplatform.config.domain.AiThreadStatus;
import org.praxisplatform.config.domain.AiTurn;
import org.praxisplatform.config.domain.AiTurnStatus;
import org.praxisplatform.config.dto.AiTurnEventEnvelope;
import org.praxisplatform.config.repository.AiRegistryRepository;
import org.praxisplatform.config.repository.AiThreadRepository;
import org.praxisplatform.config.repository.AiTurnRepository;
import org.praxisplatform.config.service.AiPrincipalContext;
import org.praxisplatform.config.service.AiProviderManagementService;
import org.praxisplatform.config.service.AiTurnEventService;
import org.praxisplatform.config.service.ApiMetadataIndexingCoordinator;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * HTTP/PostgreSQL proof that a hosted page keeps its canonical UiCompositionPlan across reopen,
 * semantic refinement and optimistic-concurrency enforcement.
 */
@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.banner-mode=off",
                "app.rate-limit.enabled=false",
                "app.cors.allowed-origins=http://localhost:4003",
                "app.security.config-origin-restriction.enabled=true",
                "app.security.config-origin-restriction.allowed-origins=http://localhost:4003",
                "app.security.read-open=true",
                "app.security.write-disabled=false",
                "app.security.csrf.disable=true",
                "praxis.ai.provider=mock",
                "praxis.ai.authoring.http-enabled=true",
                "praxis.ai.security.corporate-mode=false",
                "praxis.ai.security.allow-header-identity-in-local=true",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "praxis.openapi.prewarm.enabled=false",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:ui_composition_reopen_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none",
                "config.jpa.hibernate.ddl-auto=none"
        })
@Import(UiCompositionReopenPostgresHttpIntegrationTest.ProviderConfiguration.class)
class UiCompositionReopenPostgresHttpIntegrationTest {

    private static final String TENANT = "ui-composition-http-proof";
    private static final String USER = "quickstart-author";
    private static final String ENVIRONMENT = "local";
    private static final String COMPONENT_TYPE = "praxis-dynamic-page";
    private static final String COMPONENT_ID = "missions-master-detail";
    private static final EmbeddedPostgres CONFIG = startConfigPostgres();

    @DynamicPropertySource
    static void configDatabase(DynamicPropertyRegistry properties) {
        properties.add("config.datasource.url", () -> CONFIG.getJdbcUrl("postgres", "postgres"));
        properties.add("config.datasource.username", () -> "postgres");
        properties.add("config.datasource.password", () -> "");
        properties.add("config.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AiThreadRepository threadRepository;
    @Autowired private AiTurnRepository turnRepository;
    @Autowired private AiTurnEventService turnEventService;
    @Autowired @Qualifier("configJdbcTemplate") private JdbcTemplate configJdbcTemplate;

    @MockBean(name = "ragVectorStore") private VectorStore ragVectorStore;
    @MockBean private AiRegistryRepository aiRegistryRepository;
    @MockBean private AiProviderManagementService providerManagementService;
    @MockBean private ApiMetadataIndexingCoordinator apiMetadataIndexingCoordinator;

    @BeforeEach
    void clearAuthoringState() throws Exception {
        configJdbcTemplate.update("delete from ai_turn_event");
        configJdbcTemplate.update("delete from ai_turn");
        configJdbcTemplate.update("delete from ai_thread");
        configJdbcTemplate.update("delete from ui_user_config");
        when(aiRegistryRepository.findAuthoringManifestPayload(
                        "component_definition",
                        "praxis-chart",
                        "component-definition",
                        "SYSTEM",
                        "GLOBAL"))
                .thenReturn(Optional.of(objectMapper.writeValueAsString(releasedChartManifest())));
        when(providerManagementService.generateJson(
                        anyString(), any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(chartTypeSelection(), chartTypePlan());
    }

    @Test
    void preservesCompositionAcrossHostedReopenAndRejectsStaleOrForeignRefinement() throws Exception {
        JsonNode createPreview = postPreview(createPreviewRequest(), headers(USER));
        assertThat(createPreview.path("valid").asBoolean()).isTrue();
        assertThat(createPreview.at("/uiCompositionPlan/kind").asText())
                .isEqualTo("praxis.ui-composition-plan");

        ObjectNode createDecision = semanticDecision(
                "decision-create-missions-master-detail", "create", "page", "create_artifact");
        AiTurnEventEnvelope createTerminal = persistTerminalResult(
                createPreview, createDecision, "create", null);
        ResponseEntity<String> created = apply(
                createPreview.path("compiledFormPatch"),
                createDecision,
                createTerminal,
                null,
                headers(USER));

        assertThat(created.getStatusCode())
                .as("create apply response: %s", created.getBody())
                .isEqualTo(HttpStatus.OK);
        JsonNode createdBody = objectMapper.readTree(created.getBody());
        assertThat(createdBody.path("version").asLong()).isEqualTo(1L);
        assertThat(createdBody.path("authoringSource").path("sourceSha256").asText())
                .matches("[0-9a-f]{64}");
        String firstEtag = createdBody.path("etag").asText();

        ResponseEntity<String> reopened = readConfig(headers(USER));
        assertThat(reopened.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reopened.getHeaders().getETag()).isEqualTo('"' + firstEtag + '"');
        JsonNode reopenedBody = objectMapper.readTree(reopened.getBody());
        JsonNode reopenedPage = reopenedBody.path("payload");
        JsonNode reopenedSource = reopenedBody.path("authoringSource");
        assertThat(reopenedSource.path("source").path("diagnostics").isMissingNode()).isTrue();
        assertThat(reopenedSource.at("/source/canvas/items/missions-detail/colSpan").asInt())
                .isEqualTo(5);
        assertThat(reopenedPage.at("/composition/links/0/from/ref/port").asText())
                .isEqualTo("selectionChange");

        ObjectNode forgedBrowserSource = reopenedSource.deepCopy();
        forgedBrowserSource.withObject("/source/canvas/items/missions-detail").put("colSpan", 12);
        assertThat(forgedBrowserSource.at("/source/canvas/items/missions-detail/colSpan").asInt())
                .isEqualTo(12);
        ObjectNode refinementRequest = refinementPreviewRequest(reopenedPage, forgedBrowserSource, firstEtag);
        JsonNode refinedPreview = postPreview(refinementRequest, headers(USER));
        assertThat(refinedPreview.path("valid").asBoolean())
                .as("refinement preview: %s", refinedPreview)
                .isTrue();
        assertThat(refinedPreview.at("/uiCompositionPlan/widgets/2/inputs/chartDocument/kind").asText())
                .isEqualTo("line");
        assertThat(refinedPreview.at("/uiCompositionPlan/state"))
                .isEqualTo(reopenedSource.at("/source/state"));
        assertThat(refinedPreview.at("/uiCompositionPlan/canvas"))
                .isEqualTo(reopenedSource.at("/source/canvas"));
        assertThat(refinedPreview.at("/uiCompositionPlan/canvas/items/missions-detail/colSpan").asInt())
                .isEqualTo(5);
        assertThat(refinedPreview.at("/uiCompositionPlan/bindings"))
                .isEqualTo(reopenedSource.at("/source/bindings"));

        HttpHeaders foreignHeaders = headers("another-user");
        ResponseEntity<String> foreignRefinement = rawPostPreview(refinementRequest, foreignHeaders);
        assertThat(foreignRefinement.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(foreignRefinement.getBody()).contains("persisted-ui-composition-config-not-found");

        ObjectNode updateDecision = semanticDecision(
                "decision-refine-missions-chart", "modify", "dashboard", "set_chart_type");
        AiTurnEventEnvelope updateTerminal = persistTerminalResult(
                refinedPreview, updateDecision, "update", firstEtag);
        ResponseEntity<String> updated = apply(
                refinedPreview.path("compiledFormPatch"),
                updateDecision,
                updateTerminal,
                '"' + firstEtag + '"',
                headers(USER));

        assertThat(updated.getStatusCode())
                .as("update apply response: %s", updated.getBody())
                .isEqualTo(HttpStatus.OK);
        JsonNode updatedBody = objectMapper.readTree(updated.getBody());
        assertThat(updatedBody.path("version").asLong()).isEqualTo(2L);
        assertThat(updatedBody.path("etag").asText()).isNotEqualTo(firstEtag);
        assertThat(updatedBody.at("/payload/widgets/2/definition/inputs/chartDocument/kind").asText())
                .isEqualTo("line");
        assertThat(updatedBody.at("/authoringSource/source/widgets/2/inputs/chartDocument/kind").asText())
                .isEqualTo("line");
        assertThat(updatedBody.at("/authoringSource/source/canvas/items/missions-detail/colSpan").asInt())
                .isEqualTo(5);
        assertThat(updatedBody.at("/authoringSource/sourceSha256").asText())
                .isNotEqualTo(createdBody.at("/authoringSource/sourceSha256").asText());

        ResponseEntity<String> stale = apply(
                refinedPreview.path("compiledFormPatch"),
                updateDecision,
                updateTerminal,
                '"' + firstEtag + '"',
                headers(USER));
        assertThat(stale.getStatusCode())
                .as("stale apply response: %s", stale.getBody())
                .isEqualTo(HttpStatus.PRECONDITION_FAILED);

        JsonNode winning = objectMapper.readTree(readConfig(headers(USER)).getBody());
        assertThat(winning.path("version").asLong()).isEqualTo(2L);
        assertThat(winning.at("/payload/widgets/1/definition/inputs/formId").asText())
                .isEqualTo("missions-detail");
        assertThat(winning.at("/payload/composition/links/0/from/ref/port").asText())
                .isEqualTo("selectionChange");
        assertThat(winning.at("/authoringSource/source/widgets/2/inputs/chartDocument/kind").asText())
                .isEqualTo("line");
        assertThat(winning.at("/authoringSource/source/canvas/items/missions-detail/colSpan").asInt())
                .isEqualTo(5);
        assertThat(winning.at("/authoringSource/source/diagnostics").isMissingNode()).isTrue();
    }

    private JsonNode postPreview(ObjectNode request, HttpHeaders headers) throws Exception {
        ResponseEntity<String> response = rawPostPreview(request, headers);
        assertThat(response.getStatusCode())
                .as("preview response: %s", response.getBody())
                .isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private ResponseEntity<String> rawPostPreview(ObjectNode request, HttpHeaders headers) throws Exception {
        return restTemplate.exchange(
                "/api/praxis/config/ai/authoring/page-preview",
                HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(request), headers),
                String.class);
    }

    private ResponseEntity<String> apply(
            JsonNode compiledPatch,
            JsonNode semanticDecision,
            AiTurnEventEnvelope terminal,
            String ifMatch,
            HttpHeaders baseHeaders) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.set("compiledFormPatch", compiledPatch);
        request.put("componentType", COMPONENT_TYPE);
        request.put("componentId", COMPONENT_ID);
        request.put("scope", "user");
        request.set("semanticDecision", semanticDecision);
        request.put("streamId", terminal.getStreamId().toString());
        request.put("resultEventId", terminal.getEventId().toString());
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(baseHeaders);
        headers.add("X-Updated-By", USER);
        if (ifMatch != null) {
            headers.add(HttpHeaders.IF_MATCH, ifMatch);
        }
        return restTemplate.exchange(
                "/api/praxis/config/ai/authoring/page-apply",
                HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(request), headers),
                String.class);
    }

    private ResponseEntity<String> readConfig(HttpHeaders headers) {
        return restTemplate.exchange(
                "/api/praxis/config/ui?componentType={componentType}&componentId={componentId}&scope=user",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class,
                COMPONENT_TYPE,
                COMPONENT_ID);
    }

    private AiTurnEventEnvelope persistTerminalResult(
            JsonNode preview,
            JsonNode semanticDecision,
            String mode,
            String baseEtag) {
        UUID streamId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        UUID turnId = UUID.randomUUID();
        Instant now = Instant.now();
        threadRepository.saveAndFlush(AiThread.builder()
                .threadId(threadId)
                .tenantId(TENANT)
                .environment(ENVIRONMENT)
                .userId(USER)
                .componentType("praxis-dynamic-page-builder")
                .componentId(COMPONENT_ID)
                .status(AiThreadStatus.ACTIVE)
                .summary("")
                .schemaHash("quickstart-http-proof")
                .createdAt(now)
                .lastUsedAt(now)
                .build());
        turnRepository.saveAndFlush(AiTurn.builder()
                .threadId(threadId)
                .turnId(turnId)
                .status(AiTurnStatus.PROCESSING)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build());

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("canApply", true);
        ObjectNode terminalPreview = (ObjectNode) preview.deepCopy();
        terminalPreview.withObject("/uiCompositionPlan/diagnostics/resourceWorkspaceGrounding")
                .put("status", "verified");
        payload.set("preview", terminalPreview);
        payload.putObject("intentResolution").set("semanticDecision", semanticDecision);
        ObjectNode target = payload.putObject("applyTarget");
        target.put("schemaVersion", "praxis-agentic-authoring-apply-target.v1");
        target.put("componentType", COMPONENT_TYPE);
        target.put("componentId", COMPONENT_ID);
        target.put("scope", "user");
        target.put("environment", ENVIRONMENT);
        target.put("mode", mode);
        if (baseEtag != null) {
            target.put("baseEtag", baseEtag);
        }
        return turnEventService.appendEvent(
                new AiPrincipalContext(TENANT, USER, ENVIRONMENT, true),
                streamId,
                threadId,
                turnId,
                "result",
                payload);
    }

    private ObjectNode createPreviewRequest() {
        ObjectNode request = basePreviewRequest("Crie o workspace master-detail de missões.");
        ObjectNode intent = request.putObject("intentResolution");
        intent.put("valid", true);
        intent.put("operationKind", "create");
        intent.put("artifactKind", "page");
        intent.put("changeKind", "create_artifact");
        intent.put("authoringProfile", "generic-page-change");
        intent.put("targetApp", "praxis-ui-angular");
        intent.put("targetComponentId", "praxis-dynamic-page-builder");
        return request;
    }

    private ObjectNode refinementPreviewRequest(
            JsonNode currentPage,
            JsonNode browserSourceHint,
            String baseEtag) {
        ObjectNode request = basePreviewRequest("Altere o gráfico selecionado para linhas.");
        request.set("currentPage", currentPage);
        ObjectNode intent = request.putObject("intentResolution");
        intent.put("valid", true);
        intent.put("operationKind", "modify");
        intent.put("artifactKind", "dashboard");
        intent.put("changeKind", "set_chart_type");
        intent.put("authoringProfile", "generic-page-change");
        intent.put("targetApp", "praxis-ui-angular");
        intent.put("targetComponentId", "praxis-dynamic-page-builder");
        intent.putObject("target")
                .put("widgetKey", "missions-chart")
                .put("componentId", "praxis-chart")
                .put("resourcePath", "/api/operations/missoes")
                .put("schemaUrl", "")
                .put("submitUrl", "")
                .put("submitMethod", "post");
        intent.putObject("selectedCandidate")
                .put("resourcePath", "/api/operations/missoes")
                .put("operation", "post")
                .put("schemaUrl", "/schemas/filtered?path=/api/operations/missoes/filter&operation=post&schemaType=response")
                .put("submitUrl", "/api/operations/missoes/filter")
                .put("submitMethod", "POST")
                .put("score", 0.97d)
                .put("reason", "server-grounded current page resource")
                .putArray("evidence")
                .add("api-metadata")
                .add("current-page-target-resource");
        intent.putArray("candidates");
        intent.putObject("gate")
                .put("gateId", "candidate-eligibility@0.1.0")
                .put("status", "eligible")
                .putArray("messages");
        intent.putArray("warnings");
        intent.putArray("failureCodes");
        ObjectNode context = request.putObject("contextHints");
        context.put("selectedWidgetKey", "missions-chart");
        context.set("uiCompositionAuthoringSource", browserSourceHint.deepCopy());
        context.putObject("agenticApplyTarget")
                .put("schemaVersion", "praxis-agentic-authoring-apply-target.v1")
                .put("componentType", COMPONENT_TYPE)
                .put("componentId", COMPONENT_ID)
                .put("scope", "user")
                .put("environment", ENVIRONMENT)
                .put("mode", "update")
                .put("baseEtag", baseEtag);
        return request;
    }

    private ObjectNode basePreviewRequest(String prompt) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("userPrompt", prompt);
        request.put("provider", "mock");
        request.put("model", "mock");
        request.put("apiKey", "test-key");
        return request;
    }

    private ObjectNode semanticDecision(
            String decisionId,
            String operationKind,
            String artifactKind,
            String changeKind) {
        return objectMapper.valueToTree(new AgenticAuthoringSemanticDecision(
                "praxis-agentic-authoring-semantic-decision.v1",
                decisionId,
                operationKind,
                artifactKind,
                changeKind,
                null,
                null,
                null,
                false,
                "",
                "",
                ""));
    }

    private ObjectNode chartTypeSelection() {
        ObjectNode selection = objectMapper.createObjectNode();
        selection.put("schemaVersion", "praxis-semantic-operation-selection.v2");
        selection.put("componentId", "praxis-chart");
        selection.put("requiresClarification", false);
        selection.put("clarificationReason", "");
        selection.putArray("selectedOperationIds").add("chart.type.set");
        selection.putArray("goals")
                .addObject()
                .put("description", "Change the selected chart to a line chart")
                .put("targetConcept", "chartType")
                .putArray("operationIds")
                .add("chart.type.set");
        return selection;
    }

    private ObjectNode chartTypePlan() {
        ObjectNode plan = objectMapper.createObjectNode();
        plan.put("schemaVersion", "praxis-component-edit-plan.v1");
        plan.put("componentId", "praxis-chart");
        plan.putArray("operations")
                .addObject()
                .put("operationId", "chart.type.set")
                .putObject("input")
                .put("kind", "line");
        return plan;
    }

    private JsonNode releasedChartManifest() throws Exception {
        JsonNode manifest = objectMapper
                .readTree(resource("/ai-registry/registry-snapshot.json"))
                .at("/components/praxis-chart/authoringManifest");
        assertThat(manifest.path("componentId").asText()).isEqualTo("praxis-chart");
        assertThat(manifest.path("operations"))
                .anySatisfy(operation -> assertThat(operation.path("operationId").asText())
                        .isEqualTo("chart.type.set"));
        return manifest;
    }

    private HttpHeaders headers(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Origin", "http://localhost:4003");
        headers.add("X-Tenant-ID", TENANT);
        headers.add("X-User-ID", userId);
        headers.add("X-Env", ENVIRONMENT);
        return headers;
    }

    private static EmbeddedPostgres startConfigPostgres() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
            try (Connection connection = postgres.getPostgresDatabase().getConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute(resource("/config/ui-composition-reopen-postgres-schema.sql"));
                statement.execute(resource("/db/migration/V61__add_ui_user_config_authoring_source.sql"));
            }
            return postgres;
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static String resource(String path) throws Exception {
        try (InputStream input = UiCompositionReopenPostgresHttpIntegrationTest.class
                .getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProviderConfiguration {

        @Bean
        InitialMasterDetailCompositionProvider initialMasterDetailCompositionProvider(
                ObjectMapper objectMapper) {
            return new InitialMasterDetailCompositionProvider(objectMapper);
        }
    }

    static final class InitialMasterDetailCompositionProvider
            implements AgenticAuthoringUiCompositionPlanProvider, Ordered {

        private final ObjectMapper objectMapper;

        InitialMasterDetailCompositionProvider(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Optional<AgenticAuthoringUiCompositionPlanResult> plan(
                AgenticAuthoringPlanRequest request) {
            if (request == null
                    || request.intentResolution() == null
                    || !"create".equals(request.intentResolution().operationKind())) {
                return Optional.empty();
            }
            try {
                return Optional.of(new AgenticAuthoringUiCompositionPlanResult(
                        true,
                        List.of(),
                        List.of("ui-composition-plan-provider:quickstart-http-proof"),
                        objectMapper.readTree("""
                                {
                                  "version":"1.0",
                                  "kind":"praxis.ui-composition-plan",
                                  "layoutPreset":"resource-master-detail",
                                  "state":{"values":{"selectedItem":null}},
                                  "canvas":{
                                    "mode":"grid","columns":12,"rowUnit":"80px","gap":"16px","autoRows":"fixed",
                                    "items":{
                                      "missions-master":{"col":1,"row":1,"colSpan":7,"rowSpan":8},
                                      "missions-detail":{"col":8,"row":1,"colSpan":5,"rowSpan":8},
                                      "missions-chart":{"col":1,"row":9,"colSpan":12,"rowSpan":5}
                                    }
                                  },
                                  "widgets":[
                                    {
                                      "key":"missions-master","componentId":"praxis-table",
                                      "inputs":{
                                        "resourcePath":"/api/operations/missoes","tableId":"missions-master",
                                        "config":{"behavior":{"selection":{"enabled":true,"type":"single"}}}
                                      },
                                      "outputs":{"selectionChange":"emit"}
                                    },
                                    {
                                      "key":"missions-detail","componentId":"praxis-dynamic-form",
                                      "inputs":{
                                        "resourcePath":"/api/operations/missoes","schemaSource":"resource",
                                        "mode":"view","formId":"missions-detail"
                                      }
                                    },
                                    {
                                      "key":"missions-chart","componentId":"praxis-chart",
                                      "inputs":{
                                        "resourcePath":"/api/operations/missoes",
                                        "chartDocument":{
                                          "kind":"bar",
                                          "data":{
                                            "source":{"kind":"resource","resourcePath":"/api/operations/missoes"},
                                            "categoryField":"status","valueField":"total"
                                          }
                                        }
                                      }
                                    }
                                  ],
                                  "bindings":[
                                    {
                                      "id":"missions-master.selectionChange->state.selectedItem","intent":"state-write",
                                      "from":{"kind":"component-port","widget":"missions-master","port":"selectionChange","direction":"output"},
                                      "to":{"kind":"state","path":"selectedItem"},
                                      "transform":{"kind":"pick-path","id":"pick-selected-row","path":"payload.row"}
                                    },
                                    {
                                      "id":"state.selectedItem->missions-detail.initialValue","intent":"state-read",
                                      "from":{"kind":"state","path":"selectedItem"},
                                      "to":{"kind":"component-port","widget":"missions-detail","port":"initialValue","direction":"input"},
                                      "condition":{"!!":[{"var":"state.selectedItem"}]}
                                    }
                                  ]
                                }
                                """),
                        null));
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
