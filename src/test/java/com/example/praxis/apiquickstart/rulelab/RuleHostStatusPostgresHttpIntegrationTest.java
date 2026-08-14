package com.example.praxis.apiquickstart.rulelab;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.security.JwtTokenService;
import com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.dto.DomainRuleSnapshotStoredResponse;
import org.praxisplatform.config.service.DomainRuleSnapshotService;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleRuntimeCompatibility;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL/HTTP drill for governed host readiness.
 *
 * <p>The request attributes model tenant/environment claims installed by a trusted authentication
 * adapter. They cannot be supplied as HTTP headers by the caller in corporate mode.</p>
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = ApiQuickstartApplication.class,
        properties = {
                "spring.main.banner-mode=off",
                "app.rate-limit.enabled=false",
                "app.security.config-origin-restriction.enabled=false",
                "app.security.csrf.disable=true",
                "app.security.read-open=true",
                "praxis.ai.security.corporate-mode=true",
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
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=true",
                "spring.flyway.baseline-version=0"
        })
@AutoConfigureMockMvc(addFilters = true)
class RuleHostStatusPostgresHttpIntegrationTest {
    private static final String RULE_SET = ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY;
    private static final String SNAPSHOT = "extraordinary-grant-v1";
    private static final String HASH = "A".repeat(64);
    private static final String CATALOG_DIGEST = "B".repeat(64);
    private static final String POST_STATUS = "/api/praxis/config/domain-rules/snapshots/host-status";
    private static final String GET_SUMMARY =
            "/api/praxis/config/domain-rules/snapshots/head/host-status-summary?ruleSetKey=" + RULE_SET;

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
        properties.add("config.datasource.url", CONFIG::getJdbcUrl);
        properties.add("config.datasource.username", CONFIG::getUsername);
        properties.add("config.datasource.password", CONFIG::getPassword);
        properties.add("config.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired @Qualifier("configJdbcTemplate") private JdbcTemplate config;
    @MockBean private DomainRuleSnapshotService snapshotService;
    @MockBean private JwtTokenService jwtTokenService;
    @MockBean(name = "ragVectorStore") private VectorStore ragVectorStore;

    @BeforeEach
    void seedHeads() {
        config.update("delete from domain_rule_host_status");
        config.update("delete from domain_rule_snapshot_event");
        config.update("delete from domain_rule_snapshot_head");
        config.update("delete from domain_rule_snapshot");
        seedHead("tenant-a", "dev", UUID.randomUUID());
        seedHead("tenant-b", "dev", UUID.randomUUID());
        when(snapshotService.findSnapshot("tenant-a", "dev", SNAPSHOT))
                .thenReturn(Optional.of(new DomainRuleSnapshotStoredResponse(snapshot("tenant-a"), HASH)));
        when(snapshotService.findSnapshot("tenant-b", "dev", SNAPSHOT))
                .thenReturn(Optional.of(new DomainRuleSnapshotStoredResponse(snapshot("tenant-b"), HASH)));
        for (String actor : List.of("service:host-a", "service:host-b", "service:host-c")) {
            when(jwtTokenService.validate(actor)).thenReturn(JwtTokenService.JwtValidationResult.valid(
                    actor, "SERVICE", List.of(RuleGovernanceAuthorities.EXECUTION_OBSERVER)));
        }
        when(jwtTokenService.validate("auditor:tenant-a"))
                .thenReturn(JwtTokenService.JwtValidationResult.valid(
                        "auditor:tenant-a", "HUMAN", List.of(RuleGovernanceAuthorities.SNAPSHOT_READER)));
        when(jwtTokenService.validate("auditor:tenant-b"))
                .thenReturn(JwtTokenService.JwtValidationResult.valid(
                        "auditor:tenant-b", "HUMAN", List.of(RuleGovernanceAuthorities.SNAPSHOT_READER)));
    }

    @Test
    void provesIdentityOrderingIsolationAndIncompatibleToAlignedRecovery() throws Exception {
        Instant now = Instant.now();
        publish("service:host-a", "tenant-a", incompatible(now));
        publish("service:host-b", "tenant-a", unavailable(now.minusSeconds(1)));
        publish("service:host-c", "tenant-a", aligned(now.minusSeconds(300)));
        publish("service:host-b", "tenant-b", aligned(now));

        summary("tenant-a")
                .andExpect(jsonPath("$.totalHosts").value(3))
                .andExpect(jsonPath("$.incompatibleHosts").value(1))
                .andExpect(jsonPath("$.unavailableHosts").value(1))
                .andExpect(jsonPath("$.staleHosts").value(1))
                .andExpect(jsonPath("$.alignedHosts").value(0));
        summary("tenant-b")
                .andExpect(jsonPath("$.totalHosts").value(1))
                .andExpect(jsonPath("$.alignedHosts").value(1));

        publish("service:host-a", "tenant-a", aligned(now.minusSeconds(30)))
                .andExpect(jsonPath("$.updated").value(false));
        summary("tenant-a").andExpect(jsonPath("$.incompatibleHosts").value(1));

        publish("service:host-a", "tenant-a", aligned(now.plusSeconds(1)))
                .andExpect(jsonPath("$.updated").value(true));
        summary("tenant-a")
                .andExpect(jsonPath("$.incompatibleHosts").value(0))
                .andExpect(jsonPath("$.alignedHosts").value(1));
    }

    private org.springframework.test.web.servlet.ResultActions publish(
            String actor, String tenant, String payload) throws Exception {
        return mockMvc.perform(post(POST_STATUS)
                        .header("Authorization", "Bearer " + actor)
                        .requestAttr("tenantId", tenant)
                        .requestAttr("environment", "dev")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());
    }

    private org.springframework.test.web.servlet.ResultActions summary(String tenant) throws Exception {
        return mockMvc.perform(get(GET_SUMMARY)
                        .header("Authorization", "Bearer auditor:" + tenant)
                        .requestAttr("tenantId", tenant)
                        .requestAttr("environment", "dev"))
                .andExpect(status().isOk());
    }

    private String aligned(Instant observedAt) throws Exception {
        RuleRuntimeCompatibility runtime = RuleRuntimeCompatibility.current();
        return payload(true, runtime.engineContractVersion(), observedAt, null);
    }

    private String incompatible(Instant observedAt) throws Exception {
        return payload(true, "incompatible-engine", observedAt, null);
    }

    private String unavailable(Instant observedAt) throws Exception {
        return objectMapper.writeValueAsString(new java.util.LinkedHashMap<>(java.util.Map.of(
                "ruleSetKey", RULE_SET,
                "ready", false,
                "hostContractVersion", ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION,
                "failureCode", "SNAPSHOT_UNAVAILABLE",
                "observedAtUtc", observedAt.toString())));
    }

    private String payload(boolean ready, String engineVersion, Instant observedAt, String failure)
            throws Exception {
        RuleRuntimeCompatibility runtime = RuleRuntimeCompatibility.current();
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("ruleSetKey", RULE_SET);
        body.put("loadedSnapshotKey", SNAPSHOT);
        body.put("loadedSnapshotContentHash", HASH);
        body.put("activationRevision", 1);
        body.put("ready", ready);
        body.put("hostContractVersion", ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION);
        body.put("engineContractVersion", engineVersion);
        body.put("jsonLogicDialectVersion", runtime.jsonLogicDialectVersion());
        body.put("jsonLogicCorpusSha256", runtime.jsonLogicCorpusSha256());
        body.put("implementationCatalogDigest", CATALOG_DIGEST);
        if (failure != null) body.put("failureCode", failure);
        body.put("observedAtUtc", observedAt.toString());
        return objectMapper.writeValueAsString(body);
    }

    private void seedHead(String tenant, String environment, UUID snapshotId) {
        Instant publishedAt = Instant.now().minusSeconds(60);
        config.update("""
                insert into domain_rule_snapshot(
                    id, tenant_id, environment, snapshot_key, rule_set_key, rule_set_version,
                    publication_revision, snapshot_payload, content_hash, composition_manifest,
                    composition_digest, published_by, published_at)
                values (?, ?, ?, ?, ?, 1, 1, '{}'::jsonb, ?, ?::jsonb, ?, 'drill', ?)
                """, snapshotId, tenant, environment, SNAPSHOT, RULE_SET, HASH,
                "{\"implementationCatalogDigest\":\"" + CATALOG_DIGEST + "\"}",
                "C".repeat(64), Timestamp.from(publishedAt));
        config.update("""
                insert into domain_rule_snapshot_head(
                    id, tenant_id, environment, rule_set_key, active_snapshot_id,
                    activation_revision, head_etag, updated_at, row_version)
                values (?, ?, ?, ?, ?, 1, ?, ?, 0)
                """, UUID.randomUUID(), tenant, environment, RULE_SET, snapshotId,
                UUID.randomUUID(), Timestamp.from(publishedAt));
    }

    private static PublishedRuleSnapshot snapshot(String tenant) {
        Instant now = Instant.now();
        return new PublishedRuleSnapshot(
                PublishedRuleSnapshot.SNAPSHOT_CONTRACT_VERSION, SNAPSHOT, tenant, "dev",
                ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY, 1, now.toString(), null,
                ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION, now.toString(), null,
                List.of(new RuleSnapshotSource("definition-1", "grant:eligibility", 1, "D".repeat(64))),
                List.of(new RuleSnapshotApproval(
                        "approval-1", "RULE_DEFINITION_APPROVER", "reviewer", now.toString(),
                        "E".repeat(64))),
                ExtraordinaryGrantRuleSetFactory.definition());
    }
}
