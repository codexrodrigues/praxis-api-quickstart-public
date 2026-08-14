package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.ai.authoring.AgenticAuthoringTurnStreamRequest;
import org.praxisplatform.config.ai.authoring.AgenticAuthoringTurnStreamService;
import org.praxisplatform.config.dto.AgenticAuthoringTurnStreamStartResponse;
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
                "praxis.ai.provider=mock",
                "praxis.ai.authoring.http-enabled=true",
                "praxis.ai.security.corporate-mode=true",
                "praxis.ai.security.allow-default-tenant-in-corporate=true",
                "praxis.ai.security.server-default-tenant=desenv",
                "praxis.ai.security.server-default-environment=local",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:policy_explanation_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:policy_explanation_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        })
class PolicyStudioDecisionExplanationSecurityIntegrationTest {

    private static final String START = "/api/praxis/config/ai/authoring/turn/stream/start";

    @Autowired TestRestTemplate restTemplate;
    @Autowired JwtTokenService jwtTokenService;

    @MockBean AgenticAuthoringTurnStreamService turnStreamService;
    @MockBean(name = "ragVectorStore") VectorStore ragVectorStore;

    @Test
    void selectedDecisionRequiresDefinitionReaderBeforeTheTurnIsEnqueued() {
        HttpHeaders anonymous = headers(null);
        ResponseEntity<Void> unauthenticated = restTemplate.postForEntity(
                START, new HttpEntity<>(request(), anonymous), Void.class);
        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(turnStreamService, never()).start(any(), any(), any());

        HttpHeaders operator = headers(jwtTokenService.generate(
                "policy-operator", "HUMAN", List.of(RuleGovernanceAuthorities.SNAPSHOT_READER)));
        ResponseEntity<Void> forbidden = restTemplate.postForEntity(
                START, new HttpEntity<>(request(), operator), Void.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(turnStreamService, never()).start(any(), any(), any());

        clearInvocations(turnStreamService);
        when(turnStreamService.start(any(AgenticAuthoringTurnStreamRequest.class), any(), any()))
                .thenReturn(new AgenticAuthoringTurnStreamService.StartResult(startResponse(), true));
        HttpHeaders reader = headers(jwtTokenService.generate(
                "policy-reader", "HUMAN", List.of(RuleGovernanceAuthorities.DEFINITION_READER)));
        ResponseEntity<AgenticAuthoringTurnStreamStartResponse> accepted = restTemplate.postForEntity(
                START, new HttpEntity<>(request(), reader), AgenticAuthoringTurnStreamStartResponse.class);

        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(accepted.getBody()).isNotNull();
        verify(turnStreamService).start(any(AgenticAuthoringTurnStreamRequest.class), any(), any());
    }

    private static HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.add(HttpHeaders.COOKIE, "SESSION=" + token);
        return headers;
    }

    private static Map<String, Object> request() {
        return Map.of(
                "userPrompt", "Explique a decisao selecionada",
                "targetApp", "praxis-policy-studio",
                "targetComponentId", "policy-decision-explanation",
                "contextHints", Map.of(
                        "selectedDomainDecisionRef", Map.of(
                                "schemaVersion", "praxis.ai.context-hints.domain-decision/v1",
                                "definitionId", "7b0fca89-cb64-40bf-8eea-d3467083bbf4",
                                "ruleKey", "grant.amount-parameters",
                                "version", 3,
                                "source", "policy-studio-selection")));
    }

    private static AgenticAuthoringTurnStreamStartResponse startResponse() {
        return AgenticAuthoringTurnStreamStartResponse.builder()
                .streamId(UUID.fromString("00000000-0000-0000-0000-00000000b001"))
                .threadId(UUID.fromString("00000000-0000-0000-0000-00000000b002"))
                .turnId(UUID.fromString("00000000-0000-0000-0000-00000000b003"))
                .eventSchemaVersion("v1")
                .expiresAt(Instant.now().plusSeconds(600))
                .fallbackAuthoringUrl("/api/praxis/config/ai/authoring/page-preview")
                .build();
    }
}
