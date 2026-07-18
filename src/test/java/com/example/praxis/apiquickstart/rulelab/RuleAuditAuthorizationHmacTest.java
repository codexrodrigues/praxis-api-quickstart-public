package com.example.praxis.apiquickstart.rulelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RuleAuditAuthorizationHmacTest {
    private static final byte[] ACTIVE_SECRET =
            "fictional-active-adr12-secret-material-2026".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PREVIOUS_SECRET =
            "fictional-previous-adr12-secret-material-2025".getBytes(StandardCharsets.UTF_8);

    @Test
    void canonicalVectorRemainsStableAndUppercase() {
        var digest = RuleAuditAuthorizationHmac.compute(
                "ADR12-HMAC-2026-07",
                RuleAuditAuthorizationHmac.Purpose.RETENTION,
                "fictional-approval-reference-42",
                ACTIVE_SECRET);

        assertEquals("8ABE226A05A759853B2DEBD83657285356B0E0FE4DDD1A5A04FAB8AF3D7AD7D3",
                digest.digest());
        assertEquals("ADR12-HMAC-2026-07", digest.keyId());
        assertTrue(digest.digest().matches("[A-F0-9]{64}"));
    }

    @Test
    void rotationAndPurposeAreCryptographicallySeparated() {
        var active = RuleAuditAuthorizationHmac.compute(
                "ADR12-HMAC-2026-07",
                RuleAuditAuthorizationHmac.Purpose.RETENTION,
                "fictional-approval-reference-42",
                ACTIVE_SECRET);
        var repeated = RuleAuditAuthorizationHmac.compute(
                "ADR12-HMAC-2026-07",
                RuleAuditAuthorizationHmac.Purpose.RETENTION,
                "fictional-approval-reference-42",
                ACTIVE_SECRET);
        var previous = RuleAuditAuthorizationHmac.compute(
                "ADR12-HMAC-2025-12",
                RuleAuditAuthorizationHmac.Purpose.RETENTION,
                "fictional-approval-reference-42",
                PREVIOUS_SECRET);
        var legalHold = RuleAuditAuthorizationHmac.compute(
                "ADR12-HMAC-2026-07",
                RuleAuditAuthorizationHmac.Purpose.LEGAL_HOLD,
                "fictional-approval-reference-42",
                ACTIVE_SECRET);

        assertEquals(active, repeated);
        assertNotEquals(active.digest(), previous.digest());
        assertNotEquals(active.digest(), legalHold.digest());
    }

    @Test
    void weakOrAmbiguousInputsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> RuleAuditAuthorizationHmac.compute(
                "weak key id",
                RuleAuditAuthorizationHmac.Purpose.RETENTION,
                "reference",
                ACTIVE_SECRET));
        assertThrows(IllegalArgumentException.class, () -> RuleAuditAuthorizationHmac.compute(
                "ADR12-HMAC-2026-07",
                RuleAuditAuthorizationHmac.Purpose.RETENTION,
                " ",
                ACTIVE_SECRET));
        assertThrows(IllegalArgumentException.class, () -> RuleAuditAuthorizationHmac.compute(
                "ADR12-HMAC-2026-07",
                RuleAuditAuthorizationHmac.Purpose.RETENTION,
                "reference",
                new byte[31]));
        assertThrows(NullPointerException.class, () -> RuleAuditAuthorizationHmac.compute(
                "ADR12-HMAC-2026-07",
                null,
                "reference",
                ACTIVE_SECRET));
    }
}
