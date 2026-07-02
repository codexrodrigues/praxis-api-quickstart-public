package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.constants.ApiPaths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova downstream de que o host consegue servir o cockpit empacotado pelo metadata starter,
 * sem copiar HTML ou assets para o projeto consumidor.
 */
@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        properties = {
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.read-open=false",
                "app.security.write-disabled=false",
                "app.security.schemas-aggregator.enabled=true",
                "app.security.csrf.disable=true",
                "app.session.cookie-name=SESSION",
                "app.session.secure=false",
                "app.session.samesite=Lax",
                "praxis.stats.enabled=true",
                "praxis.ai.provider=mock",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.domain-catalog.rag-publication.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:quickstart_cockpit_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_cockpit_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
@AutoConfigureMockMvc
class PraxisCockpitStarterConsumptionIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldServeCockpitBundledByMetadataStarter() throws Exception {
        ClassPathResource cockpitIndex = new ClassPathResource("META-INF/resources/praxis/cockpit/index.html");
        if (!cockpitIndex.exists()) {
            throw new AssertionError("Expected praxis-metadata-starter to bundle " + cockpitIndex.getPath());
        }

        mockMvc.perform(get(ApiPaths.Framework.COCKPIT))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(ApiPaths.Framework.COCKPIT_INDEX));

        mockMvc.perform(get(ApiPaths.Framework.COCKPIT_INDEX))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Praxis Cockpit")));
    }
}
