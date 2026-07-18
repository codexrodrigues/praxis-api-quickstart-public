package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/** Standalone HTTPS consumer used only by the opt-in QL-08 distributed drill. */
public final class RuleLabExternalConsumerProcess {
    private static final String PATH = "/inbox/extraordinary-benefit-statements";
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final String jdbcUrl = required("QL08_CONSUMER_JDBC_URL");
    private final String databaseUser = required("QL08_DATABASE_USER");
    private final String databasePassword = required("QL08_DATABASE_PASSWORD");
    private final String databaseSchema = requiredIdentifier("QL08_CONSUMER_SCHEMA");
    private final String bearerToken = required("QL08_CONSUMER_BEARER_TOKEN");
    private final AtomicBoolean timeoutAfterCommitOnce = new AtomicBoolean(
            Boolean.parseBoolean(System.getenv().getOrDefault("QL08_TIMEOUT_AFTER_COMMIT_ONCE", "false")));

    public static void main(String[] args) throws Exception {
        new RuleLabExternalConsumerProcess().run();
    }

    private void run() throws Exception {
        initializeInbox();
        HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext()));
        server.createContext(PATH, this::handle);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0), "ql08-consumer-shutdown"));
        Path readinessFile = Path.of(required("QL08_CONSUMER_READINESS_FILE"));
        Files.createDirectories(readinessFile.getParent());
        Files.writeString(readinessFile, "https://localhost:" + server.getAddress().getPort(),
                StandardCharsets.UTF_8);
        new CountDownLatch(1).await();
    }

    private void initializeInbox() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("""
                    create table if not exists statement_inbox (
                        message_id uuid primary key,
                        operation_id uuid not null,
                        payload_hash varchar(64) not null,
                        acknowledged_at_utc timestamptz not null)
                    """);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!(exchange instanceof com.sun.net.httpserver.HttpsExchange)
                    || !constantTimeEquals("Bearer " + bearerToken,
                            exchange.getRequestHeaders().getFirst("Authorization"))) {
                send(exchange, 401, Map.of("error", "unauthorized"));
                return;
            }
            String suffix = exchange.getRequestURI().getPath().substring(PATH.length());
            if ("POST".equals(exchange.getRequestMethod()) && suffix.isEmpty()) {
                receive(exchange);
                return;
            }
            if ("GET".equals(exchange.getRequestMethod()) && suffix.matches("/[0-9a-fA-F-]{36}")) {
                find(exchange, UUID.fromString(suffix.substring(1)));
                return;
            }
            send(exchange, 404, Map.of("error", "not-found"));
        } catch (Exception failure) {
            send(exchange, 500, Map.of("error", "consumer-failure"));
        }
    }

    private void receive(HttpExchange exchange) throws Exception {
        JsonNode delivery = objectMapper.readTree(exchange.getRequestBody());
        UUID messageId = UUID.fromString(delivery.path("messageId").asText());
        UUID operationId = UUID.fromString(delivery.path("operationId").asText());
        if (!constantTimeEquals(messageId.toString(),
                exchange.getRequestHeaders().getFirst("Idempotency-Key"))) {
            send(exchange, 400, Map.of("error", "invalid-idempotency-key"));
            return;
        }
        String payloadHash = sha256(delivery.path("payload").toString());
        InboxRow row;
        boolean inserted;
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try (var insert = connection.prepareStatement("""
                    insert into statement_inbox(message_id, operation_id, payload_hash, acknowledged_at_utc)
                    values (?, ?, ?, ?)
                    on conflict (message_id) do nothing
                    """)) {
                Instant now = Instant.now();
                insert.setObject(1, messageId);
                insert.setObject(2, operationId);
                insert.setString(3, payloadHash);
                insert.setTimestamp(4, Timestamp.from(now));
                inserted = insert.executeUpdate() == 1;
            }
            row = find(connection, messageId);
            if (!row.operationId().equals(operationId) || !row.payloadHash().equals(payloadHash)) {
                connection.rollback();
                send(exchange, 409, Map.of("error", "idempotency-fingerprint-conflict"));
                return;
            }
            connection.commit();
        }
        if (timeoutAfterCommitOnce.compareAndSet(true, false)) {
            Thread.sleep(5000);
        }
        send(exchange, 200, acknowledgement(row, inserted ? "PROCESSED" : "DUPLICATE"));
    }

    private void find(HttpExchange exchange, UUID messageId) throws Exception {
        try (Connection connection = connection()) {
            InboxRow row = findOrNull(connection, messageId);
            if (row == null) {
                send(exchange, 404, Map.of("error", "not-found"));
            } else {
                send(exchange, 200, acknowledgement(row, "PROCESSED"));
            }
        }
    }

    private InboxRow find(Connection connection, UUID messageId) throws Exception {
        InboxRow row = findOrNull(connection, messageId);
        if (row == null) {
            throw new IllegalStateException("Committed inbox row was not found");
        }
        return row;
    }

    private InboxRow findOrNull(Connection connection, UUID messageId) throws Exception {
        try (var select = connection.prepareStatement("""
                select operation_id, payload_hash, acknowledged_at_utc
                from statement_inbox where message_id = ?
                """)) {
            select.setObject(1, messageId);
            try (ResultSet result = select.executeQuery()) {
                return result.next()
                        ? new InboxRow(messageId, result.getObject(1, UUID.class), result.getString(2),
                                result.getTimestamp(3).toInstant())
                        : null;
            }
        }
    }

    private Map<String, String> acknowledgement(InboxRow row, String status) {
        return Map.of(
                "messageId", row.messageId().toString(),
                "acknowledgedAtUtc", row.acknowledgedAtUtc().toString(),
                "status", status);
    }

    private void send(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] response = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
    }

    private SSLContext sslContext() throws Exception {
        char[] password = required("QL08_TLS_KEYSTORE_PASSWORD").toCharArray();
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(Path.of(required("QL08_TLS_KEYSTORE")))) {
            keyStore.load(input, password);
        }
        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keyStore, password);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers.getKeyManagers(), null, null);
        return context;
    }

    private Connection connection() throws Exception {
        Connection connection = DriverManager.getConnection(jdbcUrl, databaseUser, databasePassword);
        connection.setSchema(databaseSchema);
        return connection;
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String requiredIdentifier(String name) {
        String value = required(name);
        if (!value.matches("[a-z][a-z0-9_]{2,62}")) {
            throw new IllegalStateException(name + " must be a safe PostgreSQL identifier");
        }
        return value;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private record InboxRow(UUID messageId, UUID operationId, String payloadHash, Instant acknowledgedAtUtc) {
    }
}
