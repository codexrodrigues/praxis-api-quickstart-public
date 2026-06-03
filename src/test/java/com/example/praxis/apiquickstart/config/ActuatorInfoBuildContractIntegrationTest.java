package com.example.praxis.apiquickstart.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova operacional minima de que o host publica metadados de build no endpoint
 * publico {@code /actuator/info}, permitindo diagnosticar por HTTP qual artefato
 * realmente chegou ao deploy.
 */
@SpringBootTest(properties = {
        "app.rate-limit.enabled=false",
        "app.security.config-origin-restriction.enabled=false",
        "app.security.read-open=false",
        "app.security.write-disabled=false",
        "app.security.schemas-aggregator.enabled=true",
        "app.security.csrf.disable=true",
        "app.session.cookie-name=SESSION",
        "app.session.secure=false",
        "app.session.samesite=Lax",
        "praxis.ai.provider=mock",
        "spring.ai.embedding.provider=mock",
        "spring.ai.openai.api-key=dummy",
        "praxis.ai.rag.vector-store.enabled=false",
        "praxis.ai.registry.bootstrap.enabled=false",
        "praxis.ai.registry.health.enabled=false",
        "spring.ai.vectorstore.pgvector.initialize-schema=false",
        "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:quickstart_actuator_info_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "config.datasource.url=jdbc:h2:mem:quickstart_actuator_info_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "config.datasource.driver-class-name=org.h2.Driver",
        "config.datasource.username=sa",
        "config.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
class ActuatorInfoBuildContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorInfoShouldExposeBuildVersion() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.build.version", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.build.artifact", is("praxis-api-quickstart")));
    }
}
