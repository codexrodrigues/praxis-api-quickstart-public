package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Opt-in PostgreSQL proof for the P2F-ADR-12 least-privilege role matrix.
 * It creates only disposable NOLOGIN roles on an explicitly selected ephemeral Neon branch.
 */
public final class RuleAuditRoleSeparationProof {
    private static final String JDBC_URL = required("ADR12_ROLE_JDBC_URL");
    private static final String DATABASE_USER = required("ADR12_ROLE_DATABASE_USER");
    private static final String DATABASE_PASSWORD = required("ADR12_ROLE_DATABASE_PASSWORD");
    private static final Path PROJECT_ROOT = Path.of(required("ADR12_ROLE_PROJECT_ROOT"));
    private static final Path EVIDENCE_FILE = Path.of(required("ADR12_ROLE_EVIDENCE_FILE"));
    private static final String LEDGER = "extraordinary_benefit_statement_replay_audit";
    private static final String ACTIVE_KEY_ID = "ADR12-HMAC-2026-07";
    private static final String PREVIOUS_KEY_ID = "ADR12-HMAC-2025-12";
    private static final byte[] ACTIVE_SECRET =
            "fictional-active-adr12-secret-material-2026".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PREVIOUS_SECRET =
            "fictional-previous-adr12-secret-material-2025".getBytes(StandardCharsets.UTF_8);
    private static final Pattern SAFE_ROLE_NAME = Pattern.compile("[a-z][a-z0-9_]{0,62}");

    private RuleAuditRoleSeparationProof() {
    }

    public static void main(String[] args) throws Exception {
        require(Boolean.parseBoolean(required("ADR12_ROLE_EPHEMERAL_BRANCH")),
                "Role proof is allowed only on an explicitly selected ephemeral branch");
        Class.forName("org.postgresql.Driver");

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        RoleMatrix roles = new RoleMatrix(
                "adr12_" + suffix + "_app",
                "adr12_" + suffix + "_audit",
                "adr12_" + suffix + "_hold",
                "adr12_" + suffix + "_retention");

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("proof", "ADR12_RULE_AUDIT_ROLE_SEPARATION");
        evidence.put("ephemeralBranch", true);
        evidence.put("roleModel", "NOLOGIN_CAPABILITY_ROLES");
        evidence.put("startedAtUtc", Instant.now().toString());

        try {
            evidence.put("migrationUpgrade", applyMigrations());
            createRoles(roles);
            grantOwnerMembershipForProof(roles);
            applyGrantTemplate(roles);
            evidence.put("authorizationHmac", proveHmacRotation());
            evidence.put("ownership", proveOwnershipSeparation(roles));
            evidence.put("privilegeCatalog", provePrivilegeCatalog(roles));
            evidence.put("negativePaths", proveNegativePaths(roles));
            evidence.put("positivePaths", provePositivePaths(roles));
        } finally {
            dropRoles(roles);
        }

        evidence.put("disposableRolesDropped", true);
        evidence.put("finishedAtUtc", Instant.now().toString());
        Files.createDirectories(EVIDENCE_FILE.getParent());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(EVIDENCE_FILE.toFile(), evidence);
        System.out.println("ADR12_ROLE_SEPARATION_PROOF_PASS evidence=" + EVIDENCE_FILE);
    }

    private static Map<String, Object> applyMigrations() throws Exception {
        Path migrations = PROJECT_ROOT.resolve("db/operational-migrations");
        try (Connection connection = openOwnerConnection()) {
            connection.setAutoCommit(false);
            if (!auditLedgerObjectsExist(connection)) {
                executeSqlFile(connection, migrations.resolve(
                        "V20260714_003__extraordinary_benefit_statement_outbox.sql"));
                executeSqlFile(connection, migrations.resolve(
                        "V20260715_001__extraordinary_benefit_transformation_audit.sql"));
            }
            resetRetentionObjectsForUpgradeProof(connection);
            executeSqlFile(connection, migrations.resolve(
                    "V20260715_002__rule_audit_privacy_retention.sql"));
            seedUnversionedAuthorizationEvidence(connection);
            executeSqlFile(connection, migrations.resolve(
                    "V20260715_004__rule_audit_authorization_key_rotation.sql"));
            int legacyEvidenceRows = queryInt(connection, """
                    select count(*) from (
                        select authorization_key_id from public.praxis_rule_audit_retention_run
                        union all
                        select authorization_key_id from public.praxis_rule_audit_legal_hold
                        union all
                        select authorization_key_id from public.praxis_rule_audit_legal_hold_event
                    ) evidence
                    where authorization_key_id = 'LEGACY-UNVERSIONED'
                    """);
            require(legacyEvidenceRows == 3, "Legacy HMAC provenance was not backfilled as expected");
            require(queryBoolean(connection, """
                    select to_regprocedure(
                               'public.record_rule_audit_legal_hold(uuid,varchar,uuid,varchar,varchar,char)') is null
                       and to_regprocedure(
                               'public.purge_rule_audit(uuid,varchar,varchar,timestamptz,integer,char)') is null
                       and to_regprocedure(
                               'public.record_rule_audit_legal_hold(uuid,varchar,uuid,varchar,varchar,varchar,char)')
                               is not null
                       and to_regprocedure(
                               'public.purge_rule_audit(uuid,varchar,varchar,timestamptz,integer,varchar,char)')
                               is not null
                    """), "Versioned HMAC routines did not replace the obsolete signatures");
            connection.commit();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("legacyBackfillExercised", true);
            result.put("legacyEvidenceRowsBackfilled", legacyEvidenceRows);
            result.put("obsoleteUnversionedRoutinesRemoved", true);
            result.put("versionedRoutinesInstalled", true);
            return result;
        }
    }

    private static void resetRetentionObjectsForUpgradeProof(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    drop function if exists public.record_rule_audit_legal_hold(
                        uuid, varchar, uuid, varchar, varchar, varchar, char)
                    """);
            statement.execute("""
                    drop function if exists public.record_rule_audit_legal_hold(
                        uuid, varchar, uuid, varchar, varchar, char)
                    """);
            statement.execute("""
                    drop function if exists public.purge_rule_audit(
                        uuid, varchar, varchar, timestamptz, integer, varchar, char)
                    """);
            statement.execute("""
                    drop function if exists public.purge_rule_audit(
                        uuid, varchar, varchar, timestamptz, integer, char)
                    """);
            statement.execute("drop table if exists public.praxis_rule_audit_legal_hold_event cascade");
            statement.execute("drop table if exists public.praxis_rule_audit_legal_hold cascade");
            statement.execute("drop table if exists public.praxis_rule_audit_retention_run cascade");
        }
    }

    private static void seedUnversionedAuthorizationEvidence(Connection connection) throws SQLException {
        UUID heldAuditId = UUID.randomUUID();
        UUID retainedAuditId = UUID.randomUUID();
        try (PreparedStatement statement = connection.prepareStatement(replayInsertSql(heldAuditId))) {
            require(statement.executeUpdate() == 1, "Could not seed legacy legal-hold evidence");
        }
        try (PreparedStatement statement = connection.prepareStatement(replayInsertSql(retainedAuditId))) {
            require(statement.executeUpdate() == 1, "Could not seed legacy retention evidence");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "select public.record_rule_audit_legal_hold(?, ?, ?, 'PLACE', "
                        + "'LEGACY_HMAC_UPGRADE', repeat('A', 64)::char(64))")) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, LEDGER);
            statement.setObject(3, heldAuditId);
            require(statement.executeQuery().next(), "Could not seed unversioned legal-hold evidence");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "select deleted_rows from public.purge_rule_audit(?, ?, 'LEGACY_HMAC_UPGRADE', "
                        + "transaction_timestamp() - interval '2 days', 10, repeat('B', 64)::char(64))")) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, LEDGER);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next() && result.getInt(1) == 1,
                        "Could not seed unversioned retention evidence");
            }
        }
    }

    private static boolean auditLedgerObjectsExist(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                select to_regclass('public.extraordinary_benefit_statement_replay_audit') is not null
                   and to_regclass('public.extraordinary_benefit_transformation_audit') is not null
                """)) {
            require(result.next(), "Could not inspect the operational schema");
            return result.getBoolean(1);
        }
    }

    private static Map<String, Object> proveHmacRotation() {
        var active = RuleAuditAuthorizationHmac.compute(
                ACTIVE_KEY_ID,
                RuleAuditAuthorizationHmac.Purpose.RETENTION,
                "fictional-rotation-proof-reference",
                ACTIVE_SECRET);
        var repeated = RuleAuditAuthorizationHmac.compute(
                ACTIVE_KEY_ID,
                RuleAuditAuthorizationHmac.Purpose.RETENTION,
                "fictional-rotation-proof-reference",
                ACTIVE_SECRET);
        var previous = RuleAuditAuthorizationHmac.compute(
                PREVIOUS_KEY_ID,
                RuleAuditAuthorizationHmac.Purpose.RETENTION,
                "fictional-rotation-proof-reference",
                PREVIOUS_SECRET);
        require(active.equals(repeated), "Canonical HMAC must be deterministic");
        require(!active.digest().equals(previous.digest()), "Rotated HMAC key must change the digest");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algorithm", "HMAC-SHA-256");
        result.put("canonicalContract", "PRAXIS-RULE-AUDIT-AUTHORIZATION-V1");
        result.put("deterministic", true);
        result.put("rotationChangesDigest", true);
        result.put("minimumSecretBits", 256);
        result.put("secretPersisted", false);
        result.put("authorizationReferencePersisted", false);
        return result;
    }

    private static void executeSqlFile(Connection connection, Path path) throws Exception {
        require(Files.isRegularFile(path), "Required SQL file is missing: " + path.getFileName());
        try (Statement statement = connection.createStatement()) {
            statement.execute(Files.readString(path));
        }
    }

    private static void createRoles(RoleMatrix roles) throws SQLException {
        try (Connection connection = openOwnerConnection(); Statement statement = connection.createStatement()) {
            for (String role : roles.all()) {
                statement.execute("create role " + quoteIdentifier(role)
                        + " nologin nosuperuser nocreatedb nocreaterole noreplication nobypassrls");
            }
        }
    }

    private static void grantOwnerMembershipForProof(RoleMatrix roles) throws SQLException {
        try (Connection connection = openOwnerConnection(); Statement statement = connection.createStatement()) {
            String owner = currentUser(connection);
            for (String role : roles.all()) {
                statement.execute("grant " + quoteIdentifier(role) + " to " + quoteIdentifier(owner));
            }
        }
    }

    private static void applyGrantTemplate(RoleMatrix roles) throws Exception {
        Path templatePath = PROJECT_ROOT.resolve(
                "db/operational-provisioning/rule-audit-role-grants.sql.template");
        require(Files.isRegularFile(templatePath), "Role grant template is missing");
        String sql = Files.readString(templatePath)
                .replace("{{APPLICATION_RUNTIME_ROLE}}", quoteIdentifier(roles.applicationRuntime()))
                .replace("{{AUDIT_READER_ROLE}}", quoteIdentifier(roles.auditReader()))
                .replace("{{LEGAL_HOLD_OPERATOR_ROLE}}", quoteIdentifier(roles.legalHoldOperator()))
                .replace("{{RETENTION_WORKER_ROLE}}", quoteIdentifier(roles.retentionWorker()));
        require(!sql.contains("{{"), "Unresolved role placeholder in provisioning template");
        try (Connection connection = openOwnerConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static Map<String, Object> proveOwnershipSeparation(RoleMatrix roles) throws SQLException {
        try (Connection connection = openOwnerConnection()) {
            for (String role : roles.all()) {
                require(queryInt(connection, """
                        select count(*)
                        from pg_class relation
                        join pg_roles owner_role on owner_role.oid = relation.relowner
                        where owner_role.rolname = ?
                          and relation.relname in (
                            'extraordinary_benefit_statement_replay_audit',
                            'extraordinary_benefit_transformation_audit',
                            'praxis_rule_audit_retention_guard',
                            'praxis_rule_audit_retention_run',
                            'praxis_rule_audit_legal_hold',
                            'praxis_rule_audit_legal_hold_event')
                        """, role) == 0, "Capability role must not own rule-audit tables");
                require(queryInt(connection, """
                        select count(*)
                        from pg_proc routine
                        join pg_roles owner_role on owner_role.oid = routine.proowner
                        where owner_role.rolname = ?
                          and routine.proname in ('record_rule_audit_legal_hold', 'purge_rule_audit')
                        """, role) == 0, "Capability role must not own governed functions");
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("capabilityRolesOwnTables", false);
        result.put("capabilityRolesOwnFunctions", false);
        return result;
    }

    private static Map<String, Object> provePrivilegeCatalog(RoleMatrix roles) throws SQLException {
        try (Connection connection = openOwnerConnection()) {
            assertTablePrivilege(connection, roles.applicationRuntime(),
                    "public.extraordinary_benefit_statement_replay_audit", "INSERT", true);
            assertTablePrivilege(connection, roles.applicationRuntime(),
                    "public.extraordinary_benefit_transformation_audit", "INSERT", true);
            assertTablePrivilege(connection, roles.applicationRuntime(),
                    "public.extraordinary_benefit_statement_replay_audit", "SELECT", false);
            assertTablePrivilege(connection, roles.auditReader(),
                    "public.extraordinary_benefit_statement_replay_audit", "SELECT", true);
            assertTablePrivilege(connection, roles.auditReader(),
                    "public.praxis_rule_audit_retention_guard", "SELECT", false);
            assertFunctionPrivilege(connection, roles.legalHoldOperator(),
                    "public.record_rule_audit_legal_hold(uuid,varchar,uuid,varchar,varchar,varchar,char)", true);
            assertFunctionPrivilege(connection, roles.legalHoldOperator(),
                    "public.purge_rule_audit(uuid,varchar,varchar,timestamptz,integer,varchar,char)", false);
            assertFunctionPrivilege(connection, roles.retentionWorker(),
                    "public.purge_rule_audit(uuid,varchar,varchar,timestamptz,integer,varchar,char)", true);
            assertFunctionPrivilege(connection, roles.retentionWorker(),
                    "public.record_rule_audit_legal_hold(uuid,varchar,uuid,varchar,varchar,varchar,char)", false);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applicationInsertOnly", true);
        result.put("auditReaderExcludesInternalGuard", true);
        result.put("legalHoldExecuteOnly", true);
        result.put("retentionExecuteOnly", true);
        return result;
    }

    private static Map<String, Object> proveNegativePaths(RoleMatrix roles) throws SQLException {
        assertDenied(roles.applicationRuntime(),
                "select count(*) from public.extraordinary_benefit_statement_replay_audit");
        assertDenied(roles.applicationRuntime(), holdSql());
        assertDenied(roles.applicationRuntime(), purgeSql());
        assertDenied(roles.auditReader(), replayInsertSql(UUID.randomUUID()));
        assertDenied(roles.auditReader(), holdSql());
        assertDenied(roles.auditReader(), purgeSql());
        assertDenied(roles.legalHoldOperator(),
                "select count(*) from public.extraordinary_benefit_statement_replay_audit");
        assertDenied(roles.legalHoldOperator(), purgeSql());
        assertDenied(roles.retentionWorker(),
                "select count(*) from public.extraordinary_benefit_statement_replay_audit");
        assertDenied(roles.retentionWorker(), holdSql());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deniedAttempts", 10);
        result.put("sqlState", "42501");
        result.put("crossRoleEscalationPrevented", true);
        return result;
    }

    private static Map<String, Object> provePositivePaths(RoleMatrix roles) throws SQLException {
        UUID auditId = UUID.randomUUID();
        UUID placeEventId = UUID.randomUUID();
        UUID releaseEventId = UUID.randomUUID();
        UUID retentionRunId = UUID.randomUUID();

        try (Connection application = openAsRole(roles.applicationRuntime());
                PreparedStatement statement = application.prepareStatement(replayInsertSql(auditId))) {
            require(statement.executeUpdate() == 1, "Application role could not append the fictional audit fixture");
        }
        try (Connection reader = openAsRole(roles.auditReader())) {
            require(queryInt(reader,
                    "select count(*) from public.extraordinary_benefit_statement_replay_audit where audit_id = ?",
                    auditId) == 1, "Audit reader could not read the fictional audit fixture");
        }
        try (Connection hold = openAsRole(roles.legalHoldOperator())) {
            recordLegalHold(hold, placeEventId, auditId, "PLACE", "ROLE_PROOF_PLACE");
            recordLegalHold(hold, releaseEventId, auditId, "RELEASE", "ROLE_PROOF_RELEASE");
        }
        try (Connection retention = openAsRole(roles.retentionWorker())) {
            require(purge(retention, retentionRunId) == 1,
                    "Retention role could not purge the released fictional audit fixture");
        }
        try (Connection reader = openAsRole(roles.auditReader())) {
            require(queryInt(reader,
                    "select count(*) from public.praxis_rule_audit_legal_hold_event where record_id = ?",
                    auditId) == 2, "Audit reader could not inspect the append-only hold history");
            require(queryInt(reader,
                    "select count(*) from public.praxis_rule_audit_legal_hold_event "
                            + "where record_id = ? and authorization_key_id = '" + ACTIVE_KEY_ID + "'",
                    auditId) == 2, "Legal hold evidence did not capture the HMAC key version");
            require(queryInt(reader,
                    "select deleted_rows from public.praxis_rule_audit_retention_run where retention_run_id = ?",
                    retentionRunId) == 1, "Audit reader could not inspect the retention evidence");
            require(queryInt(reader,
                    "select count(*) from public.praxis_rule_audit_retention_run "
                            + "where retention_run_id = ? and authorization_key_id = '" + ACTIVE_KEY_ID + "'",
                    retentionRunId) == 1, "Retention evidence did not capture the HMAC key version");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applicationAppend", true);
        result.put("auditRead", true);
        result.put("legalHoldPlaceAndRelease", true);
        result.put("retentionPurge", true);
        result.put("authorizationKeyVersionCaptured", true);
        result.put("appendOnlyHoldEvents", 2);
        return result;
    }

    private static void assertDenied(String role, String sql) throws SQLException {
        try (Connection connection = openAsRole(role); Statement statement = connection.createStatement()) {
            statement.execute(sql);
            throw new IllegalStateException("Expected PostgreSQL to reject a cross-role operation");
        } catch (SQLException rejected) {
            require("42501".equals(rejected.getSQLState()),
                    "Expected insufficient_privilege, received SQLState " + rejected.getSQLState());
        }
    }

    private static void assertTablePrivilege(
            Connection connection,
            String role,
            String table,
            String privilege,
            boolean expected) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select has_table_privilege(?, ?, ?)")) {
            statement.setString(1, role);
            statement.setString(2, table);
            statement.setString(3, privilege);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next() && result.getBoolean(1) == expected,
                        "Unexpected table privilege for capability role");
            }
        }
    }

    private static void assertFunctionPrivilege(
            Connection connection,
            String role,
            String function,
            boolean expected) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select has_function_privilege(?, ?, 'EXECUTE')")) {
            statement.setString(1, role);
            statement.setString(2, function);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next() && result.getBoolean(1) == expected,
                        "Unexpected function privilege for capability role");
            }
        }
    }

    private static UUID recordLegalHold(
            Connection connection,
            UUID eventId,
            UUID auditId,
            String action,
            String reasonCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select public.record_rule_audit_legal_hold(?, ?, ?, ?, ?, ?, ?::char(64))")) {
            var authorization = RuleAuditAuthorizationHmac.compute(
                    ACTIVE_KEY_ID,
                    RuleAuditAuthorizationHmac.Purpose.LEGAL_HOLD,
                    "fictional-role-proof-" + eventId,
                    ACTIVE_SECRET);
            statement.setObject(1, eventId);
            statement.setString(2, LEDGER);
            statement.setObject(3, auditId);
            statement.setString(4, action);
            statement.setString(5, reasonCode);
            statement.setString(6, authorization.keyId());
            statement.setString(7, authorization.digest());
            try (ResultSet result = statement.executeQuery()) {
                require(result.next(), "Legal hold function returned no result");
                return result.getObject(1, UUID.class);
            }
        }
    }

    private static int purge(Connection connection, UUID retentionRunId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select deleted_rows from public.purge_rule_audit(?, ?, 'ROLE_PROOF_RETENTION', "
                        + "transaction_timestamp() - interval '2 days', 10, ?, ?::char(64))")) {
            var authorization = RuleAuditAuthorizationHmac.compute(
                    ACTIVE_KEY_ID,
                    RuleAuditAuthorizationHmac.Purpose.RETENTION,
                    "fictional-role-proof-" + retentionRunId,
                    ACTIVE_SECRET);
            statement.setObject(1, retentionRunId);
            statement.setString(2, LEDGER);
            statement.setString(3, authorization.keyId());
            statement.setString(4, authorization.digest());
            try (ResultSet result = statement.executeQuery()) {
                require(result.next(), "Retention function returned no result");
                return result.getInt(1);
            }
        }
    }

    private static String holdSql() {
        return "select public.record_rule_audit_legal_hold('" + UUID.randomUUID()
                + "', '" + LEDGER + "', '" + UUID.randomUUID()
                + "', 'PLACE', 'ROLE_PROOF_DENIED', '" + ACTIVE_KEY_ID
                + "', repeat('A', 64)::char(64))";
    }

    private static String purgeSql() {
        return "select deleted_rows from public.purge_rule_audit('" + UUID.randomUUID()
                + "', '" + LEDGER + "', 'ROLE_PROOF_DENIED', transaction_timestamp() - interval '2 days', "
                + "10, '" + ACTIVE_KEY_ID + "', repeat('B', 64)::char(64))";
    }

    private static String replayInsertSql(UUID auditId) {
        return "insert into public.extraordinary_benefit_statement_replay_audit ("
                + "audit_id, message_id, requested_at, actor_subject, justification, correlation_id, replay_outcome) "
                + "values ('" + auditId + "', '" + UUID.randomUUID()
                + "', transaction_timestamp() - interval '10 days', 'fictional-adr12-role-actor', "
                + "'fictional ADR-12 role separation proof', 'adr12-role-proof', 'REPLAY_SCHEDULED')";
    }

    private static int queryInt(Connection connection, String sql, Object parameter) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, parameter);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next(), "Proof query returned no result");
                return result.getInt(1);
            }
        }
    }

    private static int queryInt(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            require(result.next(), "Proof query returned no result");
            return result.getInt(1);
        }
    }

    private static boolean queryBoolean(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            require(result.next(), "Proof query returned no result");
            return result.getBoolean(1);
        }
    }

    private static String currentUser(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("select current_user")) {
            require(result.next(), "Could not identify migration owner");
            return result.getString(1);
        }
    }

    private static Connection openOwnerConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, DATABASE_USER, DATABASE_PASSWORD);
    }

    private static Connection openAsRole(String role) throws SQLException {
        Connection connection = openOwnerConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("set role " + quoteIdentifier(role));
            require(currentUser(connection).equals(role), "PostgreSQL session did not assume the requested role");
            return connection;
        } catch (Exception failure) {
            connection.close();
            throw failure;
        }
    }

    private static void dropRoles(RoleMatrix roles) throws SQLException {
        try (Connection connection = openOwnerConnection(); Statement statement = connection.createStatement()) {
            for (String role : roles.all()) {
                try {
                    String quotedRole = quoteIdentifier(role);
                    statement.execute("revoke all on schema public from " + quotedRole);
                    statement.execute("revoke all on table "
                            + "public.extraordinary_benefit_statement_replay_audit, "
                            + "public.extraordinary_benefit_transformation_audit, "
                            + "public.praxis_rule_audit_retention_guard, "
                            + "public.praxis_rule_audit_retention_run, "
                            + "public.praxis_rule_audit_legal_hold, "
                            + "public.praxis_rule_audit_legal_hold_event from " + quotedRole);
                    statement.execute("revoke all on function public.record_rule_audit_legal_hold("
                            + "uuid, varchar, uuid, varchar, varchar, varchar, char(64)) from " + quotedRole);
                    statement.execute("revoke all on function public.purge_rule_audit("
                            + "uuid, varchar, varchar, timestamptz, integer, varchar, char(64)) from " + quotedRole);
                    statement.execute("drop role " + quoteIdentifier(role));
                } catch (SQLException cleanupFailure) {
                    if (!"42704".equals(cleanupFailure.getSQLState())) {
                        throw cleanupFailure;
                    }
                }
            }
        }
    }

    private static String quoteIdentifier(String identifier) {
        require(SAFE_ROLE_NAME.matcher(identifier).matches(), "Unsafe PostgreSQL role identifier");
        return '"' + identifier + '"';
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private record RoleMatrix(
            String applicationRuntime,
            String auditReader,
            String legalHoldOperator,
            String retentionWorker) {
        private List<String> all() {
            return List.of(applicationRuntime, auditReader, legalHoldOperator, retentionWorker);
        }
    }
}
