package com.example.praxis.apiquickstart.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacao local de JWT usada pelo quickstart.
 *
 * <p>O objetivo desta classe nao e competir com uma stack completa de identidade, e sim oferecer
 * um mecanismo transparente e legivel para o projeto de exemplo: gerar um token assinado com papel
 * e authorities, validar expiracao e devolver um resultado simples para os filtros do host.</p>
 */
@Component
public class JwtTokenService {
    private static final Base64.Encoder B64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtTokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.exp-min}") String expMinutes
    ) {
        this.objectMapper = new ObjectMapper();
        this.secret = (secret == null ? "dev-secret-change-me" : secret).getBytes(StandardCharsets.UTF_8);
        long minutes;
        try {
            minutes = Long.parseLong((expMinutes == null || expMinutes.isBlank()) ? "60" : expMinutes.trim());
        } catch (NumberFormatException e) {
            minutes = 60;
        }
        this.expirationSeconds = Math.max(60, minutes * 60); // mínimo 60s
    }

    public String generate(String subject, String role) {
        return generate(subject, role, List.of());
    }

    /** Gera um JWT HS256 simples com subject, role, authorities e expiracao. */
    public String generate(String subject, String role, Collection<String> authorities) {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            long now = Instant.now().getEpochSecond();
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", subject);
            payload.put("role", role);
            payload.put("authorities", normalizeAuthorities(authorities));
            payload.put("iat", now);
            payload.put("exp", now + expirationSeconds);

            String headerStr = B64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(header));
            String payloadStr = B64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String signingInput = headerStr + "." + payloadStr;
            String signature = sign(signingInput);
            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWT", e);
        }
    }

    /** Valida assinatura, expiracao e claims minimas do token publicado pelo quickstart. */
    public JwtValidationResult validate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return JwtValidationResult.invalid("invalid_token_format");
            }
            String signingInput = parts[0] + "." + parts[1];
            String expectedSig = sign(signingInput);
            if (!constantTimeEquals(expectedSig, parts[2])) {
                return JwtValidationResult.invalid("invalid_signature");
            }
            byte[] payloadBytes = B64_URL_DECODER.decode(parts[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, new TypeReference<Map<String, Object>>(){});
            Object sub = payload.get("sub");
            Object exp = payload.get("exp");
            if (sub == null || exp == null) {
                return JwtValidationResult.invalid("missing_claims");
            }
            long expSec = (exp instanceof Number) ? ((Number) exp).longValue() : Long.parseLong(String.valueOf(exp));
            if (Instant.now().getEpochSecond() > expSec) {
                return JwtValidationResult.invalid("token_expired");
            }
            String role = String.valueOf(payload.getOrDefault("role", "USER"));
            List<String> authorities = normalizeAuthorities(payload.get("authorities"));
            return JwtValidationResult.valid(String.valueOf(sub), role, authorities);
        } catch (Exception e) {
            return JwtValidationResult.invalid("validation_error");
        }
    }

    /** Normaliza authorities de entrada ou de claims para uma lista limpa e sem duplicidade. */
    private List<String> normalizeAuthorities(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    /** Assina o header.payload usando HS256 e a chave local do host. */
    private String sign(String signingInput) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        byte[] sig = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        return B64_URL_ENCODER.encodeToString(sig);
    }

    /** Evita comparacao curta para nao vazar diferencas triviais de assinatura. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /** Resultado tipado da validacao consumido pelos filtros do host. */
    public record JwtValidationResult(boolean valid, String subject, String role, List<String> authorities, String reason) {
        public static JwtValidationResult valid(String subject, String role) {
            return new JwtValidationResult(true, subject, role, List.of(), null);
        }

        public static JwtValidationResult valid(String subject, String role, List<String> authorities) {
            return new JwtValidationResult(true, subject, role, authorities == null ? List.of() : List.copyOf(authorities), null);
        }

        public static JwtValidationResult invalid(String reason) {
            return new JwtValidationResult(false, null, null, List.of(), reason);
        }
    }

    public long getExpirationSeconds() { return expirationSeconds; }
}
