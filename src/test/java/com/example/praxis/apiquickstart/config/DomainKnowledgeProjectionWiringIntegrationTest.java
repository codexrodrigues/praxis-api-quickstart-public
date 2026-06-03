package com.example.praxis.apiquickstart.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.repository.DomainKnowledgeAliasRepository;
import org.praxisplatform.config.repository.DomainKnowledgeBindingRepository;
import org.praxisplatform.config.repository.DomainKnowledgeConceptRepository;
import org.praxisplatform.config.repository.DomainKnowledgeEvidenceRepository;
import org.praxisplatform.config.repository.DomainKnowledgeRelationshipRepository;
import org.praxisplatform.config.service.DomainKnowledgeProjectionService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

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
                "spring.datasource.url=jdbc:h2:mem:quickstart_domain_knowledge_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "config.datasource.url=jdbc:h2:mem:quickstart_domain_knowledge_config;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "config.datasource.driver-class-name=org.h2.Driver",
                "config.datasource.username=sa",
                "config.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
class DomainKnowledgeProjectionWiringIntegrationTest {

    @Autowired
    private DomainKnowledgeProjectionService projectionService;

    @Autowired
    private DomainKnowledgeConceptRepository conceptRepository;

    @Autowired
    private DomainKnowledgeAliasRepository aliasRepository;

    @Autowired
    private DomainKnowledgeBindingRepository bindingRepository;

    @Autowired
    private DomainKnowledgeRelationshipRepository relationshipRepository;

    @Autowired
    private DomainKnowledgeEvidenceRepository evidenceRepository;

    @MockBean(name = "ragVectorStore")
    private VectorStore ragVectorStore;

    @Test
    void shouldWireDomainKnowledgeProjectionWhenEnabled() {
        assertNotNull(projectionService);
        assertNotNull(conceptRepository);
        assertNotNull(aliasRepository);
        assertNotNull(bindingRepository);
        assertNotNull(relationshipRepository);
        assertNotNull(evidenceRepository);
    }
}
