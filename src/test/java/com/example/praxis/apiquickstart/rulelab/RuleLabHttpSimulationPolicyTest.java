package com.example.praxis.apiquickstart.rulelab;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RuleLabHttpSimulationPolicyTest {
    @Test
    void remainsDisabledUnlessExplicitlyEnabled() {
        var policy = new RuleLabHttpSimulationPolicy(false, new MockEnvironment());
        assertFalse(policy.available());
        assertThrows(RuntimeException.class, policy::requireAvailable);
    }

    @Test
    void permitsExplicitNonProductionLaboratoryUse() {
        var policy = new RuleLabHttpSimulationPolicy(true, new MockEnvironment().withProperty("mode", "test"));
        assertDoesNotThrow(policy::requireAvailable);
    }

    @Test
    void productionCannotEnableCallerSuppliedFacts() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        var policy = new RuleLabHttpSimulationPolicy(true, environment);
        assertFalse(policy.available());
        assertThrows(RuntimeException.class, policy::requireAvailable);
    }
}
