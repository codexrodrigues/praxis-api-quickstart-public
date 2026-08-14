package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.ai.authoring.AgenticAuthoringPlanRequest;
import org.praxisplatform.config.ai.authoring.AgenticAuthoringUiCompositionPlanProvider;
import org.praxisplatform.config.ai.authoring.AgenticAuthoringUiCompositionPlanResult;
import org.praxisplatform.config.domain.AiRegistry;
import org.praxisplatform.config.domain.Scope;
import org.praxisplatform.config.repository.AiRegistryRepository;
import org.praxisplatform.config.service.EmbeddingService;
import org.praxisplatform.config.service.UserConfigService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
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

@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=true",
                "app.security.config-origin-restriction.allowed-origins=http://localhost:4003",
                "app.security.read-open=false",
                "app.security.write-disabled=false",
                "app.security.csrf.disable=true",
                "app.session.cookie-name=SESSION",
                "app.session.secure=false",
                "app.session.samesite=Lax",
                "praxis.stats.enabled=true",
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
                "spring.datasource.url=jdbc:h2:mem:quickstart_governed_template_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_governed_template_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        })
@Import(GovernedUiCompositionTemplateReferenceQuickstartIntegrationTest.ProviderConfiguration.class)
class GovernedUiCompositionTemplateReferenceQuickstartIntegrationTest {

    private static final String REGISTRY_KEY =
            "praxis-dynamic-page:quickstart-governed-template-reference";
    private static final String MISSING_REGISTRY_KEY =
            "praxis-dynamic-page:quickstart-governed-template-reference-missing";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ControlledTemplateReferenceProvider templateReferenceProvider;

    @MockBean
    private AiRegistryRepository aiRegistryRepository;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private UserConfigService userConfigService;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    private final Map<String, AiRegistry> storedTemplates = new LinkedHashMap<>();

    @BeforeEach
    void setUpRegistryFixture() {
        storedTemplates.clear();
        templateReferenceProvider.clear();
        when(embeddingService.embed(anyString())).thenReturn(List.of(0.01f, 0.02f, 0.03f));
        when(aiRegistryRepository.findByRegistryTypeAndRegistryKeyAndComponentTypeAndScopeAndScopeKey(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(Scope.class),
                        anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(storedTemplates.get(invocation.getArgument(1))));
        when(aiRegistryRepository.save(any(AiRegistry.class))).thenAnswer(invocation -> {
            AiRegistry registry = invocation.getArgument(0);
            if (registry.getId() == null) {
                registry.setId(UUID.randomUUID());
                registry.onInsert();
            }
            storedTemplates.put(registry.getRegistryKey(), registry);
            return registry;
        });
    }

    @Test
    void shouldResolveContentPinnedTemplateThroughTheHostedHttpContract() throws Exception {
        JsonNode upsert = upsertTemplate(expandedPlan("employee-dossier"));
        String configSha256 = upsert.at("/revision/configSha256").asText();
        templateReferenceProvider.reference(REGISTRY_KEY, configSha256);

        JsonNode preview = postPreview();

        assertThat(preview.path("valid").asBoolean()).isTrue();
        assertThat(preview.path("failureCodes")).isEmpty();
        assertThat(preview.path("warnings").toString()).contains(
                "ui-composition-template-reference-resolved",
                "ui-composition-plan-compiled-by-config");
        assertThat(preview.at("/uiCompositionPlan/widgets/0/key").asText())
                .isEqualTo("employee-dossier");
        assertThat(preview.at("/uiCompositionPlan/diagnostics/templateResolution/registryKey").asText())
                .isEqualTo(REGISTRY_KEY);
        assertThat(preview.at("/uiCompositionPlan/diagnostics/templateResolution/configSha256").asText())
                .isEqualTo(configSha256);
        assertThat(preview.at("/compiledFormPatch/patch/page/widgets/0/key").asText())
                .isEqualTo("employee-dossier");
        assertThat(preview.at("/compiledFormPatch/patch/page/templateRef").isMissingNode()).isTrue();
        assertThat(preview.at("/compiledFormPatch/patch/page/diagnostics/templateResolution").isMissingNode()).isTrue();
    }

    @Test
    void shouldFailClosedWhenTheReferencedTemplateHeadHasChanged() throws Exception {
        JsonNode firstRevision = upsertTemplate(expandedPlan("employee-dossier-v1"));
        String staleHash = firstRevision.at("/revision/configSha256").asText();
        JsonNode currentRevision = upsertTemplate(expandedPlan("employee-dossier-v2"));
        assertThat(currentRevision.at("/revision/configSha256").asText()).isNotEqualTo(staleHash);
        templateReferenceProvider.reference(REGISTRY_KEY, staleHash);

        JsonNode preview = postPreview();

        assertThat(preview.path("valid").asBoolean()).isFalse();
        assertThat(preview.path("failureCodes").toString())
                .contains("ui-composition-template-hash-mismatch");
        assertThat(preview.at("/compiledFormPatch/patch/page").isMissingNode()).isTrue();
    }

    @Test
    void shouldFailClosedWhenTheExactRegistryKeyDoesNotExist() throws Exception {
        templateReferenceProvider.reference(MISSING_REGISTRY_KEY, "a".repeat(64));

        JsonNode preview = postPreview();

        assertThat(preview.path("valid").asBoolean()).isFalse();
        assertThat(preview.path("failureCodes").toString())
                .contains("ui-composition-template-not-found");
    }

    @Test
    void shouldFailClosedWhenTheReferencedTemplateIsInactive() throws Exception {
        JsonNode upsert = upsertTemplate(expandedPlan("employee-dossier"));
        String configSha256 = upsert.at("/revision/configSha256").asText();
        storedTemplates.get(REGISTRY_KEY).setStatus("inactive");
        templateReferenceProvider.reference(REGISTRY_KEY, configSha256);

        JsonNode preview = postPreview();

        assertThat(preview.path("valid").asBoolean()).isFalse();
        assertThat(preview.path("failureCodes").toString())
                .contains("ui-composition-template-inactive");
    }

    private JsonNode upsertTemplate(JsonNode authoringPlan) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("aiDescription", "Governed Quickstart HTTP reference proof");
        request.putObject("templateMeta").put("source", "quickstart-integration-test");
        request.putObject("configJson").set("authoringPlan", authoringPlan);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/praxis/config/ai-registry/templates/{registryKey}",
                HttpMethod.PUT,
                new HttpEntity<>(objectMapper.writeValueAsString(request), requestHeaders()),
                String.class,
                REGISTRY_KEY);

        assertThat(response.getStatusCode())
                .as("template upsert response: %s", response.getBody())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.at("/revision/version").asLong()).isPositive();
        assertThat(body.at("/revision/etag").asText()).isNotBlank();
        assertThat(body.at("/revision/configSha256").asText()).matches("[0-9a-f]{64}");

        ResponseEntity<String> readback = restTemplate.exchange(
                "/api/praxis/config/ai-registry/templates/{registryKey}",
                HttpMethod.GET,
                new HttpEntity<>(requestHeaders()),
                String.class,
                REGISTRY_KEY);
        assertThat(readback.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readback.getHeaders().getETag())
                .isEqualTo('"' + body.at("/revision/etag").asText() + '"');
        assertThat(objectMapper.readTree(readback.getBody()).at("/revision/configSha256").asText())
                .isEqualTo(body.at("/revision/configSha256").asText());
        return body;
    }

    private JsonNode postPreview() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("userPrompt", "Materialize a pagina governada selecionada pelo contexto semantico.");
        request.put("provider", "mock");
        request.put("model", "mock");
        request.put("apiKey", "test-key");
        ObjectNode intent = request.putObject("intentResolution");
        intent.put("valid", true);
        intent.put("operationKind", "create");
        intent.put("artifactKind", "page");
        intent.put("changeKind", "create_artifact");
        intent.put("authoringProfile", "generic-page-change");
        intent.put("targetApp", "praxis-ui-angular");
        intent.put("targetComponentId", "praxis-dynamic-page-builder");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/praxis/config/ai/authoring/page-preview",
                new HttpEntity<>(objectMapper.writeValueAsString(request), requestHeaders()),
                String.class);

        assertThat(response.getStatusCode())
                .as("page preview response: %s", response.getBody())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return objectMapper.readTree(response.getBody());
    }

    private HttpHeaders requestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Origin", "http://localhost:4003");
        headers.add("X-Tenant-ID", "governed-template-http-proof");
        headers.add("X-User-ID", "quickstart-integration-test");
        headers.add("X-Env", "local");
        return headers;
    }

    private JsonNode expandedPlan(String widgetKey) {
        ObjectNode plan = objectMapper.createObjectNode();
        plan.put("version", "1.0");
        plan.put("kind", "praxis.ui-composition-plan");
        plan.putArray("widgets")
                .addObject()
                .put("key", widgetKey)
                .put("componentId", "praxis-tabs");
        return plan;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProviderConfiguration {

        @Bean
        ControlledTemplateReferenceProvider controlledTemplateReferenceProvider(ObjectMapper objectMapper) {
            return new ControlledTemplateReferenceProvider(objectMapper);
        }
    }

    static final class ControlledTemplateReferenceProvider
            implements AgenticAuthoringUiCompositionPlanProvider, Ordered {

        private final ObjectMapper objectMapper;
        private JsonNode reference;

        ControlledTemplateReferenceProvider(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        void reference(String registryKey, String configSha256) {
            ObjectNode candidate = objectMapper.createObjectNode();
            candidate.put("version", "1.0");
            candidate.put("kind", "praxis.ui-composition-plan");
            candidate.putObject("templateRef")
                    .put("registryKey", registryKey)
                    .put("configSha256", configSha256);
            candidate.putObject("overrides");
            reference = candidate;
        }

        void clear() {
            reference = null;
        }

        @Override
        public Optional<AgenticAuthoringUiCompositionPlanResult> plan(
                AgenticAuthoringPlanRequest request) {
            if (reference == null) {
                return Optional.empty();
            }
            return Optional.of(new AgenticAuthoringUiCompositionPlanResult(
                    true,
                    List.of(),
                    List.of("ui-composition-plan-provider:quickstart-governed-template-reference"),
                    reference.deepCopy(),
                    null));
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }
}
