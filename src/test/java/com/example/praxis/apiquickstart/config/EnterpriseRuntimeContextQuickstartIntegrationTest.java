package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.read-open=true",
                "app.security.write-disabled=false",
                "app.security.csrf.disable=true",
                "praxis.ai.provider=mock",
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
                "spring.datasource.url=jdbc:h2:mem:quickstart_runtime_context_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_runtime_context_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class EnterpriseRuntimeContextQuickstartIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private QuickstartEnterpriseRuntimeContextProvider runtimeContextProvider;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @Test
    void shouldHostEnterpriseRuntimeContextThroughQuickstartProvider() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Tenant-ID", "tenant-demo");
        headers.add("X-User-ID", "user-demo");
        headers.add("X-Env", "local");
        headers.add("Accept-Language", "pt-BR,pt;q=0.9");
        headers.add("X-Timezone", "America/Sao_Paulo");
        headers.add("X-Praxis-Profile-ID", "operator");
        headers.add("X-Praxis-Module-Key", "payroll");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/praxis/runtime/context",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(runtimeContextProvider).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("schemaVersion")).isEqualTo("praxis-enterprise-runtime-context.v1");
        assertThat(response.getBody().get("environment")).isEqualTo("local");
        assertThat(response.getBody().get("locale")).isEqualTo("pt-BR");
        assertThat(response.getBody().get("timezone")).isEqualTo("America/Sao_Paulo");
        assertThat(response.getBody().get("activeProfileId")).isEqualTo("operator");
        assertThat(response.getBody().get("activeModuleKey")).isEqualTo("payroll");

        Map<?, ?> user = (Map<?, ?>) response.getBody().get("user");
        assertThat(user.get("userId")).isEqualTo("user-demo");
        assertThat(user.get("displayName")).isEqualTo("Praxis demo user");
        assertThat(user.get("resolvedFromServerPrincipal")).isEqualTo(false);

        Map<?, ?> tenant = (Map<?, ?>) response.getBody().get("activeTenant");
        assertThat(tenant.get("tenantId")).isEqualTo("tenant-demo");
        assertThat(tenant.get("label")).isEqualTo("Praxis demo tenant");
        assertThat(tenant.get("active")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        List<String> capabilities = (List<String>) response.getBody().get("capabilities");
        assertThat(capabilities)
                .contains("runtime.context.read", "runtime.context.demo-provider");
    }

    @Test
    void shouldHostEnterpriseRuntimeTenantsThroughQuickstartProvider() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Tenant-ID", "tenant-demo");
        headers.add("X-User-ID", "user-demo");
        headers.add("X-Env", "local");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/praxis/runtime/tenants",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(runtimeContextProvider).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("schemaVersion")).isEqualTo("praxis-enterprise-runtime-tenants.v1");

        Map<?, ?> activeTenant = (Map<?, ?>) response.getBody().get("activeTenant");
        assertThat(activeTenant.get("tenantId")).isEqualTo("tenant-demo");
        assertThat(activeTenant.get("label")).isEqualTo("Praxis demo tenant");
        assertThat(activeTenant.get("active")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tenants = (List<Map<String, Object>>) response.getBody().get("tenants");
        assertThat(tenants)
                .extracting(tenant -> tenant.get("tenantId"))
                .containsExactly("tenant-demo", "corporate-holding", "shared-services");
        assertThat(tenants)
                .filteredOn(tenant -> Boolean.TRUE.equals(tenant.get("active")))
                .singleElement()
                .extracting(tenant -> tenant.get("tenantId"))
                .isEqualTo("tenant-demo");

        @SuppressWarnings("unchecked")
        List<String> capabilities = (List<String>) response.getBody().get("capabilities");
        assertThat(capabilities)
                .contains("runtime.tenants.read", "runtime.tenants.demo-provider");
    }

    @Test
    void shouldHostEnterpriseRuntimeContextSwitchThroughQuickstartProvider() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Tenant-ID", "tenant-demo");
        headers.add("X-User-ID", "user-demo");
        headers.add("X-Env", "local");
        headers.add("Accept-Language", "en-US,en;q=0.9");
        headers.add("X-Timezone", "UTC");
        headers.add("X-Praxis-Profile-ID", "manager");
        headers.add("X-Praxis-Module-Key", "benefits");

        Map<String, Object> command = Map.of(
                "targetTenantId", "corporate-holding",
                "targetProfileId", "operator",
                "targetModuleKey", "payroll",
                "locale", "pt-BR",
                "timezone", "America/Sao_Paulo",
                "reason", "quickstart demo switch");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/praxis/runtime/context",
                HttpMethod.PUT,
                new HttpEntity<>(command, headers),
                Map.class);

        assertThat(runtimeContextProvider).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("schemaVersion")).isEqualTo("praxis-enterprise-runtime-context-switch.v1");
        assertThat(response.getBody().get("accepted")).isEqualTo(true);

        Map<?, ?> effectiveContext = (Map<?, ?>) response.getBody().get("effectiveContext");
        assertThat(effectiveContext.get("schemaVersion")).isEqualTo("praxis-enterprise-runtime-context.v1");
        assertThat(effectiveContext.get("environment")).isEqualTo("local");
        assertThat(effectiveContext.get("locale")).isEqualTo("pt-BR");
        assertThat(effectiveContext.get("timezone")).isEqualTo("America/Sao_Paulo");
        assertThat(effectiveContext.get("activeProfileId")).isEqualTo("operator");
        assertThat(effectiveContext.get("activeModuleKey")).isEqualTo("payroll");

        Map<?, ?> activeTenant = (Map<?, ?>) effectiveContext.get("activeTenant");
        assertThat(activeTenant.get("tenantId")).isEqualTo("corporate-holding");
        assertThat(activeTenant.get("label")).isEqualTo("Corporate holding");
        assertThat(activeTenant.get("active")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        Map<String, String> propagationHeaders = (Map<String, String>) response.getBody().get("propagationHeaders");
        assertThat(propagationHeaders)
                .containsEntry("X-Tenant-ID", "corporate-holding")
                .containsEntry("X-Env", "local")
                .containsEntry("X-Praxis-Profile-ID", "operator")
                .containsEntry("X-Praxis-Module-Key", "payroll")
                .containsEntry("X-Timezone", "America/Sao_Paulo");

        @SuppressWarnings("unchecked")
        List<String> capabilities = (List<String>) response.getBody().get("capabilities");
        assertThat(capabilities)
                .contains("runtime.context.switch", "runtime.context.switch.demo-provider");
    }

    @Test
    void shouldHostEnterpriseRuntimeNavigationThroughQuickstartProvider() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Tenant-ID", "tenant-demo");
        headers.add("X-User-ID", "user-demo");
        headers.add("X-Env", "local");
        headers.add("X-Praxis-Module-Key", "payroll");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/praxis/runtime/navigation",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(runtimeContextProvider).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("schemaVersion")).isEqualTo("praxis-enterprise-runtime-navigation.v1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) response.getBody().get("nodes");
        assertThat(nodes).hasSize(1);
        Map<String, Object> payrollModule = nodes.get(0);
        assertThat(payrollModule)
                .containsEntry("id", "payroll")
                .containsEntry("label", "Payroll")
                .containsEntry("type", "module")
                .containsEntry("route", "/payroll")
                .containsEntry("moduleKey", "payroll");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) payrollModule.get("children");
        assertThat(children)
                .extracting(child -> child.get("id"))
                .containsExactly("payroll.folhas-pagamento", "payroll.folhas-pagamento.aprovacoes");

        Map<String, Object> payrollRuns = children.get(0);
        assertThat(payrollRuns)
                .containsEntry("href", "/api/human-resources/folhas-pagamento")
                .containsEntry("route", "/payroll/folhas-pagamento")
                .containsEntry("resourceKey", "human-resources.folhas-pagamento")
                .containsEntry("surfaceRef", "table")
                .containsEntry("capabilityRef", "resource.read");

        Map<String, Object> payrollApprovals = children.get(1);
        assertThat(payrollApprovals)
                .containsEntry("href", "/api/human-resources/folhas-pagamento/actions/approve")
                .containsEntry("route", "/payroll/folhas-pagamento/approvals")
                .containsEntry("surfaceRef", "detail")
                .containsEntry("actionRef", "approve")
                .containsEntry("capabilityRef", "resource.action.approve");

        @SuppressWarnings("unchecked")
        List<String> capabilities = (List<String>) response.getBody().get("capabilities");
        assertThat(capabilities)
                .contains("runtime.navigation.read", "runtime.navigation.demo-provider");
    }
}
