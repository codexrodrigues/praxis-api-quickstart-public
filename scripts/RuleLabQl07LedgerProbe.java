import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only QL-07 ledger counts plus narrowly scoped cleanup for its disposable fixture. */
public final class RuleLabQl07LedgerProbe {
    private static final String RESOURCE_KEY = "human-resources.extraordinary-benefit-requests";

    private RuleLabQl07LedgerProbe() {
    }

    public static void main(String[] args) throws Exception {
        String url = required("SPRING_DATASOURCE_URL");
        String username = required("SPRING_DATASOURCE_USERNAME");
        String password = required("SPRING_DATASOURCE_PASSWORD");
        Class.forName("org.postgresql.Driver");

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            if (args.length > 0 && "cleanup".equals(args[0])) {
                if (args.length != 3 || !args[1].startsWith("QL07-") || !args[2].startsWith("ql07-")) {
                    throw new IllegalArgumentException("Cleanup requires a QL07- reference and ql07- idempotency prefix.");
                }
                cleanup(connection, args[1], args[2]);
            }
            System.out.println(toJson(counts(connection)));
        }
    }

    private static Map<String, Long> counts(Connection connection) throws Exception {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("requests", count(connection, "select count(*) from public.extraordinary_benefit_request"));
        counts.put("effects", count(connection, "select count(*) from public.extraordinary_benefit_grant_effect"));
        counts.put("executions", count(connection, "select count(*) from public.praxis_resource_action_execution"));
        counts.put("transitions", count(connection, "select count(*) from public.praxis_resource_action_transition"));
        return counts;
    }

    private static long count(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static void cleanup(Connection connection, String reference, String idempotencyPrefix) throws Exception {
        connection.setAutoCommit(false);
        try {
            Long requestId = null;
            try (PreparedStatement statement = connection.prepareStatement(
                    "select id from public.extraordinary_benefit_request where request_reference = ?")) {
                statement.setString(1, reference);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) requestId = result.getLong(1);
                }
            }
            if (requestId != null) {
                execute(connection, "delete from public.extraordinary_benefit_grant_effect where benefit_request_id = ?", requestId);
                execute(connection, "delete from public.praxis_resource_action_transition where resource_key = ? and resource_id = ?",
                        RESOURCE_KEY, requestId.toString());
            }
            execute(connection, "delete from public.praxis_resource_action_execution where resource_key = ? and idempotency_key like ?",
                    RESOURCE_KEY, idempotencyPrefix + "%");
            execute(connection, "delete from public.extraordinary_benefit_request where request_reference = ?", reference);
            connection.commit();
        } catch (Exception failure) {
            connection.rollback();
            throw failure;
        }
    }

    private static void execute(Connection connection, String sql, Object... values) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            statement.executeUpdate();
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing environment variable " + name);
        return value;
    }

    private static String toJson(Map<String, Long> values) {
        return "{\"requests\":" + values.get("requests")
                + ",\"effects\":" + values.get("effects")
                + ",\"executions\":" + values.get("executions")
                + ",\"transitions\":" + values.get("transitions") + "}";
    }
}
