package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.domain.AiRegistry;
import org.praxisplatform.config.domain.Scope;
import org.praxisplatform.config.rag.RagVectorStoreService;
import org.praxisplatform.config.repository.AiRegistryRepository;
import org.praxisplatform.config.service.EmbeddingService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.read-open=false",
                "app.security.write-disabled=false",
                "app.security.csrf.disable=true",
                "app.session.cookie-name=SESSION",
                "app.session.secure=false",
                "app.session.samesite=Lax",
                "praxis.stats.enabled=true",
                "praxis.ai.provider=mock",
                "praxis.ai.authoring.http-enabled=true",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "praxis.ai.security.corporate-mode=false",
                "praxis.ai.security.allow-header-identity-in-local=true",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:quickstart_ai_registry_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_ai_registry_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class AiRegistryChartAuthoringQuickstartIntegrationTest {

    private static final Path ANGULAR_REGISTRY =
            Path.of(System.getProperty(
                    "praxis.angular.registry.path",
                    Path.of("..", "praxis-ui-angular", "dist", "praxis-component-registry-ingestion.json")
                            .toString()))
                    .normalize();
    private static final String VALIDATE_PLAN_PATH =
            "/api/praxis/config/ai/authoring/manifests/praxis-chart/validate-plan";
    private static final String COMPILE_PATCH_PATH =
            "/api/praxis/config/ai/authoring/manifests/praxis-chart/compile-patch";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiRegistryRepository aiRegistryRepository;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private RagVectorStoreService ragVectorStoreService;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @BeforeEach
    void configureRegistryIngestionTimeout() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        restTemplate.getRestTemplate().setRequestFactory(requestFactory);
    }

    @Test
    void shouldExecuteGovernedPointClickAuthoringAndFailClosedForInvalidTargetCatalogs() throws Exception {
        assumeTrue(
                Files.exists(ANGULAR_REGISTRY),
                "Run npm run generate:registry:ingestion in ../praxis-ui-angular before this monorepo proof.");

        Map<String, AiRegistry> savedRegistries = new LinkedHashMap<>();
        stubRegistryIngestion(savedRegistries);
        HttpHeaders headers = requestHeaders();

        ResponseEntity<String> ingest = restTemplate.postForEntity(
                "/api/praxis/config/ai-registry/component-definitions",
                new HttpEntity<>(Files.readString(ANGULAR_REGISTRY), headers),
                String.class);

        assertThat(ingest.getStatusCode())
                .as("registry ingest response: %s", ingest.getBody())
                .isEqualTo(HttpStatus.ACCEPTED);
        assertThat(savedRegistries.get("praxis-chart")).isNotNull();

        ObjectNode governedTarget = targetDescriptor("employees-filter", "pointClick");
        JsonNode validRequest = pointClickRequest(List.of(governedTarget));

        JsonNode validation = postJson(VALIDATE_PLAN_PATH, validRequest, headers);
        assertThat(validation.path("valid").isBoolean()).isTrue();
        assertThat(validation.path("valid").asBoolean())
                .as("valid pointClick response: %s", validation)
                .isTrue();
        assertThat(validation.path("failures").isArray()).isTrue();
        assertThat(validation.path("failures").size()).isZero();
        assertThat(validation.path("normalizedPlan").path("componentId").asText()).isEqualTo("praxis-chart");
        assertThat(validation.path("normalizedPlan").path("operations").get(0).path("operationId").asText())
                .isEqualTo("pointClick.configure");

        JsonNode compiled = postJson(COMPILE_PATCH_PATH, validRequest, headers);
        assertThat(compiled.path("compiled").isBoolean()).isTrue();
        assertThat(compiled.path("compiled").asBoolean())
                .as("compiled pointClick response: %s", compiled)
                .isTrue();
        assertThat(compiled.path("failures").isArray()).isTrue();
        assertThat(compiled.path("failures").size()).isZero();

        JsonNode compiledOperation = compiled.path("patch").path("compiledOperations").get(0);
        assertThat(compiledOperation.path("operationId").asText()).isEqualTo("pointClick.configure");
        assertThat(compiledOperation.path("op").asText())
                .as("compiled pointClick operation: %s", compiledOperation)
                .isEqualTo("domain-patch");
        assertThat(compiledOperation.path("compilerBoundary").asBoolean()).isTrue();

        JsonNode domainOperation = compiled.path("patch").path("operations").get(0);
        assertThat(domainOperation.path("domainHandler").asText())
                .isEqualTo("chart-event-point-click-configure");
        assertThat(domainOperation.path("op").asText()).isEqualTo("configure-chart-point-click-event");
        assertThat(domainOperation.path("path").asText()).isEqualTo("chartDocument.events.pointClick");
        assertThat(compiledOperation.path("path").asText()).isEqualTo("chartDocument.events.pointClick");

        JsonNode proposedConfig = compiled.path("patch").path("proposedConfig");
        JsonNode pointClick = proposedConfig.path("chartDocument").path("events").path("pointClick");
        assertThat(pointClick.path("action").asText()).isEqualTo("filter-widget");
        assertThat(pointClick.path("target").asText()).isEqualTo("employees-filter");
        assertThat(pointClick.has("event")).isFalse();
        assertThat(proposedConfig.has("availableTargets")).isFalse();
        assertThat(proposedConfig.has("validationContext")).isFalse();

        JsonNode mismatchedEventRequest = pointClickRequest(List.of(targetDescriptor(
                "employees-filter",
                "crossFilter")));
        assertEventTargetRejectedByBothEndpoints(mismatchedEventRequest, headers);

        ObjectNode targetWithoutEvents = targetDescriptor("employees-filter", "pointClick");
        targetWithoutEvents.remove("events");
        assertEventTargetRejectedByBothEndpoints(pointClickRequest(List.of(targetWithoutEvents)), headers);

        assertEventTargetRejectedByBothEndpoints(
                pointClickRequest(List.of(
                        targetDescriptor("employees-filter", "pointClick"),
                        targetDescriptor("employees-filter", "pointClick"))),
                headers);
    }

    private void stubRegistryIngestion(Map<String, AiRegistry> savedRegistries) {
        when(embeddingService.embed(anyString())).thenReturn(List.of(0.01f, 0.02f, 0.03f));
        when(aiRegistryRepository.findByRegistryTypeAndRegistryKeyAndComponentTypeAndScopeAndScopeKey(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(Scope.class),
                        anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(savedRegistries.get(invocation.getArgument(1))));
        when(aiRegistryRepository.save(any(AiRegistry.class))).thenAnswer(invocation -> {
            AiRegistry registry = invocation.getArgument(0);
            if ("praxis-chart".equals(registry.getRegistryKey())) {
                savedRegistries.put(registry.getRegistryKey(), registry);
            }
            return registry;
        });
        when(ragVectorStoreService.corpusReleaseStatus(
                        anyString(),
                        anyString(),
                        anyString(),
                        org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(new RagVectorStoreService.RagCorpusReleaseStatus(
                        true,
                        true,
                        "default",
                        "default",
                        "local",
                        1,
                        1,
                        1,
                        Map.of("summary", 1L),
                        Map.of("allow", 1L),
                        List.of(),
                        "2026-07-15T00:00:00Z",
                        List.of()));
    }

    private HttpHeaders requestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Origin", "http://localhost:4003");
        headers.add("X-Tenant-ID", "desenv");
        headers.add("X-User-ID", "demo");
        headers.add("X-Env", "local");
        return headers;
    }

    private ObjectNode pointClickRequest(List<ObjectNode> availableTargets) {
        ObjectNode request = objectMapper.createObjectNode();
        ObjectNode chartDocument = request.putObject("config").putObject("chartDocument");
        chartDocument.put("version", "0.1.0");
        chartDocument.put("kind", "bar");
        chartDocument.putObject("source").put("kind", "derived");
        chartDocument.putArray("dimensions").addObject().put("field", "department");
        chartDocument.putArray("metrics")
                .addObject()
                .put("field", "headcount")
                .put("aggregation", "count");

        var targetCatalog = request.putObject("validationContext").putArray("availableTargets");
        availableTargets.forEach(targetCatalog::add);

        ObjectNode plan = request.putObject("plan");
        plan.put("operationId", "pointClick.configure");
        plan.putObject("input")
                .put("action", "filter-widget")
                .put("target", "employees-filter");
        return request;
    }

    private ObjectNode targetDescriptor(String id, String event) {
        ObjectNode target = objectMapper.createObjectNode();
        target.put("id", id);
        target.put("kind", "widget");
        target.putArray("actions").add("filter-widget");
        target.putArray("events").add(event);
        return target;
    }

    private void assertEventTargetRejectedByBothEndpoints(JsonNode request, HttpHeaders headers) throws Exception {
        JsonNode validation = postJson(VALIDATE_PLAN_PATH, request, headers);
        assertRejectedResult(validation, "valid");

        JsonNode compiled = postJson(COMPILE_PATCH_PATH, request, headers);
        assertRejectedResult(compiled, "compiled");
        assertThat(compiled.path("patch").isObject()).isTrue();
        assertThat(compiled.path("patch").size()).isZero();
    }

    private void assertRejectedResult(JsonNode response, String outcomeField) {
        assertThat(response.path(outcomeField).isBoolean()).isTrue();
        assertThat(response.path(outcomeField).asBoolean()).isFalse();
        JsonNode failures = response.path("failures");
        assertThat(failures.isArray()).isTrue();
        assertThat(failures.size()).isEqualTo(1);
        assertThat(failures.get(0).isTextual()).isTrue();
        assertThat(failures.get(0).asText())
                .startsWith("validator event-target-governed failed for pointClick.configure:");
    }

    private JsonNode postJson(String path, JsonNode payload, HttpHeaders headers) throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
                path,
                new HttpEntity<>(objectMapper.writeValueAsString(payload), headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return objectMapper.readTree(response.getBody());
    }
}
