package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RuleFactScopeDigesterTest {
    private static final RuleFactLookup LOOKUP = new RuleFactLookup(
            "desenv", "local", "DEMO-ORG", "FACT-001", Instant.parse("2026-07-16T12:00:00Z"));

    @Test
    void producesStableKeyedDigestWithoutPublishingRawScope() {
        var first = new RuleFactScopeDigester("first-test-only-scope-hmac-key-at-least-32-bytes");
        var second = new RuleFactScopeDigester("second-test-only-scope-hmac-key-at-least-32-bytes");

        assertThat(first.digest(LOOKUP)).matches("[0-9A-F]{64}");
        assertThat(first.digest(LOOKUP)).isEqualTo(first.digest(LOOKUP));
        assertThat(first.digest(LOOKUP)).isNotEqualTo(second.digest(LOOKUP));
        assertThat(first.digest(LOOKUP)).doesNotContain("DEMO", "FACT");
    }

    @Test
    void rejectsMissingOrWeakKeys() {
        assertThatThrownBy(() -> new RuleFactScopeDigester("short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32");
    }
}
