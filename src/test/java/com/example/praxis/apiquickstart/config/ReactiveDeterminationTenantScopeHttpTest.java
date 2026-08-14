package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHead;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHeadActivationType;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHeadReader;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHeadScope;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;
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

/**
 * HTTP proof that a host determination resolves Config from authenticated server scope.
 * Caller-authored identity headers are deliberately conflicting and must never select a tenant.
 */
@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.csrf.disable=true",
                "app.security.write-disabled=true",
                "app.session.cookie-name=SESSION",
                "app.session.secure=false",
                "praxis.ai.provider=mock",
                "praxis.ai.security.corporate-mode=true",
                "praxis.ai.security.allow-default-tenant-in-corporate=true",
                "praxis.ai.security.server-default-tenant=tenant-a",
                "praxis.ai.security.server-default-environment=prod",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:reactive_scope_api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "CONFIG_DATASOURCE_URL=jdbc:h2:mem:reactive_scope_config;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "CONFIG_DATASOURCE_USERNAME=sa",
                "CONFIG_DATASOURCE_PASSWORD=",
                "config.datasource.url=jdbc:h2:mem:reactive_scope_config;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        })
class ReactiveDeterminationTenantScopeHttpTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired JwtTokenService jwtTokenService;

    @MockBean PublishedRuleSnapshotHeadReader headReader;
    @MockBean(name = "ragVectorStore") VectorStore ragVectorStore;

    @Test
    void executesWithSnapshotAggregateFromServerPrincipalAndIgnoresIdentityHeaders() {
        when(headReader.findActive(scope("tenant-a", "prod"))).thenReturn(Optional.of(head()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("payroll-user", "ADMIN"));
        headers.add("X-Tenant-ID", "tenant-b");
        headers.add("X-Env", "dev");
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/human-resources/folhas-pagamento/determinations/net-salary",
                HttpMethod.POST,
                new HttpEntity<>("{\"salarioBruto\":10000.00,\"totalDescontos\":1250.00}", headers),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("salarioLiquido").decimalValue())
                .isEqualByComparingTo("8750.00");
        verify(headReader).findActive(scope("tenant-a", "prod"));
        verify(headReader, never()).findActive(scope("tenant-b", "dev"));
    }

    @Test
    void executesPaymentDateFromTheSameAggregateHeadInServerPrincipalScope() {
        when(headReader.findActive(scope("tenant-a", "prod"))).thenReturn(Optional.of(head()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("payroll-user", "ADMIN"));
        headers.add("X-Tenant-ID", "tenant-b");
        headers.add("X-Env", "dev");
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/human-resources/folhas-pagamento/determinations/payment-date",
                HttpMethod.POST,
                new HttpEntity<>("{\"ano\":2026,\"mes\":4,\"salarioLiquido\":7549.65}", headers),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("dataPagamento").asText()).isEqualTo("2026-05-07");
        verify(headReader).findActive(scope("tenant-a", "prod"));
        verify(headReader, never()).findActive(scope("tenant-b", "dev"));
    }

    @Test
    void returnsServiceUnavailableWhenTheAggregateHeadIsMissing() {
        when(headReader.findActive(scope("tenant-a", "prod"))).thenReturn(Optional.empty());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate("payroll-user", "ADMIN"));
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/human-resources/folhas-pagamento/determinations/payment-date",
                HttpMethod.POST,
                new HttpEntity<>("{\"ano\":2026,\"mes\":4,\"salarioLiquido\":7549.65}", headers),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void separatesGovernancePublisherFromBusinessDeterminationPrincipal() {
        clearInvocations(headReader);
        when(headReader.findActive(scope("tenant-a", "prod"))).thenReturn(Optional.of(head()));

        HttpHeaders publisherHeaders = new HttpHeaders();
        publisherHeaders.setContentType(MediaType.APPLICATION_JSON);
        publisherHeaders.add(HttpHeaders.COOKIE, "SESSION=" + jwtTokenService.generate(
                "snapshot-publisher",
                "GOVERNANCE_PUBLISHER",
                List.of(
                        RuleGovernanceAuthorities.SNAPSHOT_PUBLISHER,
                        RuleGovernanceAuthorities.SNAPSHOT_READER)));
        ResponseEntity<JsonNode> publisherResponse = restTemplate.exchange(
                "/api/human-resources/folhas-pagamento/determinations/net-salary",
                HttpMethod.POST,
                new HttpEntity<>("{\"salarioBruto\":10000.00,\"totalDescontos\":1250.00}", publisherHeaders),
                JsonNode.class);

        assertThat(publisherResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(headReader, never()).findActive(scope("tenant-a", "prod"));

        HttpHeaders businessHeaders = new HttpHeaders();
        businessHeaders.setContentType(MediaType.APPLICATION_JSON);
        businessHeaders.add(HttpHeaders.COOKIE,
                "SESSION=" + jwtTokenService.generate("payroll-business-reader", "ADMIN"));
        ResponseEntity<JsonNode> businessResponse = restTemplate.exchange(
                "/api/human-resources/folhas-pagamento/determinations/net-salary",
                HttpMethod.POST,
                new HttpEntity<>("{\"salarioBruto\":10000.00,\"totalDescontos\":1250.00}", businessHeaders),
                JsonNode.class);

        assertThat(businessResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(businessResponse.getBody()).isNotNull();
        assertThat(businessResponse.getBody().path("salarioLiquido").decimalValue())
                .isEqualByComparingTo("8750.00");
        verify(headReader).findActive(scope("tenant-a", "prod"));
    }

    private static PublishedRuleSnapshotHeadScope scope(String tenant, String environment) {
        return new PublishedRuleSnapshotHeadScope(
                tenant, environment, PayrollReactiveDeterminationRuleSet.RULE_SET_KEY);
    }

    private static PublishedRuleSnapshotHead head() {
        PublishedRuleSnapshot snapshot = new PublishedRuleSnapshot(
                PublishedRuleSnapshot.SNAPSHOT_CONTRACT_VERSION,
                "payroll-tenant-a-v1",
                "tenant-a",
                "prod",
                PayrollReactiveDeterminationRuleSet.OWNER_SERVICE_KEY,
                1,
                "2026-08-13T12:00:00Z",
                null,
                PayrollReactiveDeterminationRuleSet.HOST_CONTRACT_VERSION,
                "2020-01-01T00:00:00Z",
                "2099-01-01T00:00:00Z",
                List.of(
                        new RuleSnapshotSource(
                                "net-definition", PayrollReactiveDeterminationRuleSet.NET_SALARY_KEY,
                                7, "B".repeat(64)),
                        new RuleSnapshotSource(
                                "date-definition", PayrollReactiveDeterminationRuleSet.PAYMENT_DATE_KEY,
                                9, "C".repeat(64))),
                List.of(
                        new RuleSnapshotApproval(
                                "approval-a", "RULE_COMPOSITION_APPROVER", "approver-a",
                                "2026-08-13T12:00:00Z", "D".repeat(64)),
                        new RuleSnapshotApproval(
                                "approval-b", "RULE_COMPOSITION_APPROVER", "approver-b",
                                "2026-08-13T12:00:00Z", "D".repeat(64))),
                PayrollReactiveDeterminationRuleSet.definition(1));
        var compiled = new PraxisRuleSnapshotCompiler(RuleBindingExecutorRegistry.empty())
                .compile(snapshot, PayrollReactiveDeterminationRuleSet.HOST_CONTRACT_VERSION);
        return new PublishedRuleSnapshotHead(
                compiled.snapshot(), compiled.snapshotContentHash(), "payroll-head-1", 1,
                PublishedRuleSnapshotHeadActivationType.ACTIVE);
    }
}
