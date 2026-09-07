package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Opt-in browser proof against the real Quickstart security chain and PostgreSQL rule store.
 * Only unrelated AI vector operations are mocked. Login, signed cookies, roles, CORS, Origin,
 * rule approvals, publication and palette reads execute their production implementations.
 * Config uses a documented focal migration set, not the entire pgvector database upgrade.
 */
@EnabledIfSystemProperty(named = "praxis.palette.browser", matches = "true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = ApiQuickstartApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
      "server.port=8088", "server.address=127.0.0.1",
      "app.cors.allowed-origins=http://127.0.0.1:4014",
      "app.security.config-origin-restriction.enabled=true",
      "app.security.config-origin-restriction.allowed-origins=http://127.0.0.1:4014",
      "app.security.csrf.disable=false", "app.security.write-disabled=false",
      "app.session.cookie-name=PALETTE_PROOF_SESSION", "app.session.secure=false",
      "app.auth.governance-lab.enabled=true",
      "praxis.ai.security.corporate-mode=true",
      "praxis.ai.security.allow-default-tenant-in-corporate=true",
      "praxis.ai.security.server-default-tenant=desenv",
      "praxis.ai.security.server-default-environment=local",
      "praxis.ai.provider=mock", "spring.ai.embedding.provider=mock",
      "spring.ai.openai.api-key=dummy",
      "praxis.ai.rag.vector-store.enabled=false",
      "praxis.ai.registry.bootstrap.enabled=false", "praxis.ai.registry.health.enabled=false",
      "spring.ai.vectorstore.pgvector.initialize-schema=false",
      "spring.ai.vectorstore.pgvector.vector-table-validations-enabled=false",
      "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
      "config.jpa.hibernate.ddl-auto=none"
    })
class GovernedColorPaletteBrowserPostgresTest {
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String JWT_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    private static EmbeddedPostgres postgres;
    private static Path migrations;

    @MockBean(name = "ragVectorStore") VectorStore unusedVectorStore;

    @DynamicPropertySource
    static void databaseAndIdentities(DynamicPropertyRegistry properties) throws Exception {
        postgres = EmbeddedPostgres.builder().setCleanDataDirectory(true)
                .setRegisterShutdownHook(true).start();
        migrations = Files.createTempDirectory("praxis-palette-browser-migrations-");
        try (var connection = postgres.getPostgresDatabase().getConnection();
                var statement = connection.createStatement()) {
            statement.execute("create table domain_catalog_release (id uuid primary key)");
            statement.execute("create table domain_knowledge_change_set (id uuid primary key)");
            statement.execute("create table domain_knowledge_evidence (subject_type varchar(64))");
        }
        var resolver = new PathMatchingResourcePatternResolver();
        for (int version : List.of(5, 7, 9, 20, 22, 23, 24, 25, 36, 37, 41, 45, 46, 47, 48, 49, 55, 58, 59, 61, 62)) {
            var resources = resolver.getResources("classpath*:db/migration/V" + version + "__*.sql");
            assertThat(resources).hasSize(1);
            try (var input = resources[0].getInputStream()) {
                Files.copy(input, migrations.resolve(resources[0].getFilename()));
            }
        }
        var flyway = Flyway.configure().dataSource(postgres.getPostgresDatabase())
                .locations("filesystem:" + migrations).baselineOnMigrate(true)
                .baselineVersion("4").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(21);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        properties.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("config.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        properties.add("config.datasource.username", () -> "postgres");
        properties.add("config.datasource.password", () -> "");
        properties.add("config.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("app.jwt.secret", () -> JWT_SECRET);
        for (String actor : List.of("author", "approver-a", "approver-b", "publisher", "operator", "auditor")) {
            properties.add("app.auth.governance-lab." + actor + ".username", () -> "palette-" + actor);
            properties.add("app.auth.governance-lab." + actor + ".password", () -> PASSWORD);
        }
    }

    @Test
    void consumesRealPublicationThroughBrowserAndPersistsDistinctApprovals() throws Exception {
        String uiRoot = System.getProperty("praxis.palette.ui-root");
        assertThat(uiRoot).as("Explicit Color Lab worktree required").isNotBlank();
        Process ui = startUi(Path.of(uiRoot));
        var runner = new ProcessBuilder("node", "node_modules/@playwright/test/cli.js", "test",
                "src/app/features/color-components-lab/color-components-lab-real-backend.playwright.spec.ts",
                "--workers=1", "--retries=0", "--output=test-results/palette-http")
                .directory(Path.of(uiRoot).toFile()).redirectErrorStream(true)
                .redirectOutput(Path.of("target/palette-browser.log").toFile());
        runner.environment().put("PRAXIS_PALETTE_BROWSER_PROOF", "true");
        runner.environment().put("PRAXIS_GOVERNANCE_LAB_PASSWORD", PASSWORD);
        runner.environment().put("PLAYWRIGHT_BASE_URL", "http://127.0.0.1:4014");
        runner.environment().put("PRAXIS_QUICKSTART_URL", "http://127.0.0.1:8088");
        Process browser = null;
        try {
            browser = runner.start();
            assertThat(browser.waitFor(Duration.ofMinutes(3).toSeconds(), TimeUnit.SECONDS)).isTrue();
            assertThat(browser.exitValue()).as("See target/palette-browser.log").isZero();
        } finally {
            if (browser != null && browser.isAlive()) {
                browser.descendants().forEach(ProcessHandle::destroy);
                browser.destroyForcibly();
            }
            stop(ui);
        }
        try (var connection = postgres.getPostgresDatabase().getConnection();
                var statement = connection.createStatement();
                var rows = statement.executeQuery("""
                    select count(*), count(distinct definition_id), count(distinct actor_ref)
                    from domain_rule_definition_approval
                    """)) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isEqualTo(2);
            assertThat(rows.getInt(2)).isEqualTo(2);
            assertThat(rows.getInt(3)).isEqualTo(1);
        }
    }

    private static Process startUi(Path uiRoot) throws Exception {
        var process = new ProcessBuilder(
                "node", "node_modules/@angular/cli/bin/ng.js", "serve", "praxis-ui-workspace",
                "--host", "127.0.0.1", "--port", "4014", "--proxy-config", "proxy.conf.js")
                .directory(uiRoot.toFile())
                .redirectErrorStream(true)
                .redirectOutput(Path.of("target/palette-ui.log").toFile());
        process.environment().put("PAX_PROXY_TARGET", "http://127.0.0.1:8088");
        Process ui = process.start();
        long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
        while (System.nanoTime() < deadline) {
            if (!ui.isAlive()) {
                throw new IllegalStateException("Color Lab host exited before readiness; see target/palette-ui.log");
            }
            try {
                var connection = (HttpURLConnection) URI.create("http://127.0.0.1:4014/color-lab")
                        .toURL().openConnection();
                connection.setConnectTimeout(500);
                connection.setReadTimeout(500);
                if (connection.getResponseCode() == 200) return ui;
            } catch (Exception ignored) {
                // The managed Angular compiler is still starting.
            }
            Thread.sleep(250);
        }
        stop(ui);
        throw new IllegalStateException("Color Lab host did not become ready; see target/palette-ui.log");
    }

    private static void stop(Process process) {
        if (process == null || !process.isAlive()) return;
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (postgres != null) postgres.close();
        if (migrations != null) {
            try (var paths = Files.walk(migrations)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
