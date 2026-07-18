package com.example.praxis.apiquickstart.rulelab;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Produz um identificador correlacionavel sem publicar o escopo empresarial em claro. */
final class RuleFactScopeDigester {
    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    RuleFactScopeDigester(String secret) {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("rule fact scope HMAC key must contain at least 32 UTF-8 bytes");
        }
        this.key = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    String digest(RuleFactLookup lookup) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            String identity = String.join("\u0000", lookup.tenantId(), lookup.environment(),
                    lookup.organizationKey(), lookup.factReference());
            return HexFormat.of().withUpperCase()
                    .formatHex(mac.doFinal(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException unavailable) {
            throw new IllegalStateException("HmacSHA256 is unavailable", unavailable);
        }
    }
}
