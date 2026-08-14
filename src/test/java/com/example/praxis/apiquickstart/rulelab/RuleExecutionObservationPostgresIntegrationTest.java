package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHead;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHeadActivationType;
import org.praxisplatform.config.service.DomainRuleExecutionObservationService;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL proof of evaluation -> durable outbox -> Config recovery -> idempotent summary.
 * Two databases make control-plane unavailability independent from the operational decision path.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        properties = {
                "spring.main.banner-mode=off",
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.csrf.disable=true",
                "praxis.ai.provider=mock",
                "spring.ai.embedding.provider=mock",
                "spring.ai.openai.api-key=dummy",
                "praxis.ai.rag.vector-store.enabled=false",
                "praxis.ai.registry.bootstrap.enabled=false",
                "praxis.ai.registry.health.enabled=false",
                "spring.ai.vectorstore.pgvector.initialize-schema=false",
                "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
                "praxis.rule-lab.snapshot.enabled=false",
                "praxis.rule-lab.policy-studio.seed.enabled=false",
                "praxis.rule-lab.execution-observations.maximum-attempts=3",
                "praxis.rule-lab.execution-observations.lease-ms=1000",
                "praxis.rule-lab.execution-observations.retry-ms=1",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=true",
                "spring.flyway.baseline-version=0"
        })
class RuleExecutionObservationPostgresIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

    @Container
    static final PostgreSQLContainer<?> OPERATIONAL = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final PostgreSQLContainer<?> CONFIG = new PostgreSQLContainer<>(DockerImageName
            .parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void databases(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", OPERATIONAL::getJdbcUrl);
        properties.add("spring.datasource.username", OPERATIONAL::getUsername);
        properties.add("spring.datasource.password", OPERATIONAL::getPassword);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("config.datasource.url", () -> CONFIG.getJdbcUrl()
                + (CONFIG.getJdbcUrl().contains("?") ? "&" : "?")
                + "socketTimeout=1&connectTimeout=1&tcpKeepAlive=true");
        properties.add("config.datasource.username", CONFIG::getUsername);
        properties.add("config.datasource.password", CONFIG::getPassword);
        properties.add("config.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("config.datasource.hikari.connection-timeout", () -> "500");
        properties.add("config.datasource.hikari.validation-timeout", () -> "250");
    }

    @Autowired @Qualifier("apiJdbcTemplate") private JdbcTemplate operational;
    @Autowired @Qualifier("configJdbcTemplate") private JdbcTemplate config;
    @Autowired private ExtraordinaryGrantRuleSnapshotRuntime runtime;
    @Autowired private RuleExecutionObservationDispatcher dispatcher;
    @Autowired private DomainRuleExecutionObservationService observationService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired @Qualifier("extraordinaryGrantRuleExecutorRegistry") private RuleBindingExecutorRegistry registry;
    @MockBean(name = "ragVectorStore") private VectorStore ragVectorStore;

    @BeforeEach
    void migrateAndSeed() throws Exception {
        operational.execute("drop table if exists rule_execution_observation_outbox cascade");
        operational.execute(Files.readString(Path.of(
                "db/operational-migrations/V20260813_001__rule_execution_observation_outbox.sql")));
        config.update("delete from domain_rule_execution_observation");
        config.update("delete from domain_rule_snapshot_event");
        config.update("delete from domain_rule_snapshot_head");
        config.update("delete from domain_rule_snapshot");
        activateAndSeedCanonicalSnapshot();
    }

    @Test
    void survivesConfigOutageThenDeliversExactlyOnceAndBuildsCanonicalSummary() throws Exception {
        var result = runtime.evaluateWithSnapshot(objectMapper.readTree("""
                {
                  "request":{"requestedAmount":2500.00},
                  "actor":{"permissions":["benefit:request"]},
                  "worker":{"status":"ACTIVE"},
                  "grant":{"hasDuplicate":false},
                  "program":{"active":true,"maxAmount":5000.00},
                  "customer":{"additionalEligible":true},
                  "payment":{"requestedDate":"2026-07-20","allowedDates":["2026-07-20"]},
                  "budget":{"availableAmount":100000.00}
                }
                """), NOW, ZoneId.of("America/Sao_Paulo"));
        assertThat(result.result().decision().name()).isEqualTo("ALLOW");
        assertThat(outboxCount()).isEqualTo(1);

        CONFIG.getDockerClient().pauseContainerCmd(CONFIG.getContainerId()).exec();
        try {
            assertThat(dispatcher.dispatchNext()).isTrue();
        } finally {
            CONFIG.getDockerClient().unpauseContainerCmd(CONFIG.getContainerId()).exec();
        }
        assertThat(outboxStatus()).isEqualTo("PENDING");
        Thread.sleep(10);

        assertThat(dispatcher.dispatchNext()).isTrue();
        assertThat(outboxStatus()).isEqualTo("DELIVERED");
        assertThat(dispatcher.dispatchNext()).isFalse();

        var principal = new DomainRuleGovernancePrincipal(
                "desenv", "service:praxis-api-quickstart", "local");
        var summary = observationService.summary("extraordinary-grant-v1", principal);
        assertThat(summary.totalObservations()).isEqualTo(1);
        assertThat(summary.outcomeCounts()).containsEntry("ALLOW", 1L);
        assertThat(config.queryForObject(
                "select count(*) from domain_rule_execution_observation", Long.class)).isEqualTo(1L);
    }

    private void activateAndSeedCanonicalSnapshot() {
        PublishedRuleSnapshot snapshot = snapshot();
        String contentHash = new PraxisRuleSnapshotCompiler(registry)
                .compile(snapshot, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION)
                .snapshotContentHash();
        UUID snapshotId = UUID.randomUUID();
        config.update("""
                insert into domain_rule_snapshot(
                    id, tenant_id, environment, snapshot_key, rule_set_key, rule_set_version,
                    publication_revision, snapshot_payload, content_hash, composition_manifest,
                    composition_digest, published_by, published_at)
                values (?, 'desenv', 'local', ?, ?, 1, 1, '{}'::jsonb, ?, '{}'::jsonb, ?, 'drill', ?)
                """, snapshotId, snapshot.snapshotKey(), ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY,
                contentHash, "C".repeat(64), Timestamp.from(NOW.minusSeconds(60)));
        config.update("""
                insert into domain_rule_snapshot_event(
                    id, tenant_id, environment, rule_set_key, event_type, from_snapshot_id,
                    to_snapshot_id, activation_revision, head_etag, actor, created_at)
                values (?, 'desenv', 'local', ?, 'ACTIVATED', null, ?, 1, ?, 'drill', ?)
                """, UUID.randomUUID(), ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY,
                snapshotId, UUID.randomUUID(), Timestamp.from(NOW.minusSeconds(30)));
        runtime.activate(new PublishedRuleSnapshotHead(
                        snapshot, contentHash, "postgres-drill-head", 1,
                        PublishedRuleSnapshotHeadActivationType.ACTIVE),
                "desenv", "local", NOW);
    }

    private long outboxCount() {
        return operational.queryForObject(
                "select count(*) from rule_execution_observation_outbox", Long.class);
    }

    private String outboxStatus() {
        return operational.queryForObject(
                "select delivery_status from rule_execution_observation_outbox", String.class);
    }

    private static PublishedRuleSnapshot snapshot() {
        return new PublishedRuleSnapshot(
                PublishedRuleSnapshot.SNAPSHOT_CONTRACT_VERSION, "extraordinary-grant-v1", "desenv", "local",
                ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY, 1, NOW.minusSeconds(120).toString(), null,
                ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION, NOW.minusSeconds(3600).toString(), null,
                List.of(new RuleSnapshotSource("definition-1", "grant:eligibility", 1, "A".repeat(64))),
                List.of(
                        new RuleSnapshotApproval("approval-1", "RULE_DEFINITION_APPROVER", "approver-a",
                                NOW.minusSeconds(300).toString(), "A".repeat(64)),
                        new RuleSnapshotApproval("approval-2", "RULE_DEFINITION_APPROVER", "approver-b",
                                NOW.minusSeconds(240).toString(), "B".repeat(64))),
                ExtraordinaryGrantRuleSetFactory.definition());
    }
}
