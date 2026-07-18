package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Opt-in PostgreSQL proof for the ADR-12 race between legal hold and governed retention.
 * It is intentionally restricted to an ephemeral Neon branch and writes only fictional fixtures.
 */
public final class RuleAuditRetentionConcurrencyProof {
    private static final String JDBC_URL = required("ADR12_JDBC_URL");
    private static final String DATABASE_USER = required("ADR12_DATABASE_USER");
    private static final String DATABASE_PASSWORD = required("ADR12_DATABASE_PASSWORD");
    private static final Path PROJECT_ROOT = Path.of(required("ADR12_PROJECT_ROOT"));
    private static final Path EVIDENCE_FILE = Path.of(required("ADR12_EVIDENCE_FILE"));
    private static final String LEDGER = "extraordinary_benefit_statement_replay_audit";
    private static final String AUTHORIZATION_KEY_ID = "ADR12-HMAC-LAB-CONCURRENCY";

    private RuleAuditRetentionConcurrencyProof() {
    }

    public static void main(String[] args) throws Exception {
        require(Boolean.parseBoolean(required("ADR12_EPHEMERAL_BRANCH")),
                "ADR-12 concurrency proof is allowed only on an explicitly selected ephemeral branch");
        Class.forName("org.postgresql.Driver");
        applyMigrations();

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("proof", "ADR12_LEGAL_HOLD_RETENTION_CONCURRENCY");
        evidence.put("ephemeralBranch", true);
        evidence.put("startedAtUtc", Instant.now().toString());
        evidence.put("placeWins", provePlaceWins());
        evidence.put("purgeWins", provePurgeWins());
        evidence.put("finishedAtUtc", Instant.now().toString());

        Files.createDirectories(EVIDENCE_FILE.getParent());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(EVIDENCE_FILE.toFile(), evidence);
        System.out.println("ADR12_CONCURRENCY_PROOF_PASS evidence=" + EVIDENCE_FILE);
    }

    private static Map<String, Object> provePlaceWins() throws Exception {
        UUID auditId = UUID.randomUUID();
        UUID holdEventId = UUID.randomUUID();
        UUID releaseEventId = UUID.randomUUID();
        UUID skippedRunId = UUID.randomUUID();
        UUID finalRunId = UUID.randomUUID();
        insertReplayAudit(auditId, "place-wins");

        int skippedRows;
        try (Connection holdConnection = openConnection()) {
            holdConnection.setAutoCommit(false);
            recordLegalHold(holdConnection, holdEventId, auditId, "PLACE", "CONCURRENCY_PLACE_WINS");
            try (Connection retentionConnection = openConnection()) {
                skippedRows = purge(retentionConnection, skippedRunId, "CONCURRENCY_SKIP_LOCKED", 10);
            }
            require(skippedRows == 0, "Retention must skip the audit row locked by legal hold");
            holdConnection.commit();
        }

        require(countReplayAudit(auditId) == 1, "Held audit row must remain after the concurrent purge");
        require(countActiveHold(auditId) == 1, "PLACE must create exactly one active legal hold");
        require(retentionDeletedRows(skippedRunId) == 0, "Skipped retention run must record zero deletions");

        try (Connection connection = openConnection()) {
            recordLegalHold(connection, releaseEventId, auditId, "RELEASE", "CONCURRENCY_RELEASE");
            require(purge(connection, finalRunId, "CONCURRENCY_AFTER_RELEASE", 10) == 1,
                    "Released audit row must be removed by the next retention run");
        }
        require(countReplayAudit(auditId) == 0, "Released audit row must no longer exist");
        require(countActiveHold(auditId) == 0, "Released audit row must not retain hold state");
        require(countHoldEvents(auditId) == 2, "PLACE and RELEASE must both remain in the append-only history");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("retentionSkippedLockedRow", true);
        result.put("holdProtectedRow", true);
        result.put("releaseEnabledLaterPurge", true);
        result.put("holdHistoryEvents", 2);
        return result;
    }

    private static Map<String, Object> provePurgeWins() throws Exception {
        UUID auditId = UUID.randomUUID();
        UUID attemptedHoldEventId = UUID.randomUUID();
        UUID purgeRunId = UUID.randomUUID();
        insertReplayAudit(auditId, "purge-wins");

        CountDownLatch placeStarted = new CountDownLatch(1);
        AtomicInteger placeBackendPid = new AtomicInteger();
        CompletableFuture<SQLException> placeAttempt;
        try (Connection purgeConnection = openConnection()) {
            purgeConnection.setAutoCommit(false);
            require(purge(purgeConnection, purgeRunId, "CONCURRENCY_PURGE_WINS", 10) == 1,
                    "Retention transaction must select the second fixture");

            placeAttempt = CompletableFuture.supplyAsync(() -> {
                try (Connection holdConnection = openConnection()) {
                    holdConnection.setAutoCommit(false);
                    placeBackendPid.set(backendPid(holdConnection));
                    placeStarted.countDown();
                    recordLegalHold(holdConnection, attemptedHoldEventId, auditId, "PLACE",
                            "CONCURRENCY_PURGE_ALREADY_LOCKED");
                    holdConnection.commit();
                    return null;
                } catch (SQLException rejected) {
                    return rejected;
                }
            });
            require(placeStarted.await(5, TimeUnit.SECONDS), "Concurrent PLACE session did not start");
            require(waitingForLock(purgeConnection, placeBackendPid.get(), 5, TimeUnit.SECONDS),
                    "PLACE was not observed waiting for the purge row lock");
            purgeConnection.commit();
        }

        SQLException rejection = placeAttempt.get(10, TimeUnit.SECONDS);
        require(rejection != null, "PLACE must fail after the winning purge commits");
        require(rejection.getMessage() != null
                        && rejection.getMessage().contains("rule audit record does not exist"),
                "PLACE failed for an unexpected reason: " + safeMessage(rejection));
        require(countReplayAudit(auditId) == 0, "Purged audit row must remain deleted");
        require(countActiveHold(auditId) == 0, "Losing PLACE must not create orphan hold state");
        require(countHoldEvents(auditId) == 0, "Losing PLACE must not create an append-only hold event");
        require(retentionDeletedRows(purgeRunId) == 1, "Winning retention run must record one deletion");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("placeWaitedForPurgeCommit", true);
        result.put("placeRejectedAfterPurge", true);
        result.put("orphanHoldPrevented", true);
        result.put("orphanEventPrevented", true);
        return result;
    }

    private static void applyMigrations() throws Exception {
        Path migrations = PROJECT_ROOT.resolve("db/operational-migrations");
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            executeSqlFile(connection, migrations.resolve(
                    "V20260714_003__extraordinary_benefit_statement_outbox.sql"));
            executeSqlFile(connection, migrations.resolve(
                    "V20260715_001__extraordinary_benefit_transformation_audit.sql"));
            executeSqlFile(connection, migrations.resolve(
                    "V20260715_002__rule_audit_privacy_retention.sql"));
            executeSqlFile(connection, migrations.resolve(
                    "V20260715_004__rule_audit_authorization_key_rotation.sql"));
            connection.commit();
        }
    }

    private static void executeSqlFile(Connection connection, Path path) throws Exception {
        require(Files.isRegularFile(path), "Required migration is missing: " + path.getFileName());
        try (Statement statement = connection.createStatement()) {
            statement.execute(Files.readString(path));
        }
    }

    private static void insertReplayAudit(UUID auditId, String scenario) throws SQLException {
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement("""
                insert into public.extraordinary_benefit_statement_replay_audit (
                    audit_id, message_id, requested_at, actor_subject, justification,
                    correlation_id, replay_outcome)
                values (?, ?, transaction_timestamp() - interval '10 days', ?, ?, ?, 'REPLAY_SCHEDULED')
                """)) {
            statement.setObject(1, auditId);
            statement.setObject(2, UUID.randomUUID());
            statement.setString(3, "fictional-adr12-actor");
            statement.setString(4, "fictional ADR-12 concurrency proof");
            statement.setString(5, "adr12-concurrency-" + scenario);
            require(statement.executeUpdate() == 1, "Could not insert the fictional replay audit fixture");
        }
    }

    private static UUID recordLegalHold(
            Connection connection,
            UUID eventId,
            UUID auditId,
            String action,
            String reasonCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select public.record_rule_audit_legal_hold(?, ?, ?, ?, ?, ?, repeat('A', 64)::char(64))")) {
            statement.setObject(1, eventId);
            statement.setString(2, LEDGER);
            statement.setObject(3, auditId);
            statement.setString(4, action);
            statement.setString(5, reasonCode);
            statement.setString(6, AUTHORIZATION_KEY_ID);
            statement.setQueryTimeout(10);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next(), "Legal hold function returned no result");
                return result.getObject(1, UUID.class);
            }
        }
    }

    private static int purge(Connection connection, UUID runId, String policyKey, int batchSize)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select deleted_rows from public.purge_rule_audit(?, ?, ?, "
                        + "transaction_timestamp() - interval '2 days', ?, ?, repeat('B', 64)::char(64))")) {
            statement.setObject(1, runId);
            statement.setString(2, LEDGER);
            statement.setString(3, policyKey);
            statement.setInt(4, batchSize);
            statement.setString(5, AUTHORIZATION_KEY_ID);
            statement.setQueryTimeout(10);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next(), "Retention function returned no result");
                return result.getInt(1);
            }
        }
    }

    private static int countReplayAudit(UUID auditId) throws SQLException {
        return count("select count(*) from public.extraordinary_benefit_statement_replay_audit where audit_id = ?",
                auditId);
    }

    private static int countActiveHold(UUID auditId) throws SQLException {
        return count("select count(*) from public.praxis_rule_audit_legal_hold "
                + "where ledger_name = 'extraordinary_benefit_statement_replay_audit' and record_id = ?", auditId);
    }

    private static int countHoldEvents(UUID auditId) throws SQLException {
        return count("select count(*) from public.praxis_rule_audit_legal_hold_event "
                + "where ledger_name = 'extraordinary_benefit_statement_replay_audit' and record_id = ?", auditId);
    }

    private static int retentionDeletedRows(UUID runId) throws SQLException {
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(
                "select deleted_rows from public.praxis_rule_audit_retention_run where retention_run_id = ?")) {
            statement.setObject(1, runId);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next(), "Retention run evidence is missing");
                return result.getInt(1);
            }
        }
    }

    private static int backendPid(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("select pg_backend_pid()")) {
            require(result.next(), "Could not identify the concurrent PostgreSQL session");
            return result.getInt(1);
        }
    }

    private static boolean waitingForLock(
            Connection observer,
            int backendPid,
            long timeout,
            TimeUnit unit) throws Exception {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        try (PreparedStatement statement = observer.prepareStatement(
                "select wait_event_type = 'Lock' from pg_stat_activity where pid = ?")) {
            statement.setInt(1, backendPid);
            while (System.nanoTime() < deadline) {
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next() && result.getBoolean(1)) {
                        return true;
                    }
                }
                Thread.sleep(50);
            }
            return false;
        }
    }

    private static int count(String sql, UUID id) throws SQLException {
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next(), "Count query returned no result");
                return result.getInt(1);
            }
        }
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, DATABASE_USER, DATABASE_PASSWORD);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private static String safeMessage(SQLException failure) {
        return failure.getSQLState() + "/" + failure.getClass().getSimpleName();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
