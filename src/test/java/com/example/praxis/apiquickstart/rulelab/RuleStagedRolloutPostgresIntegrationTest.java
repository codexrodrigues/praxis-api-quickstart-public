package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.dto.DomainRuleRolloutCreateRequest;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleImplementationCatalog;
import org.praxisplatform.config.service.DomainRuleImplementationCatalogFingerprint;
import org.praxisplatform.config.service.DomainRuleImplementationScope;
import org.praxisplatform.config.service.DomainRuleRolloutService;
import org.praxisplatform.config.service.DomainRuleSnapshotService;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.praxisplatform.rules.digest.PraxisCanonicalJson;
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

/** Complete PostgreSQL proof of candidate preload, rediscovery and governed head promotion. */
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
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=false"
        })
class RuleStagedRolloutPostgresIntegrationTest {
    private static final String TENANT = "desenv";
    private static final String ENVIRONMENT = "local";
    private static final String RULE_SET = ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY;
    private static final String ACTIVE_KEY = "extraordinary-grant-v1";
    private static final String CANDIDATE_KEY = "extraordinary-grant-v2";

    static final EmbeddedPostgres OPERATIONAL = startPostgres();
    static final EmbeddedPostgres CONFIG = startConfigPostgres();

    @DynamicPropertySource
    static void databases(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> OPERATIONAL.getJdbcUrl("postgres", "postgres"));
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("config.datasource.url", () -> CONFIG.getJdbcUrl("postgres", "postgres"));
        properties.add("config.datasource.username", () -> "postgres");
        properties.add("config.datasource.password", () -> "");
        properties.add("config.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired @Qualifier("configJdbcTemplate") private JdbcTemplate config;
    @Autowired private DomainRuleRolloutService rollouts;
    @Autowired private DomainRuleSnapshotService snapshots;
    @Autowired private DomainRuleImplementationCatalog implementationCatalog;
    @Autowired private ObjectMapper objectMapper;
    @Autowired @Qualifier("extraordinaryGrantRuleExecutorRegistry")
    private RuleBindingExecutorRegistry registry;
    @MockBean(name = "ragVectorStore") private VectorStore ragVectorStore;

    private UUID activeId;
    private UUID candidateId;
    private UUID headEtag;
    private String candidateHash;

    @BeforeEach
    void seedGovernedCandidateAndRequiredPolicy() throws Exception {
        config.update("delete from domain_rule_candidate_probe");
        config.update("delete from domain_rule_snapshot_rollout_event");
        config.update("delete from domain_rule_snapshot_rollout");
        config.update("delete from domain_rule_rollout_policy_event");
        config.update("delete from domain_rule_rollout_policy_head");
        config.update("delete from domain_rule_rollout_policy");
        config.update("delete from domain_rule_snapshot_event");
        config.update("delete from domain_rule_snapshot_head");
        config.update("delete from domain_rule_snapshot");

        activeId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        headEtag = UUID.randomUUID();
        seedSnapshot(activeId, snapshot(ACTIVE_KEY, 1, null));
        candidateHash = seedSnapshot(candidateId, snapshot(CANDIDATE_KEY, 2, ACTIVE_KEY));
        Instant now = Instant.now();
        config.update("""
                insert into domain_rule_snapshot_head(
                    id, tenant_id, environment, rule_set_key, active_snapshot_id,
                    activation_revision, head_etag, updated_at, row_version)
                values (?, ?, ?, ?, ?, 1, ?, ?, 0)
                """, UUID.randomUUID(), TENANT, ENVIRONMENT, RULE_SET, activeId, headEtag,
                Timestamp.from(now));
        UUID policyId = UUID.randomUUID();
        config.update("""
                insert into domain_rule_rollout_policy(
                    id, tenant_id, environment, rule_set_key, policy_key, policy_version,
                    enforcement_mode, minimum_fresh_probes, minimum_ready_ratio,
                    block_on_incompatible, stale_after_seconds, maximum_rollout_age_seconds,
                    active, status, created_by, created_at, approved_by, approved_at,
                    activated_by, activated_at)
                values (?, ?, ?, ?, 'single-host-required', 1, 'REQUIRED', 1, 1.0000,
                    true, 120, 900, true, 'ACTIVE', 'author', ?, 'reviewer', ?, 'operator', ?)
                """, policyId, TENANT, ENVIRONMENT, RULE_SET,
                Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
    }

    @AfterAll
    static void stopPostgres() throws IOException {
        CONFIG.close();
        OPERATIONAL.close();
    }

    @Test
    void persistsReloadsAndPromotesOnlyAfterTheQuickstartHostPreloadsTheCandidate() {
        var operator = principal("operator");
        var reader = principal("auditor");
        var created = rollouts.create(
                new DomainRuleRolloutCreateRequest(CANDIDATE_KEY, Instant.now().plusSeconds(300)),
                operator, quote(headEtag));

        var beforeProbe = rollouts.catalog(RULE_SET, reader, true).rollouts().getFirst();
        assertThat(beforeProbe.availableActions()).containsExactly("CANCEL");
        assertThat(beforeProbe.readiness().activationReady()).isFalse();

        Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        var preloader = new RuleCandidatePreloader(
                snapshots, rollouts, registry, implementationCatalog, objectMapper, clock,
                TENANT, ENVIRONMENT, "service:quickstart-host-a");
        var preload = preloader.preload(new RuleCandidatePreloadCommand(
                created.rolloutId(), CANDIDATE_KEY, candidateHash));
        assertThat(preload.preloadReady()).isTrue();
        assertThat(preload.probeUpdated()).isTrue();

        var rediscovered = rollouts.catalog(RULE_SET, reader, true).rollouts().getFirst();
        assertThat(rediscovered.readiness().activationReady()).isTrue();
        assertThat(rediscovered.availableActions())
                .containsExactly("CANCEL", "ACTIVATE_CANDIDATE");

        var activation = snapshots.activatePublished(
                CANDIDATE_KEY, "operator", TENANT, ENVIRONMENT,
                quote(headEtag), created.rolloutId());

        assertThat(activation.activationType()).isEqualTo("ACTIVATED");
        assertThat(rollouts.catalog(RULE_SET, reader, true).rollouts()).isEmpty();
        assertThat(config.queryForObject(
                "select active_snapshot_id from domain_rule_snapshot_head where rule_set_key = ?",
                UUID.class, RULE_SET)).isEqualTo(candidateId);
        assertThat(config.queryForObject(
                "select status from domain_rule_snapshot_rollout where id = ?",
                String.class, created.rolloutId())).isEqualTo("ACTIVATED");
        assertThat(config.queryForObject(
                "select count(*) from domain_rule_snapshot_rollout_event where rollout_id = ? and event_type = 'ACTIVATED'",
                Long.class, created.rolloutId())).isEqualTo(1L);
        assertThat(config.queryForObject(
                "select activation_revision from domain_rule_snapshot_head where rule_set_key = ?",
                Long.class, RULE_SET)).isEqualTo(2L);
    }

    private String seedSnapshot(UUID id, PublishedRuleSnapshot snapshot) throws Exception {
        String contentHash = new PraxisRuleSnapshotCompiler(registry)
                .compile(snapshot, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION)
                .snapshotContentHash();
        var scope = new DomainRuleImplementationScope(
                TENANT, ENVIRONMENT, ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY);
        String catalogDigest = DomainRuleImplementationCatalogFingerprint.sha256(
                objectMapper, scope, implementationCatalog.allowedImplementations(scope));
        var manifest = objectMapper.createObjectNode();
        manifest.put("implementationCatalogDigest", catalogDigest);
        String compositionDigest = PraxisCanonicalJson.sha256(manifest);
        PublishedRuleSnapshot stored = withCompositionApprovals(snapshot, compositionDigest);
        contentHash = new PraxisRuleSnapshotCompiler(registry)
                .compile(stored, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION)
                .snapshotContentHash();
        config.update("""
                insert into domain_rule_snapshot(
                    id, tenant_id, environment, snapshot_key, rule_set_key, rule_set_version,
                    publication_revision, snapshot_payload, content_hash, composition_manifest,
                    composition_digest, published_by, published_at)
                values (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::jsonb, ?, 'release-manager', ?)
                """, id, TENANT, ENVIRONMENT, stored.snapshotKey(), RULE_SET,
                stored.ruleSet().ref().version(), stored.publicationRevision(),
                objectMapper.writeValueAsString(stored), contentHash,
                objectMapper.writeValueAsString(manifest), compositionDigest,
                Timestamp.from(Instant.now()));
        return contentHash;
    }

    private static PublishedRuleSnapshot withCompositionApprovals(
            PublishedRuleSnapshot source, String compositionDigest) {
        var approvals = List.of(
                new RuleSnapshotApproval("composition-a", "RULE_COMPOSITION_APPROVER", "reviewer-a",
                        Instant.now().minusSeconds(60).toString(), compositionDigest),
                new RuleSnapshotApproval("composition-b", "RULE_COMPOSITION_APPROVER", "reviewer-b",
                        Instant.now().minusSeconds(30).toString(), compositionDigest));
        return new PublishedRuleSnapshot(
                source.snapshotContractVersion(), source.snapshotKey(), source.tenantId(),
                source.environment(), source.ownerServiceKey(), source.publicationRevision(),
                source.publishedAtUtc(), source.supersedesSnapshotKey(),
                source.requiredHostContractVersion(), source.validFromUtc(), source.validUntilUtc(),
                source.sources(), approvals, source.ruleSet());
    }

    private static PublishedRuleSnapshot snapshot(String key, int revision, String supersedes) {
        Instant now = Instant.now();
        return new PublishedRuleSnapshot(
                PublishedRuleSnapshot.SNAPSHOT_CONTRACT_VERSION, key, TENANT, ENVIRONMENT,
                ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY, revision,
                now.minusSeconds(120).toString(), supersedes,
                ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION,
                now.minusSeconds(3600).toString(), now.plusSeconds(3600).toString(),
                List.of(new RuleSnapshotSource(
                        "definition-" + revision, "grant:eligibility", revision, "A".repeat(64))),
                List.of(new RuleSnapshotApproval(
                        "definition-approval-" + revision, "RULE_DEFINITION_APPROVER", "reviewer",
                        now.minusSeconds(180).toString(), "B".repeat(64))),
                ExtraordinaryGrantRuleSetFactory.definition(revision));
    }

    private static DomainRuleGovernancePrincipal principal(String actor) {
        return new DomainRuleGovernancePrincipal(TENANT, actor, ENVIRONMENT);
    }

    private static String quote(UUID etag) {
        return '"' + etag.toString() + '"';
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.builder()
                    .setCleanDataDirectory(true)
                    .setRegisterShutdownHook(false)
                    .start();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static EmbeddedPostgres startConfigPostgres() {
        EmbeddedPostgres postgres = startPostgres();
        try (Connection connection = postgres.getPostgresDatabase().getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(readResource("/rule-lab/staged-rollout-postgres-schema.sql"));
            statement.execute(readResource("/db/migration/V53__create_domain_rule_staged_rollout.sql"));
            statement.execute(readResource("/db/migration/V54__govern_domain_rule_rollout_policy.sql"));
            return postgres;
        } catch (Exception exception) {
            try {
                postgres.close();
            } catch (IOException ignored) {
                // Preserve the schema preparation failure.
            }
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = RuleStagedRolloutPostgresIntegrationTest.class
                .getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Missing test migration resource " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
