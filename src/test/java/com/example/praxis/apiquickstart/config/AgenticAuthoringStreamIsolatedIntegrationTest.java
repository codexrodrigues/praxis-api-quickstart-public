package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.praxisplatform.config.ai.authoring.AgenticAuthoringTurnStreamRequest;
import org.praxisplatform.config.ai.authoring.AgenticAuthoringTurnStreamService;
import org.praxisplatform.config.dto.AgenticAuthoringTurnStreamStartResponse;
import org.praxisplatform.config.service.AiPrincipalContext;
import org.praxisplatform.config.service.AiStreamAccessTokenService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
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
                "praxis.ai.security.corporate-mode=false",
                "praxis.ai.security.allow-header-identity-in-local=true",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "praxis.ai.stream.auth.mode=signed-url-token",
                "praxis.ai.stream.auth.token-secret=quickstart-authoring-stream-test-secret-32",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:quickstart_authoring_stream_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_authoring_stream_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class AgenticAuthoringStreamIsolatedIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AiStreamAccessTokenService streamAccessTokenService;

    @MockBean
    private AgenticAuthoringTurnStreamService turnStreamService;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @Test
    void shouldHostAuthoringStreamStartAndProbeWithSignedUrlToken() {
        UUID streamId = UUID.fromString("00000000-0000-0000-0000-00000000a001");
        UUID threadId = UUID.fromString("00000000-0000-0000-0000-00000000a002");
        UUID turnId = UUID.fromString("00000000-0000-0000-0000-00000000a003");
        Instant expiresAt = Instant.now().plusSeconds(600);
        AiPrincipalContext tokenContext = new AiPrincipalContext("desenv", "demo", "local", true);
        String accessToken = streamAccessTokenService.issueToken(streamId, tokenContext, expiresAt);
        AgenticAuthoringTurnStreamStartResponse startResponse = AgenticAuthoringTurnStreamStartResponse.builder()
                .streamId(streamId)
                .threadId(threadId)
                .turnId(turnId)
                .eventSchemaVersion("v1")
                .streamAuthMode("signed_url_token")
                .streamAccessToken(accessToken)
                .expiresAt(expiresAt)
                .fallbackAuthoringUrl("http://localhost/api/praxis/config/ai/authoring/page-preview")
                .build();
        when(turnStreamService.start(any(AgenticAuthoringTurnStreamRequest.class), any(), any()))
                .thenReturn(new AgenticAuthoringTurnStreamService.StartResult(startResponse, true));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Tenant-ID", "desenv");
        headers.add("X-User-ID", "demo");
        headers.add("X-Env", "local");
        ResponseEntity<AgenticAuthoringTurnStreamStartResponse> start = restTemplate.postForEntity(
                "/api/praxis/config/ai/authoring/turn/stream/start",
                new HttpEntity<>(Map.of(
                        "userPrompt", "crie painel de visualizacao de graficos",
                        "targetApp", "praxis-ui-angular",
                        "targetComponentId", "praxis-dynamic-page-builder",
                        "currentRoute", "/page-builder-ia",
                        "clientTurnId", "quickstart-stream-smoke-1",
                        "currentPage", Map.of("widgets", java.util.List.of())),
                        headers),
                AgenticAuthoringTurnStreamStartResponse.class);

        assertThat(start.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(start.getBody()).isNotNull();
        assertThat(start.getBody().getStreamAuthMode()).isEqualTo("signed_url_token");
        assertThat(start.getBody().getStreamAccessToken()).isNotBlank();

        ResponseEntity<Void> probe = restTemplate.exchange(
                "/api/praxis/config/ai/authoring/turn/stream/{streamId}/probe?accessToken={accessToken}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Void.class,
                streamId,
                accessToken);

        assertThat(probe.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ArgumentCaptor<AiPrincipalContext> principalCaptor = ArgumentCaptor.forClass(AiPrincipalContext.class);
        verify(turnStreamService).probe(eq(streamId), principalCaptor.capture());
        assertThat(principalCaptor.getValue().tenantId()).isEqualTo("desenv");
        assertThat(principalCaptor.getValue().userId()).isEqualTo("demo");
        assertThat(principalCaptor.getValue().environment()).isEqualTo("local");
        assertThat(principalCaptor.getValue().resolvedFromServerPrincipal()).isTrue();
    }
}
