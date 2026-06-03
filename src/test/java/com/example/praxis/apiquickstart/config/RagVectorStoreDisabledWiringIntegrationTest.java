package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.rag.RagChatAdvisorProperties;
import org.praxisplatform.config.rag.RagChatAdvisorService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.read-open=false",
                "app.security.write-disabled=false",
                "app.security.schemas-aggregator.enabled=true",
                "app.security.csrf.disable=true",
                "praxis.stats.enabled=true",
                "praxis.ai.provider=mock",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.domain-catalog.rag-publication.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "praxis.domain-knowledge.projection.enabled=true",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:quickstart_rag_disabled_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_rag_disabled_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class RagVectorStoreDisabledWiringIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldNotCreateVectorStoreWhenRagVectorStoreIsDisabled() {
        assertThat(applicationContext.getBeansOfType(VectorStore.class)).isEmpty();
        assertThat(applicationContext.getBean(RagChatAdvisorProperties.class)).isNotNull();
        assertThat(applicationContext.getBean(RagChatAdvisorService.class)).isNotNull();
    }
}
