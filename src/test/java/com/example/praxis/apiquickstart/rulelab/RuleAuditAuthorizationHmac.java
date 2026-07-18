package com.example.praxis.apiquickstart.rulelab;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Laboratory reference for the provider-neutral ADR-12 authorization HMAC contract.
 * Production secrets remain owned and injected by the target environment's secret manager.
 */
final class RuleAuditAuthorizationHmac {
    private static final byte[] DOMAIN = "PRAXIS-RULE-AUDIT-AUTHORIZATION-V1"
            .getBytes(StandardCharsets.US_ASCII);
    private static final Pattern KEY_ID = Pattern.compile("[A-Z0-9][A-Z0-9._:-]{2,79}");
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final int MAXIMUM_REFERENCE_BYTES = 1024;

    private RuleAuditAuthorizationHmac() {
    }

    static AuthorizationDigest compute(
            String keyId,
            Purpose purpose,
            String authorizationReference,
            byte[] secret) {
        require(keyId != null && KEY_ID.matcher(keyId).matches(),
                "keyId must be a bounded uppercase opaque version identifier");
        Objects.requireNonNull(purpose, "purpose is required");
        require(authorizationReference != null && !authorizationReference.isBlank(),
                "authorizationReference is required");
        byte[] reference = authorizationReference.getBytes(StandardCharsets.UTF_8);
        require(reference.length <= MAXIMUM_REFERENCE_BYTES,
                "authorizationReference exceeds the canonical byte limit");
        require(secret != null && secret.length >= MINIMUM_SECRET_BYTES,
                "secret must contain at least 256 bits");

        byte[] keyMaterial = secret.clone();
        byte[] payload = null;
        try {
            payload = canonicalPayload(keyId, purpose.name(), reference);
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(keyMaterial, "HmacSHA256"));
            return new AuthorizationDigest(
                    keyId,
                    HexFormat.of().withUpperCase().formatHex(hmac.doFinal(payload)));
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("HmacSHA256 is unavailable", failure);
        } finally {
            Arrays.fill(keyMaterial, (byte) 0);
            Arrays.fill(reference, (byte) 0);
            if (payload != null) {
                Arrays.fill(payload, (byte) 0);
            }
        }
    }

    private static byte[] canonicalPayload(String keyId, String purpose, byte[] reference) {
        byte[] key = keyId.getBytes(StandardCharsets.US_ASCII);
        byte[] use = purpose.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer payload = ByteBuffer.allocate(
                Integer.BYTES + DOMAIN.length
                        + Integer.BYTES + key.length
                        + Integer.BYTES + use.length
                        + Integer.BYTES + reference.length);
        put(payload, DOMAIN);
        put(payload, key);
        put(payload, use);
        put(payload, reference);
        return payload.array();
    }

    private static void put(ByteBuffer target, byte[] value) {
        target.putInt(value.length);
        target.put(value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    enum Purpose {
        LEGAL_HOLD,
        RETENTION
    }

    record AuthorizationDigest(String keyId, String digest) {
        AuthorizationDigest {
            Objects.requireNonNull(keyId, "keyId is required");
            require(digest != null && digest.matches("[A-F0-9]{64}"),
                    "digest must be uppercase HMAC-SHA-256");
        }
    }
}
