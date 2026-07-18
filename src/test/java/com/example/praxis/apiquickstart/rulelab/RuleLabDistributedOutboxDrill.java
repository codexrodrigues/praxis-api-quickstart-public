package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.ApiQuickstartApplication;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.praxisplatform.config.dto.DomainRuleSnapshotActivationResponse;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Opt-in multi-process drill runner. It starts and restarts a standalone HTTPS consumer while the
 * Quickstart uses PostgreSQL and its production outbox adapter.
 */
public final class RuleLabDistributedOutboxDrill {
    private static final Instant ACTIVATION_TIME = Instant.parse("2026-07-14T12:00:00Z");
    private static final String TOKEN_ONE = randomSecret();
    private static final String TOKEN_TWO = randomSecret();
    private static final Path WORK_DIRECTORY = Path.of(required("QL08_DRILL_WORK_DIRECTORY"));
    private static final String API_JDBC_URL = required("QL08_API_JDBC_URL");
    private static final String CONSUMER_JDBC_URL = required("QL08_CONSUMER_JDBC_URL");
    private static final String CONFIG_JDBC_URL = required("QL08_CONFIG_JDBC_URL");
    private static final String API_SCHEMA = requiredIdentifier("QL08_API_SCHEMA");
    private static final String CONSUMER_SCHEMA = requiredIdentifier("QL08_CONSUMER_SCHEMA");
    private static final String API_DATABASE_USER = required("QL08_API_DATABASE_USER");
    private static final String API_DATABASE_PASSWORD = required("QL08_API_DATABASE_PASSWORD");
    private static final String CONSUMER_DATABASE_USER = required("QL08_CONSUMER_DATABASE_USER");
    private static final String CONSUMER_DATABASE_PASSWORD = required("QL08_CONSUMER_DATABASE_PASSWORD");
    private static final boolean EPHEMERAL_BRANCHES = Boolean.parseBoolean(
            required("QL08_EPHEMERAL_BRANCHES"));

    public static void main(String[] args) throws Exception {
        require(EPHEMERAL_BRANCHES,
                "The distributed drill is allowed only on explicitly selected ephemeral Neon branches");
        Files.createDirectories(WORK_DIRECTORY);
        var evidence = new LinkedHashMap<String, Object>();
        ConsumerProcess consumer = null;
        ConfigurableApplicationContext context = null;
        try {
            consumer = startConsumer(TOKEN_ONE, true, "phase-1-timeout");
            clearConsumerInbox();
            context = startQuickstart(consumer.baseUrl(), TOKEN_ONE, 750);
            initializeOperationalSchema(context);
            activateSnapshot(context);
            var command = context.getBean(ExtraordinaryBenefitStatementCommandService.class);
            var dispatcher = context.getBean(ExtraordinaryBenefitStatementOutboxDispatcher.class);
            var first = command.execute(
                    List.of(eligibleRequest(context, "BEN-QL08-DISTRIBUTED-001")),
                    Set.of("benefit:request"), "ql08-drill", "ql08-timeout", "ql08-timeout");
            var ambiguous = dispatcher.dispatchNext();
            require(ambiguous.outcome() == ExtraordinaryBenefitStatementDispatchOutcome.RETRY_SCHEDULED,
                    "First delivery must time out after the external commit");
            require(consumerInboxCount(first.outboxMessageId()) == 1,
                    "External inbox must contain exactly one committed message after timeout");
            evidence.put("ambiguousDelivery", ambiguous.outcome().name());
            close(context);
            context = null;
            consumer.close();
            consumer = null;

            consumer = startConsumer(TOKEN_ONE, false, "phase-2-restart");
            int restartProbeStatus = probeStatus(consumer.baseUrl(), first.outboxMessageId(), TOKEN_ONE);
            require(restartProbeStatus == 200,
                    "Restarted external consumer acknowledgement probe returned HTTP " + restartProbeStatus);
            context = startQuickstart(consumer.baseUrl(), TOKEN_ONE, 10_000);
            try {
                var acknowledgement = context.getBean(ExtraordinaryBenefitStatementDeliveryProbe.class)
                        .findAcknowledgement(first.outboxMessageId());
                require(acknowledgement.isPresent(),
                        "Quickstart delivery probe did not find the committed external acknowledgement");
            } catch (Exception probeFailure) {
                throw new IllegalStateException("Quickstart delivery probe failed: "
                        + probeFailure.getClass().getSimpleName() + ": " + probeFailure.getMessage(), probeFailure);
            }
            var reconciler = context.getBean(ExtraordinaryBenefitStatementOutboxReconciler.class);
            var reconciled = reconciler.reconcileNext();
            require(reconciled.outcome() == ExtraordinaryBenefitStatementReconciliationOutcome.RECONCILED,
                    "Restarted Quickstart must reconcile the committed external inbox row; outcome="
                            + reconciled.outcome());
            require(localDeliveryStatus(context, first.outboxMessageId()).equals("DELIVERED"),
                    "Reconciled outbox message must be DELIVERED");
            require(consumerInboxCount(first.outboxMessageId()) == 1,
                    "Reconciliation must not redeliver the committed message");
            evidence.put("restartReconciliation", reconciled.outcome().name());
            close(context);
            context = null;
            consumer.close();
            consumer = null;

            consumer = startConsumer(TOKEN_TWO, false, "phase-3-credential-rotation");
            require(probeStatus(consumer.baseUrl(), first.outboxMessageId(), TOKEN_ONE) == 401,
                    "Rotated consumer must reject the previous credential");
            context = startQuickstart(consumer.baseUrl(), TOKEN_TWO, 10_000);
            activateSnapshot(context);
            var rotatedCommand = context.getBean(ExtraordinaryBenefitStatementCommandService.class);
            var rotatedDispatcher = context.getBean(ExtraordinaryBenefitStatementOutboxDispatcher.class);
            var second = rotatedCommand.execute(
                    List.of(eligibleRequest(context, "BEN-QL08-DISTRIBUTED-002")),
                    Set.of("benefit:request"), "ql08-drill", "ql08-rotated", "ql08-rotated");
            var delivered = rotatedDispatcher.dispatchNext();
            require(delivered.outcome() == ExtraordinaryBenefitStatementDispatchOutcome.DELIVERED,
                    "Delivery with the rotated credential must succeed; outcome=" + delivered.outcome());
            require(consumerInboxCount(second.outboxMessageId()) == 1,
                    "Rotated delivery must create exactly one inbox row");
            evidence.put("oldCredentialHttpStatus", 401);
            evidence.put("rotatedDelivery", delivered.outcome().name());
            evidence.put("consumerInboxRows", totalConsumerInboxCount());
            evidence.put("transport", "HTTPS");
            evidence.put("quickstartDatabase", "PostgreSQL 17");
            evidence.put("consumerDatabase", "PostgreSQL 17 / separate database");
            evidence.put("databaseIsolation", "TWO_EPHEMERAL_NEON_BRANCHES");
            evidence.put("processBoundary", "SEPARATE_JVM");
            evidence.put("completedAtUtc", Instant.now().toString());
            Path evidenceFile = WORK_DIRECTORY.resolve("distributed-outbox-drill-evidence.json");
            new ObjectMapper().findAndRegisterModules().writerWithDefaultPrettyPrinter()
                    .writeValue(evidenceFile.toFile(), evidence);
            System.out.println("QL08_DISTRIBUTED_OUTBOX_DRILL_PASS evidence=" + evidenceFile.toAbsolutePath());
        } finally {
            close(context);
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    private static ConfigurableApplicationContext startQuickstart(
            String baseUrl, String bearerToken, long requestTimeoutMs) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("server.port", "0");
        properties.put("spring.main.banner-mode", "off");
        properties.put("logging.level.root", "WARN");
        properties.put("app.rate-limit.enabled", "false");
        properties.put("app.security.config-origin-restriction.enabled", "false");
        properties.put("app.security.read-open", "true");
        properties.put("app.security.write-disabled", "false");
        properties.put("app.security.csrf.disable", "true");
        properties.put("praxis.resource-version.etag.secret", "ql08-distributed-drill-etag-secret");
        properties.put("praxis.rule-lab.snapshot.enabled", "false");
        properties.put("praxis.rule-lab.outbox.maximum-attempts", "3");
        properties.put("praxis.rule-lab.outbox.lease-ms", "2000");
        properties.put("praxis.rule-lab.outbox.retry-base-ms", "100");
        properties.put("praxis.rule-lab.outbox.reconciliation-retry-ms", "100");
        properties.put("praxis.rule-lab.outbox.http.enabled", "true");
        properties.put("praxis.rule-lab.outbox.http.base-url", baseUrl);
        properties.put("praxis.rule-lab.outbox.http.bearer-token", bearerToken);
        properties.put("praxis.rule-lab.outbox.http.connect-timeout-ms", "1000");
        properties.put("praxis.rule-lab.outbox.http.request-timeout-ms", Long.toString(requestTimeoutMs));
        properties.put("praxis.ai.provider", "mock");
        properties.put("spring.ai.embedding.provider", "mock");
        properties.put("spring.ai.openai.api-key", "dummy");
        properties.put("praxis.ai.rag.vector-store.enabled", "false");
        properties.put("praxis.ai.registry.bootstrap.enabled", "false");
        properties.put("praxis.ai.registry.health.enabled", "false");
        properties.put("spring.ai.vectorstore.pgvector.initialize-schema", "false");
        properties.put("spring.ai.vectorstore.pgvector.vector-table-validations-enabled", "false");
        properties.put("spring.flyway.enabled", "false");
        properties.put("spring.datasource.url", API_JDBC_URL);
        properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        properties.put("spring.datasource.username", API_DATABASE_USER);
        properties.put("spring.datasource.password", API_DATABASE_PASSWORD);
        properties.put("spring.datasource.hikari.schema", API_SCHEMA);
        properties.put("config.datasource.url", CONFIG_JDBC_URL);
        properties.put("config.datasource.driver-class-name", "org.postgresql.Driver");
        properties.put("config.datasource.username", CONSUMER_DATABASE_USER);
        properties.put("config.datasource.password", CONSUMER_DATABASE_PASSWORD);
        properties.put("spring.jpa.hibernate.ddl-auto", "none");
        String[] arguments = properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
        return new SpringApplicationBuilder(ApiQuickstartApplication.class).run(arguments);
    }

    private static void initializeOperationalSchema(ConfigurableApplicationContext context) throws Exception {
        JdbcTemplate jdbc = context.getBean("apiJdbcTemplate", JdbcTemplate.class);
        String currentSchema = jdbc.queryForObject("select current_schema()", String.class);
        require(API_SCHEMA.equals(currentSchema),
                "API datasource isolation failed: expected schema " + API_SCHEMA + " but found " + currentSchema);
        jdbc.execute("""
                create table if not exists extraordinary_benefit_request (
                    id bigint generated by default as identity primary key, request_reference varchar(80) not null unique,
                    reason_code varchar(40) not null, event_date date not null, requested_amount numeric(15,2) not null,
                    worker_status varchar(20) not null, duplicate_grant boolean not null, program_active boolean not null,
                    program_maximum_amount numeric(15,2) not null, customer_additional_eligible boolean,
                    requested_payment_date date not null, allowed_payment_dates varchar(1000) not null,
                    available_budget_amount numeric(15,2) not null, user_time_zone varchar(80) not null,
                    lifecycle_status varchar(20) not null, recommended_amount numeric(15,2) not null,
                    currency varchar(3) not null, snapshot_key varchar(200) not null,
                    snapshot_content_hash varchar(64) not null, snapshot_activation_revision bigint not null,
                    rule_set_key varchar(200) not null, rule_set_version integer not null,
                    facts_digest varchar(64) not null, plan_digest varchar(64) not null,
                    planned_effect_intent varchar(120) not null, evaluation_business_message varchar(1000) not null,
                    evaluation_reason_codes varchar(1000) not null, effect_status varchar(20) not null,
                    evaluated_at timestamptz not null, submitted_at timestamptz, approved_at timestamptz,
                    applied_at timestamptz, created_by varchar(255) not null,
                    last_transition_by varchar(255) not null, version bigint not null default 0)
                """);
        jdbc.execute("""
                create table if not exists extraordinary_benefit_grant_effect (
                    id bigint generated by default as identity primary key, effect_execution_id uuid not null unique,
                    benefit_request_id bigint not null unique, request_reference varchar(80) not null,
                    intent_type varchar(120) not null, amount numeric(15,2) not null, currency varchar(3) not null,
                    executed_at timestamptz not null, executed_by varchar(255) not null,
                    foreign key (benefit_request_id) references extraordinary_benefit_request(id))
                """);
        jdbc.execute("""
                create table if not exists praxis_resource_action_execution (
                    execution_id uuid primary key, resource_key varchar(200) not null, resource_id varchar(128) not null,
                    action_id varchar(120) not null, action_scope varchar(32) not null,
                    idempotency_key varchar(255) not null, request_hash varchar(128) not null,
                    execution_status varchar(32) not null, response_payload jsonb, correlation_id varchar(255) not null,
                    request_id varchar(255), actor_subject varchar(255) not null, actor_authorities varchar(1000),
                    started_at timestamptz not null, completed_at timestamptz,
                    failure_code varchar(120), failure_message varchar(1000),
                    unique(resource_key, resource_id, action_id, actor_subject, idempotency_key))
                """);
        jdbc.execute("""
                create table if not exists praxis_resource_action_transition (
                    transition_id uuid primary key, resource_key varchar(200) not null, resource_id varchar(128) not null,
                    action_id varchar(120) not null, action_scope varchar(32) not null, previous_state varchar(120),
                    resulting_state varchar(120), reason_code varchar(120), comment varchar(1000),
                    effective_at date not null, performed_at timestamptz not null,
                    actor_subject varchar(255) not null, actor_authorities varchar(1000), correlation_id varchar(255) not null,
                    request_id varchar(255), idempotency_key varchar(255), version_before bigint, version_after bigint)
                """);
        String migration = Files.readString(Path.of("db", "operational-migrations",
                "V20260714_003__extraordinary_benefit_statement_outbox.sql"), StandardCharsets.UTF_8)
                .replace("public.", "");
        jdbc.execute(migration);
        jdbc.update("delete from praxis_resource_action_transition");
        jdbc.update("delete from extraordinary_benefit_statement_outbox");
        jdbc.update("delete from praxis_resource_action_execution");
        jdbc.update("delete from extraordinary_benefit_grant_effect");
        jdbc.update("delete from extraordinary_benefit_request");
    }

    private static void activateSnapshot(ConfigurableApplicationContext context) {
        var runtime = context.getBean(ExtraordinaryGrantRuleSnapshotRuntime.class);
        var registry = context.getBean("extraordinaryGrantRuleExecutorRegistry", RuleBindingExecutorRegistry.class);
        PublishedRuleSnapshot snapshot = snapshot();
        String contentHash = new PraxisRuleSnapshotCompiler(registry)
                .compile(snapshot, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION)
                .snapshotContentHash();
        runtime.activate(new DomainRuleSnapshotActivationResponse(
                        snapshot, contentHash, "ql08-distributed-head", 1, "ACTIVE"),
                "desenv", "local", ACTIVATION_TIME);
    }

    private static ConsumerProcess startConsumer(String token, boolean timeoutOnce, String phase) throws Exception {
        Path readiness = WORK_DIRECTORY.resolve(phase + ".ready");
        Files.deleteIfExists(readiness);
        Path log = WORK_DIRECTORY.resolve(phase + ".log");
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        ProcessBuilder builder = new ProcessBuilder(
                javaExecutable,
                "-cp", System.getProperty("java.class.path"),
                RuleLabExternalConsumerProcess.class.getName());
        Map<String, String> environment = builder.environment();
        environment.put("QL08_CONSUMER_JDBC_URL", CONSUMER_JDBC_URL);
        environment.put("QL08_DATABASE_USER", CONSUMER_DATABASE_USER);
        environment.put("QL08_DATABASE_PASSWORD", CONSUMER_DATABASE_PASSWORD);
        environment.put("QL08_CONSUMER_SCHEMA", CONSUMER_SCHEMA);
        environment.put("QL08_CONSUMER_BEARER_TOKEN", token);
        environment.put("QL08_TIMEOUT_AFTER_COMMIT_ONCE", Boolean.toString(timeoutOnce));
        environment.put("QL08_TLS_KEYSTORE", required("QL08_TLS_KEYSTORE"));
        environment.put("QL08_TLS_KEYSTORE_PASSWORD", required("QL08_TLS_KEYSTORE_PASSWORD"));
        environment.put("QL08_CONSUMER_READINESS_FILE", readiness.toString());
        Process process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!Files.exists(readiness) && process.isAlive() && System.nanoTime() < deadline) {
            Thread.sleep(100);
        }
        if (!Files.exists(readiness)) {
            process.destroyForcibly();
            throw new IllegalStateException("External consumer did not become ready; log=" + log);
        }
        return new ConsumerProcess(process, Files.readString(readiness, StandardCharsets.UTF_8).trim());
    }

    private static ExtraordinaryBenefitEvaluationRequest eligibleRequest(
            ConfigurableApplicationContext context, String reference) throws Exception {
        String json = """
                {"requestReference":"%s","reasonCode":"FAMILY_HARDSHIP","eventDate":"2026-07-13",
                 "requestedAmount":2500.00,"workerStatus":"ACTIVE","duplicateGrant":false,
                 "programActive":true,"programMaximumAmount":5000.00,"customerAdditionalEligible":true,
                 "requestedPaymentDate":"2026-07-20","allowedPaymentDates":["2026-07-20","2026-08-05"],
                 "availableBudgetAmount":100000.00,"userTimeZone":"America/Sao_Paulo"}
                """.formatted(reference);
        return context.getBean(ObjectMapper.class).readValue(json, ExtraordinaryBenefitEvaluationRequest.class);
    }

    private static PublishedRuleSnapshot snapshot() {
        return new PublishedRuleSnapshot(
                PublishedRuleSnapshot.SNAPSHOT_CONTRACT_VERSION, "extraordinary-grant-v1", "desenv", "local",
                ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY, 1, "2026-07-13T11:00:00Z", null,
                ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION, "2026-01-01T00:00:00Z", null,
                List.of(
                        new RuleSnapshotSource("definition-1", "grant:eligibility", 1, "A".repeat(64)),
                        new RuleSnapshotSource("definition-2", "grant:amount", 1, "B".repeat(64))),
                List.of(
                        new RuleSnapshotApproval("approval-1", "RULE_DEFINITION_APPROVER", "approver-a",
                                "2026-07-13T10:00:00Z", "A".repeat(64)),
                        new RuleSnapshotApproval("approval-2", "RULE_DEFINITION_APPROVER", "approver-b",
                                "2026-07-13T10:05:00Z", "B".repeat(64))),
                ExtraordinaryGrantRuleSetFactory.definition());
    }

    private static String localDeliveryStatus(ConfigurableApplicationContext context, UUID messageId) {
        return context.getBean("apiJdbcTemplate", JdbcTemplate.class).queryForObject(
                "select delivery_status from extraordinary_benefit_statement_outbox where message_id = ?",
                String.class, messageId);
    }

    private static int consumerInboxCount(UUID messageId) throws Exception {
        try (var connection = consumerConnection();
                var statement = connection.prepareStatement(
                        "select count(*) from statement_inbox where message_id = ?")) {
            statement.setObject(1, messageId);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static int totalConsumerInboxCount() throws Exception {
        try (var connection = consumerConnection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("select count(*) from statement_inbox")) {
            result.next();
            return result.getInt(1);
        }
    }

    private static void clearConsumerInbox() throws Exception {
        try (var connection = consumerConnection();
                var statement = connection.createStatement()) {
            statement.executeUpdate("delete from statement_inbox");
        }
    }

    private static int probeStatus(String baseUrl, UUID messageId, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        baseUrl + "/inbox/extraordinary-benefit-statements/" + messageId))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private static java.sql.Connection consumerConnection() throws Exception {
        var connection = DriverManager.getConnection(
                CONSUMER_JDBC_URL, CONSUMER_DATABASE_USER, CONSUMER_DATABASE_PASSWORD);
        connection.setSchema(CONSUMER_SCHEMA);
        return connection;
    }

    private static void close(ConfigurableApplicationContext context) {
        if (context != null) {
            context.close();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private static String requiredIdentifier(String name) {
        String value = required(name);
        if (!value.matches("[a-z][a-z0-9_]{2,62}")) {
            throw new IllegalStateException(name + " must be a safe PostgreSQL identifier");
        }
        return value;
    }

    private record ConsumerProcess(Process process, String baseUrl) implements AutoCloseable {
        @Override
        public void close() {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }
}
