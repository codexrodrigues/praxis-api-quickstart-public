package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                "spring.datasource.url=jdbc:h2:mem:quickstart_affordances_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_affordances_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class AiRegistryPresentationAffordancesQuickstartIntegrationTest {

    private static final Path ANGULAR_REGISTRY =
            Path.of("..", "praxis-ui-angular", "dist", "praxis-component-registry-ingestion.json");

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private AiRegistryRepository aiRegistryRepository;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private RagVectorStoreService ragVectorStoreService;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @Test
    void shouldIngestAngularRegistryAndExposePresentationAffordanceSlice() throws Exception {
        assumeTrue(
                Files.exists(ANGULAR_REGISTRY),
                "Run npm run generate:registry:ingestion in ../praxis-ui-angular before this monorepo proof.");

        Map<String, AiRegistry> savedRegistries = new LinkedHashMap<>();
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
            savedRegistries.put(registry.getRegistryKey(), registry);
            return registry;
        });
        when(ragVectorStoreService.corpusReleaseStatus(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(new RagVectorStoreService.RagCorpusReleaseStatus(
                        true,
                        true,
                        "default",
                        "default",
                        "local",
                        1,
                        1,
                        1,
                        java.util.Map.of("summary", 1L),
                        java.util.Map.of("allow", 1L),
                        List.of(),
                        "2026-06-03T00:00:00Z",
                        List.of()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Origin", "http://localhost:4003");
        String registryPayload = Files.readString(ANGULAR_REGISTRY);

        ResponseEntity<String> ingest = restTemplate.postForEntity(
                "/api/praxis/config/ai-registry/component-definitions",
                new HttpEntity<>(registryPayload, headers),
                String.class);

        assertThat(ingest.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(savedRegistries.get("praxis-table")).isNotNull();

        ResponseEntity<String> slice = restTemplate.getForEntity(
                "/api/praxis/config/ai/authoring/manifests/praxis-table/presentation-affordances",
                String.class);

        assertThat(slice.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(slice.getBody()).contains(
                "\"componentId\":\"praxis-table\"",
                "praxis-ui-angular:projects/praxis-table/src/lib/ai/praxis-table-authoring-manifest.ts#presentationAffordances",
                "table.column.semantic-renderers",
                "secondary-line",
                "\"id\":\"table.column.date-formatting\"",
                "\"unknownCompatible\":false");
    }
}
