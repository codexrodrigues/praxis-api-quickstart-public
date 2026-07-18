import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class ApiOperationalSchemaDriftCheck {
    private ApiOperationalSchemaDriftCheck() {
    }

    public static void main(String[] args) throws Exception {
        Properties app = loadProperties(Path.of("src/main/resources/application.properties"));
        String url = resolve("SPRING_DATASOURCE_URL", "spring.datasource.url", app);
        String username = resolve("SPRING_DATASOURCE_USERNAME", "spring.datasource.username", app);
        String password = resolve("SPRING_DATASOURCE_PASSWORD", "spring.datasource.password", app);

        Class.forName("org.postgresql.Driver");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            List<String> failures = new ArrayList<>();
            checkTable(connection, "public", "legacy_pay_codes", failures);
            checkColumn(connection, "public", "legacy_pay_codes", "code", failures);
            checkColumn(connection, "public", "legacy_pay_codes", "description", failures);
            checkColumn(connection, "public", "legacy_pay_codes", "payroll_category", failures);
            checkColumn(connection, "public", "legacy_pay_codes", "status", failures);
            checkColumn(connection, "public", "legacy_pay_codes", "active", failures);
            checkColumn(connection, "public", "eventos_folha", "status", failures);
            checkColumn(connection, "public", "eventos_folha", "version", failures);
            checkColumn(connection, "public", "funcionarios", "version", failures);
            checkTable(connection, "public", "praxis_resource_action_transition", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "transition_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "resource_key", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "resource_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "action_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "performed_at", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "actor_subject", failures);
            checkColumn(connection, "public", "praxis_resource_action_transition", "correlation_id", failures);
            checkTable(connection, "public", "praxis_resource_action_execution", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "execution_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "resource_key", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "resource_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "action_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "action_scope", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "idempotency_key", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "request_hash", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "execution_status", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "response_payload", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "correlation_id", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "actor_subject", failures);
            checkColumn(connection, "public", "praxis_resource_action_execution", "started_at", failures);
            checkTable(connection, "public", "procurement_purchase_orders", failures);
            checkColumn(connection, "public", "procurement_purchase_orders", "status", failures);
            checkColumn(connection, "public", "procurement_purchase_orders", "disabled_reason", failures);
            checkColumn(connection, "public", "procurement_purchase_orders", "approved_at", failures);
            checkColumn(connection, "public", "procurement_purchase_orders", "cancelled_at", failures);
            checkColumn(connection, "public", "procurement_purchase_orders", "received_at", failures);
            checkTable(connection, "public", "extraordinary_benefit_request", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "request_reference", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "lifecycle_status", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "effect_status", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "snapshot_content_hash", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "fact_reference", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "fact_provider_key", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "fact_source_record_digest", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "fact_source_version", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "fact_scope_digest", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "fact_as_of", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "evaluation_reason_codes", failures);
            checkColumn(connection, "public", "extraordinary_benefit_request", "version", failures);
            checkTable(connection, "public", "extraordinary_benefit_grant_effect", failures);
            checkColumn(connection, "public", "extraordinary_benefit_grant_effect", "effect_execution_id", failures);
            checkColumn(connection, "public", "extraordinary_benefit_grant_effect", "benefit_request_id", failures);
            checkColumn(connection, "public", "extraordinary_benefit_grant_effect", "revalidation_snapshot_key", failures);
            checkColumn(connection, "public", "extraordinary_benefit_grant_effect", "revalidation_facts_digest", failures);
            checkColumn(connection, "public", "extraordinary_benefit_grant_effect", "revalidation_source_version", failures);
            checkColumn(connection, "public", "extraordinary_benefit_grant_effect", "revalidated_at", failures);
            checkTable(connection, "public", "extraordinary_benefit_statement_outbox", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_outbox", "message_id", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_outbox", "operation_id", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_outbox", "payload", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_outbox", "delivery_status", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_outbox", "delivery_attempts", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_outbox", "next_attempt_at", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_outbox", "next_reconciliation_at", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_outbox", "lease_token", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_outbox", "lease_until", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_outbox", "last_failure_at", failures);
            checkTable(connection, "public", "extraordinary_benefit_statement_replay_audit", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_replay_audit", "audit_id", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_replay_audit", "message_id", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_replay_audit", "actor_subject", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_replay_audit", "justification", failures);
            checkColumn(connection, "public", "extraordinary_benefit_statement_replay_audit", "replay_outcome", failures);
            checkTable(connection, "public", "extraordinary_benefit_transformation_audit", failures);
            checkColumn(connection, "public", "extraordinary_benefit_transformation_audit", "audit_id", failures);
            checkColumn(connection, "public", "extraordinary_benefit_transformation_audit", "benefit_request_id", failures);
            checkColumn(connection, "public", "extraordinary_benefit_transformation_audit", "proposal_key", failures);
            checkColumn(connection, "public", "extraordinary_benefit_transformation_audit", "proposal_identity_digest", failures);
            checkColumn(connection, "public", "extraordinary_benefit_transformation_audit", "before_digest", failures);
            checkColumn(connection, "public", "extraordinary_benefit_transformation_audit", "after_digest", failures);
            checkColumnType(connection, "public", "extraordinary_benefit_transformation_audit", "proposal_identity_digest", "character varying", 64, failures);
            checkColumnType(connection, "public", "extraordinary_benefit_transformation_audit", "before_digest", "character varying", 64, failures);
            checkColumnType(connection, "public", "extraordinary_benefit_transformation_audit", "after_digest", "character varying", 64, failures);
            checkColumnType(connection, "public", "extraordinary_benefit_transformation_audit", "snapshot_content_hash", "character varying", 64, failures);
            checkColumnType(connection, "public", "extraordinary_benefit_transformation_audit", "facts_digest", "character varying", 64, failures);
            checkColumnType(connection, "public", "extraordinary_benefit_transformation_audit", "plan_digest", "character varying", 64, failures);
            checkColumn(connection, "public", "extraordinary_benefit_transformation_audit", "correlation_id", failures);
            checkColumn(connection, "public", "extraordinary_benefit_transformation_audit", "recorded_at", failures);
            checkTable(connection, "public", "praxis_rule_audit_retention_run", failures);
            checkColumn(connection, "public", "praxis_rule_audit_retention_run", "retention_run_id", failures);
            checkColumn(connection, "public", "praxis_rule_audit_retention_run", "ledger_name", failures);
            checkColumn(connection, "public", "praxis_rule_audit_retention_run", "authorization_key_id", failures);
            checkColumn(connection, "public", "praxis_rule_audit_retention_run", "authorization_reference_digest", failures);
            checkColumn(connection, "public", "praxis_rule_audit_retention_run", "deleted_rows", failures);
            checkTable(connection, "public", "praxis_rule_audit_legal_hold", failures);
            checkColumn(connection, "public", "praxis_rule_audit_legal_hold", "record_id", failures);
            checkColumn(connection, "public", "praxis_rule_audit_legal_hold", "hold_id", failures);
            checkColumn(connection, "public", "praxis_rule_audit_legal_hold", "authorization_key_id", failures);
            checkTable(connection, "public", "praxis_rule_audit_legal_hold_event", failures);
            checkColumn(connection, "public", "praxis_rule_audit_legal_hold_event", "event_id", failures);
            checkColumn(connection, "public", "praxis_rule_audit_legal_hold_event", "hold_action", failures);
            checkColumn(connection, "public", "praxis_rule_audit_legal_hold_event", "authorization_key_id", failures);
            checkRoutineSignature(connection,
                    "public.record_rule_audit_legal_hold(uuid,varchar,uuid,varchar,varchar,varchar,char)",
                    true, failures);
            checkRoutineSignature(connection,
                    "public.purge_rule_audit(uuid,varchar,varchar,timestamptz,integer,varchar,char)",
                    true, failures);
            checkRoutineSignature(connection,
                    "public.record_rule_audit_legal_hold(uuid,varchar,uuid,varchar,varchar,char)",
                    false, failures);
            checkRoutineSignature(connection,
                    "public.purge_rule_audit(uuid,varchar,varchar,timestamptz,integer,char)",
                    false, failures);
            checkTrigger(connection, "public", "extraordinary_benefit_statement_replay_audit",
                    "trg_extraordinary_benefit_statement_replay_audit_append_only", failures);
            checkTrigger(connection, "public", "extraordinary_benefit_transformation_audit",
                    "trg_extraordinary_benefit_transformation_audit_append_only", failures);
            checkTable(connection, "public", "funcionario_lotacoes_departamento", failures);
            checkColumn(connection, "public", "funcionario_lotacoes_departamento", "funcionario_id", failures);
            checkColumn(connection, "public", "funcionario_lotacoes_departamento", "departamento_id", failures);
            checkColumn(connection, "public", "funcionario_lotacoes_departamento", "effective_from", failures);
            checkColumn(connection, "public", "funcionario_lotacoes_departamento", "effective_to", failures);
            checkTable(connection, "public", "vw_analytics_afastamentos", failures);
            checkRoutine(connection, "public", "hr_absence_criticality_level", failures);
            checkColumn(connection, "public", "vw_analytics_afastamentos", "analytics_id", failures);
            checkColumn(connection, "public", "vw_analytics_afastamentos", "funcionario_id", failures);
            checkColumn(connection, "public", "vw_analytics_afastamentos", "departamento_id", failures);
            checkColumn(connection, "public", "vw_analytics_afastamentos", "departamento", failures);
            checkColumn(connection, "public", "vw_analytics_afastamentos", "competencia", failures);
            checkColumn(connection, "public", "vw_analytics_afastamentos", "periodo_inicio", failures);
            checkColumn(connection, "public", "vw_analytics_afastamentos", "periodo_fim", failures);
            checkColumn(connection, "public", "vw_analytics_afastamentos", "dias_afastado", failures);
            checkColumn(connection, "public", "vw_analytics_afastamentos", "criticality_level", failures);
            checkColumn(connection, "public", "vw_analytics_afastamentos", "criticality_policy_id", failures);
            checkColumn(connection, "public", "vw_analytics_afastamentos", "criticality_policy_version", failures);
            checkTable(connection, "public", "vw_analytics_folha_pagamento", failures);
            checkColumn(connection, "public", "vw_analytics_folha_pagamento", "folha_pagamento_id", failures);
            checkColumn(connection, "public", "vw_analytics_folha_pagamento", "departamento_id", failures);
            checkColumn(connection, "public", "vw_analytics_folha_pagamento", "departamento", failures);
            checkColumn(connection, "public", "vw_analytics_folha_pagamento", "competencia", failures);
            checkColumn(connection, "public", "vw_analytics_folha_pagamento", "salario_bruto", failures);
            checkColumn(connection, "public", "vw_analytics_folha_pagamento", "total_descontos", failures);
            checkColumn(connection, "public", "vw_analytics_folha_pagamento", "salario_liquido", failures);
            checkZeroCount(connection, """
                    select count(*)
                    from public.folhas_pagamento fp
                    where not exists (
                        select 1
                        from public.funcionario_lotacoes_departamento ld
                        where ld.funcionario_id = fp.funcionario_id
                          and ld.effective_from <= make_date(fp.ano, fp.mes, 1)
                          and coalesce(ld.effective_to, 'infinity'::date) > make_date(fp.ano, fp.mes, 1)
                    )
                    """, "Payroll facts without an effective department assignment at competence day 1.", failures);
            checkTable(connection, "public", "rule_lab_authoritative_benefit_facts", failures);
            checkColumn(connection, "public", "rule_lab_authoritative_benefit_facts", "tenant_id", failures);
            checkColumn(connection, "public", "rule_lab_authoritative_benefit_facts", "environment", failures);
            checkColumn(connection, "public", "rule_lab_authoritative_benefit_facts", "organization_key", failures);
            checkColumn(connection, "public", "rule_lab_authoritative_benefit_facts", "fact_reference", failures);
            checkColumn(connection, "public", "rule_lab_authoritative_benefit_facts", "source_record_digest", failures);
            checkColumn(connection, "public", "rule_lab_authoritative_benefit_facts", "source_version", failures);
            checkColumn(connection, "public", "rule_lab_authoritative_benefit_facts", "effective_from", failures);
            checkColumn(connection, "public", "rule_lab_authoritative_benefit_facts", "effective_to", failures);
            checkTable(connection, "public", "rule_lab_authoritative_benefit_payment_date", failures);
            checkColumn(connection, "public", "rule_lab_authoritative_benefit_payment_date", "allowed_payment_date", failures);
            checkDistinctCount(
                    connection,
                    "public.procurement_purchase_orders",
                    "status",
                    2,
                    "Expected procurement purchase orders to expose more than one lifecycle status for cockpit charts.",
                    failures
            );

            if (!failures.isEmpty()) {
                System.err.println("API operational datasource drift detected:");
                for (String failure : failures) {
                    System.err.println("- " + failure);
                }
                System.exit(1);
            }

            System.out.println("API operational datasource drift check passed.");
        }
    }

    private static void checkTable(Connection connection, String schema, String table, List<String> failures)
            throws SQLException {
        if (!exists(connection, """
                select 1
                from information_schema.tables
                where table_schema = ?
                  and table_name = ?
                """, schema, table)) {
            failures.add("Missing table " + schema + "." + table);
        }
    }

    private static void checkColumn(Connection connection, String schema, String table, String column,
            List<String> failures) throws SQLException {
        if (!exists(connection, """
                select 1
                from information_schema.columns
                where table_schema = ?
                  and table_name = ?
                  and column_name = ?
                """, schema, table, column)) {
            failures.add("Missing column " + schema + "." + table + "." + column);
        }
    }

    private static void checkColumnType(
            Connection connection,
            String schema,
            String table,
            String column,
            String dataType,
            int maximumLength,
            List<String> failures) throws SQLException {
        boolean expectedType;
        try (PreparedStatement statement = connection.prepareStatement("""
                select 1
                from information_schema.columns
                where table_schema = ?
                  and table_name = ?
                  and column_name = ?
                  and data_type = ?
                  and character_maximum_length = ?
                """)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            statement.setString(3, column);
            statement.setString(4, dataType);
            statement.setInt(5, maximumLength);
            try (ResultSet resultSet = statement.executeQuery()) {
                expectedType = resultSet.next();
            }
        }
        if (!expectedType) {
            failures.add("Expected " + schema + "." + table + "." + column
                    + " to be " + dataType + "(" + maximumLength + ")");
        }
    }

    private static void checkRoutine(
            Connection connection,
            String schema,
            String routine,
            List<String> failures) throws SQLException {
        if (!exists(connection, """
                select 1
                from information_schema.routines
                where routine_schema = ?
                  and routine_name = ?
                """, schema, routine)) {
            failures.add("Missing routine " + schema + "." + routine);
        }
    }

    private static void checkRoutineSignature(
            Connection connection,
            String signature,
            boolean expected,
            List<String> failures) throws SQLException {
        boolean present;
        try (PreparedStatement statement = connection.prepareStatement(
                "select to_regprocedure(?) is not null")) {
            statement.setString(1, signature);
            try (ResultSet resultSet = statement.executeQuery()) {
                present = resultSet.next() && resultSet.getBoolean(1);
            }
        }
        if (present != expected) {
            failures.add((expected ? "Missing routine " : "Obsolete routine remains ") + signature);
        }
    }

    private static void checkTrigger(
            Connection connection,
            String schema,
            String table,
            String trigger,
            List<String> failures) throws SQLException {
        if (!exists(connection, """
                select 1
                from information_schema.triggers
                where event_object_schema = ?
                  and event_object_table = ?
                  and trigger_name = ?
                """, schema, table, trigger)) {
            failures.add("Missing trigger " + schema + "." + trigger + " on " + table);
        }
    }

    private static boolean exists(Connection connection, String sql, String... args) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setString(i + 1, args[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void checkDistinctCount(
            Connection connection,
            String table,
            String column,
            int minimum,
            String message,
            List<String> failures
    ) throws SQLException {
        String sql = "select count(distinct " + column + ") from " + table;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next() && resultSet.getInt(1) < minimum) {
                failures.add(message);
            }
        }
    }

    private static void checkZeroCount(
            Connection connection,
            String sql,
            String message,
            List<String> failures) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next() || resultSet.getLong(1) != 0L) {
                failures.add(message);
            }
        }
    }

    private static Properties loadProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static String resolve(String envName, String propertyName, Properties properties) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }

        String value = properties.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + envName + " or " + propertyName);
        }

        if (value.startsWith("${") && value.endsWith("}")) {
            int separator = value.indexOf(':');
            if (separator > 2) {
                return value.substring(separator + 1, value.length() - 1);
            }
        }
        return value;
    }
}
