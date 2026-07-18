package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * HTTP adapter for the statement outbox contract.
 *
 * <p>The target is operator-controlled startup configuration. HTTP is rejected by default so that
 * production deployments cannot silently downgrade transport security.</p>
 */
@Component
@ConditionalOnProperty(name = "praxis.rule-lab.outbox.http.enabled", havingValue = "true")
public final class HttpExtraordinaryBenefitStatementEventSink
        implements ExtraordinaryBenefitStatementEventSink, ExtraordinaryBenefitStatementDeliveryProbe {
    static final String DELIVERY_PATH = "inbox/extraordinary-benefit-statements";
    private static final int MAXIMUM_ACKNOWLEDGEMENT_BYTES = 16 * 1024;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI deliveryUri;
    private final String bearerToken;
    private final Duration requestTimeout;

    public HttpExtraordinaryBenefitStatementEventSink(
            ObjectMapper objectMapper,
            @Value("${praxis.rule-lab.outbox.http.base-url}") String baseUrl,
            @Value("${praxis.rule-lab.outbox.http.bearer-token:}") String bearerToken,
            @Value("${praxis.rule-lab.outbox.http.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${praxis.rule-lab.outbox.http.request-timeout-ms:5000}") long requestTimeoutMs,
            @Value("${praxis.rule-lab.outbox.http.allow-insecure-http:false}") boolean allowInsecureHttp) {
        this.objectMapper = objectMapper;
        validateTimeout(connectTimeoutMs, "connect-timeout-ms");
        validateTimeout(requestTimeoutMs, "request-timeout-ms");
        URI validatedBaseUri = validateBaseUri(baseUrl, allowInsecureHttp);
        this.deliveryUri = appendPath(validatedBaseUri, DELIVERY_PATH);
        this.bearerToken = bearerToken == null ? "" : bearerToken.trim();
        this.requestTimeout = Duration.ofMillis(requestTimeoutMs);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public void deliver(ExtraordinaryBenefitStatementOutboxDelivery delivery) throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(delivery);
        HttpRequest request = requestBuilder(deliveryUri)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", delivery.messageId().toString())
                .header("X-Correlation-ID", delivery.correlationId())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (HttpTimeoutException failure) {
            throw ExtraordinaryBenefitStatementDeliveryFailure.transientFailure("HTTP_TIMEOUT");
        } catch (IOException failure) {
            throw ExtraordinaryBenefitStatementDeliveryFailure.transientFailure("HTTP_TRANSPORT_UNAVAILABLE");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw classifyDeliveryStatus(response.statusCode());
        }
        validateAcknowledgement(readBounded(response.body()), delivery.messageId());
    }

    @Override
    public Optional<ExtraordinaryBenefitStatementExternalAcknowledgement> findAcknowledgement(UUID messageId)
            throws Exception {
        URI acknowledgementUri = appendPath(deliveryUri, messageId.toString());
        HttpRequest request = requestBuilder(acknowledgementUri).GET().build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 404) {
            response.body().close();
            return Optional.empty();
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IOException("Consumer acknowledgement probe returned a non-success status");
        }
        return Optional.of(validateAcknowledgement(readBounded(response.body()), messageId));
    }

    private HttpRequest.Builder requestBuilder(URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json");
        if (!bearerToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return builder;
    }

    private ExtraordinaryBenefitStatementExternalAcknowledgement validateAcknowledgement(
            byte[] responseBody,
            UUID expectedMessageId) throws Exception {
        JsonNode acknowledgement;
        try {
            acknowledgement = objectMapper.readTree(responseBody);
        } catch (IOException invalidJson) {
            throw ExtraordinaryBenefitStatementDeliveryFailure.permanentFailure("HTTP_ACK_CONTRACT_INVALID");
        }
        UUID acknowledgedMessageId;
        Instant acknowledgedAt;
        try {
            acknowledgedMessageId = UUID.fromString(acknowledgement.path("messageId").asText());
            acknowledgedAt = Instant.parse(acknowledgement.path("acknowledgedAtUtc").asText());
        } catch (RuntimeException invalidContract) {
            throw ExtraordinaryBenefitStatementDeliveryFailure.permanentFailure("HTTP_ACK_CONTRACT_INVALID");
        }
        String status = acknowledgement.path("status").asText().toUpperCase(Locale.ROOT);
        if (!expectedMessageId.equals(acknowledgedMessageId)
                || !("PROCESSED".equals(status) || "DUPLICATE".equals(status))) {
            throw ExtraordinaryBenefitStatementDeliveryFailure.permanentFailure("HTTP_ACK_MISMATCH");
        }
        return new ExtraordinaryBenefitStatementExternalAcknowledgement(acknowledgedMessageId, acknowledgedAt);
    }

    private byte[] readBounded(InputStream responseBody)
            throws IOException, ExtraordinaryBenefitStatementDeliveryFailure {
        try (responseBody) {
            byte[] content = responseBody.readNBytes(MAXIMUM_ACKNOWLEDGEMENT_BYTES + 1);
            if (content.length > MAXIMUM_ACKNOWLEDGEMENT_BYTES) {
                throw ExtraordinaryBenefitStatementDeliveryFailure.permanentFailure("HTTP_ACK_TOO_LARGE");
            }
            return content;
        }
    }

    private static URI validateBaseUri(String value, boolean allowInsecureHttp) {
        URI uri;
        try {
            uri = URI.create(value == null ? "" : value.trim());
        } catch (IllegalArgumentException invalidUri) {
            throw new IllegalArgumentException("praxis.rule-lab.outbox.http.base-url must be a valid URI");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        boolean supportedScheme = "https".equals(scheme) || (allowInsecureHttp && "http".equals(scheme));
        if (!supportedScheme || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Outbox HTTP base URL must be an absolute HTTPS URI without credentials, query or fragment");
        }
        return uri;
    }

    private static URI appendPath(URI baseUri, String path) {
        String base = baseUri.toString();
        return URI.create((base.endsWith("/") ? base : base + "/") + path);
    }

    private static void validateTimeout(long value, String property) {
        if (value < 1 || value > Duration.ofMinutes(1).toMillis()) {
            throw new IllegalArgumentException(property + " must be between 1ms and 1m");
        }
    }

    private static ExtraordinaryBenefitStatementDeliveryFailure classifyDeliveryStatus(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return ExtraordinaryBenefitStatementDeliveryFailure.permanentFailure(
                    "HTTP_AUTHORIZATION_REJECTED");
        }
        if (statusCode == 408 || statusCode == 425 || statusCode == 429 || statusCode >= 500) {
            return ExtraordinaryBenefitStatementDeliveryFailure.transientFailure(
                    statusCode == 429 ? "HTTP_THROTTLED" : "HTTP_TARGET_UNAVAILABLE");
        }
        return ExtraordinaryBenefitStatementDeliveryFailure.permanentFailure("HTTP_CONTRACT_REJECTED");
    }
}
