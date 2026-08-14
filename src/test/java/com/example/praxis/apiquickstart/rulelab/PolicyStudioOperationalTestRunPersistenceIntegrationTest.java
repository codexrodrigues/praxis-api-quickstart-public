package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.domain.DomainRuleChangeWorkspace;
import org.praxisplatform.config.domain.DomainRuleTestScenario;
import org.praxisplatform.config.dto.DomainRuleOperationalTestEvidence;
import org.praxisplatform.config.dto.DomainRuleTestRunRecordRequest;
import org.praxisplatform.config.dto.DomainRuleTestRunResultRequest;
import org.praxisplatform.config.repository.DomainRuleChangeWorkspaceRepository;
import org.praxisplatform.config.repository.DomainRuleTestRunRepository;
import org.praxisplatform.config.repository.DomainRuleTestRunResultRepository;
import org.praxisplatform.config.repository.DomainRuleTestScenarioRepository;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleTestRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Round-trip JPA proof that V57 evidence is persisted and read by the Config owner service. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PolicyStudioOperationalTestRunPersistenceIntegrationTest {
    private static final String DIGEST = "A".repeat(64);
    private static final DomainRuleGovernancePrincipal PRINCIPAL =
            new DomainRuleGovernancePrincipal("desenv", "policy-proof-agent", "local");
    private static final EmbeddedPostgres POSTGRES = startPostgres();

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @AfterAll
    static void stopPostgres() throws Exception {
        POSTGRES.close();
    }

    @Autowired private DomainRuleChangeWorkspaceRepository workspaces;
    @Autowired private DomainRuleTestScenarioRepository scenarios;
    @Autowired private DomainRuleTestRunRepository runs;
    @Autowired private DomainRuleTestRunResultRepository results;
    private PolicyStudioOperationalTestRunRecorder recorder;
    private UUID workspaceId;
    private UUID scenarioId;

    @BeforeEach
    void setUp() {
        DomainRuleTestRunService owner = new DomainRuleTestRunService(
                runs, results, workspaces, scenarios, new ObjectMapper());
        recorder = new PolicyStudioOperationalTestRunRecorder(owner);
        workspaceId = UUID.randomUUID();
        scenarioId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-14T12:00:00Z");
        workspaces.saveAndFlush(DomainRuleChangeWorkspace.builder()
                .id(workspaceId).tenantId("desenv").environment("local")
                .ruleKey("grant.amount-parameters").baseDefinitionId(UUID.randomUUID())
                .baseDefinitionVersion(1).baseDefinitionHash(DIGEST).title("Operational V57 proof")
                .status("OPEN").draftCondition("{\"===\":[true,true]}").draftParameters("{}")
                .etag(UUID.randomUUID()).revision(1L).createdBy("policy-proof-agent")
                .updatedBy("policy-proof-agent").createdAt(now).updatedAt(now).build());
        scenarios.saveAndFlush(DomainRuleTestScenario.builder()
                .id(scenarioId).workspaceId(workspaceId).tenantId("desenv").environment("local")
                .scenarioKey("create-allow").name("Create allowed")
                .facts("{\"request\":{\"requestedAmount\":100}}")
                .expectedDecision("ALLOW").expectedReasonCodes("[]").expectedEffectIntents("[]")
                .status("ACTIVE").etag(UUID.randomUUID()).revision(1L)
                .createdBy("policy-proof-agent").updatedBy("policy-proof-agent")
                .createdAt(now).updatedAt(now).build());
    }

    @Test
    void persistsAndReadsOperationalEvidenceThroughTheConfigService() {
        DomainRuleOperationalTestEvidence evidence = new DomainRuleOperationalTestEvidence(
                "CREATE", null, DIGEST, true, false, true, DIGEST, 1);
        DomainRuleTestRunRecordRequest evaluated = new DomainRuleTestRunRecordRequest(
                1L, DIGEST, Instant.parse("2026-08-14T12:00:00Z"), "UTC",
                null, null, 0L, null,
                List.of(new DomainRuleTestRunResultRequest(
                        scenarioId, "create-allow", "ALLOW", "ALLOW", null, null,
                        List.of(), List.of(), List.of(), List.of(), DIGEST, DIGEST, DIGEST)));

        var recorded = recorder.record(workspaceId, evaluated, Map.of(scenarioId, evidence), PRINCIPAL);
        var reloaded = new DomainRuleTestRunService(
                runs, results, workspaces, scenarios, new ObjectMapper()).list(workspaceId, PRINCIPAL);

        assertThat(recorded.results()).singleElement()
                .extracting(item -> item.operationalEvidence()).isEqualTo(evidence);
        assertThat(reloaded).singleElement().satisfies(run -> {
            assertThat(run.runId()).isEqualTo(recorded.runId());
            assertThat(run.results()).singleElement()
                    .extracting(item -> item.operationalEvidence()).isEqualTo(evidence);
        });
        assertThat(runs.count()).isOne();
        assertThat(results.count()).isOne();
    }

    @Configuration
    @EntityScan(basePackages = "org.praxisplatform.config.domain")
    @EnableJpaRepositories(basePackages = "org.praxisplatform.config.repository")
    static class JpaConfig {}

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.builder()
                    .setCleanDataDirectory(true)
                    .setRegisterShutdownHook(false)
                    .start();
        } catch (Exception failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }
}
