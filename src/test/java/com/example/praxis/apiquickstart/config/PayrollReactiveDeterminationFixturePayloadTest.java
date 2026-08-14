package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PayrollReactiveDeterminationFixturePayloadTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void derivesBothOrderedDeterminationsFromTheCanonicalHostRuleSet() throws Exception {
        var payload = json.readTree(PayrollReactiveDeterminationFixturePayload.serialize(
                "00000000-0000-0000-0000-000000000001",
                "00000000-0000-0000-0000-000000000002",
                2,
                Instant.parse("2026-08-13T12:00:00Z")));

        assertThat(payload.path("ownerServiceKey").asText()).isEqualTo("praxis-api-quickstart");
        assertThat(payload.path("requiredHostContractVersion").asText()).isEqualTo("quickstart/1.0");
        assertThat(payload.path("sourceDefinitionIds")).hasSize(2);
        assertThat(payload.path("ruleSet").path("ref").path("version").asInt()).isEqualTo(2);
        assertThat(payload.path("ruleSet").path("bindings")).hasSize(2);
        assertThat(payload.path("ruleSet").path("bindings").get(1).path("dependsOn").get(0).asText())
                .isEqualTo("human-resources.payroll.net-salary");
        assertThat(payload.path("validFromUtc").asText()).isEqualTo("2026-08-13T11:00:00Z");
        assertThat(payload.path("validUntilUtc").asText()).isEqualTo("2026-08-14T12:00:00Z");
    }
}
