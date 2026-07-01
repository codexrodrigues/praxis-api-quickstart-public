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
}
