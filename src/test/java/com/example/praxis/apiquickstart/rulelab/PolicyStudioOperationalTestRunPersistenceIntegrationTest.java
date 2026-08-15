package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.domain.DomainRuleChangeWorkspace;
import org.praxisplatform.config.domain.DomainRuleTestScenario;
import org.praxisplatform.config.contract.DomainRuleOperationalTestEvidence;
import org.praxisplatform.config.contract.DomainRuleTestBaselineEvidence;
import org.praxisplatform.config.contract.DomainRuleTestBaselineResult;
import org.praxisplatform.config.contract.DomainRuleTestRunRecordRequest;
import org.praxisplatform.config.contract.DomainRuleTestRunResultRequest;
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

/** Round-trip PostgreSQL proof for the idempotent four-result V58 operational Test Run. */
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
    private Map<String, UUID> scenarioIds;

    @BeforeEach
    void setUp() {
        DomainRuleTestRunService owner = new DomainRuleTestRunService(
                runs, results, workspaces, scenarios, new ObjectMapper().findAndRegisterModules());
        recorder = new PolicyStudioOperationalTestRunRecorder(owner);
        workspaceId = UUID.randomUUID();
        scenarioIds = new LinkedHashMap<>();
        scenarioIds.put("create-allow", UUID.randomUUID());
        scenarioIds.put("create-deny", UUID.randomUUID());
        scenarioIds.put("update-allow", UUID.randomUUID());
        scenarioIds.put("update-deny", UUID.randomUUID());
        Instant now = Instant.parse("2026-08-14T12:00:00Z");
        workspaces.saveAndFlush(DomainRuleChangeWorkspace.builder()
                .id(workspaceId).tenantId("desenv").environment("local")
                .ruleKey("grant.amount-parameters").baseDefinitionId(UUID.randomUUID())
                .baseDefinitionVersion(1).baseDefinitionHash(DIGEST).title("Operational V58 proof")
                .status("OPEN").draftCondition("{\"===\":[true,true]}").draftParameters("{}")
                .etag(UUID.randomUUID()).revision(1L).createdBy("policy-proof-agent")
                .updatedBy("policy-proof-agent").createdAt(now).updatedAt(now).build());
        scenarioIds.forEach((key, id) -> scenarios.saveAndFlush(DomainRuleTestScenario.builder()
                .id(id).workspaceId(workspaceId).tenantId("desenv").environment("local")
                .scenarioKey(key).name(key)
                .facts("{\"request\":{\"case\":\"" + key + "\"}}")
                .expectedDecision(key.endsWith("allow") ? "ALLOW" : "DENY")
                .expectedReasonCodes("[]").expectedEffectIntents("[]")
                .status("ACTIVE").etag(UUID.randomUUID()).revision(1L)
                .createdBy("policy-proof-agent").updatedBy("policy-proof-agent")
                .createdAt(now).updatedAt(now).build()));
    }

    @Test
    void persistsOneFourResultRunAndReplaysTheSameCommandWithoutDuplication() {
        Map<UUID, DomainRuleOperationalTestEvidence> evidence = new LinkedHashMap<>();
        evidence.put(scenarioIds.get("create-allow"), operational("CREATE", true, "B".repeat(64)));
        evidence.put(scenarioIds.get("create-deny"), operational("CREATE", false, null));
        evidence.put(scenarioIds.get("update-allow"), operational("UPDATE", true, "C".repeat(64)));
        evidence.put(scenarioIds.get("update-deny"), operational("UPDATE", false, DIGEST));
        Instant evaluatedAt = Instant.parse("2026-08-14T12:00:00Z");
        DomainRuleTestRunRecordRequest evaluated = new DomainRuleTestRunRecordRequest(
                "quickstart:v58:create-update-allow-deny", 1L, DIGEST, evaluatedAt, "UTC",
                null, null, 0L,
                new DomainRuleTestBaselineEvidence(
                        "SYNTHETIC_EXPECTED", "quickstart:v58:four-result", DIGEST,
                        evaluatedAt, "ELIGIBLE"),
                scenarioIds.entrySet().stream().map(entry -> {
                    String decision = entry.getKey().endsWith("allow") ? "ALLOW" : "DENY";
                    return new DomainRuleTestRunResultRequest(
                            entry.getValue(), entry.getKey(), decision, decision, null, null,
                            List.of(), List.of(), List.of(), List.of(), DIGEST, DIGEST, DIGEST,
                            new DomainRuleTestBaselineResult(
                                    decision, null, List.of(), List.of(), DIGEST, null, null), null);
                }).toList());

        var recorded = recorder.record(workspaceId, evaluated, evidence, PRINCIPAL);
        var replay = recorder.record(workspaceId, evaluated, evidence, PRINCIPAL);
        var reloaded = new DomainRuleTestRunService(
                runs, results, workspaces, scenarios, new ObjectMapper().findAndRegisterModules())
                .list(workspaceId, PRINCIPAL);

        assertThat(recorded.runId()).isEqualTo(replay.runId());
        assertThat(recorded.results()).hasSize(4)
                .allSatisfy(item -> {
                    assertThat(item.operationalEvidence().cleanupVerified()).isTrue();
                    assertThat(item.baselineResult()).isNotNull();
                    assertThat(item.candidateBaselineComparison()).isEqualTo("MATCH");
                });
        assertThat(reloaded).singleElement().satisfies(run -> {
            assertThat(run.runId()).isEqualTo(recorded.runId());
            assertThat(run.idempotencyKey()).isEqualTo("quickstart:v58:create-update-allow-deny");
            assertThat(run.results()).hasSize(4);
        });
        assertThat(runs.count()).isOne();
        assertThat(results.count()).isEqualTo(4);
    }

    private DomainRuleOperationalTestEvidence operational(
            String operation, boolean mutation, String afterStateDigest) {
        return new DomainRuleOperationalTestEvidence(
                operation, "UPDATE".equals(operation) ? DIGEST : null, afterStateDigest,
                mutation, !mutation, true, DIGEST, 0);
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
