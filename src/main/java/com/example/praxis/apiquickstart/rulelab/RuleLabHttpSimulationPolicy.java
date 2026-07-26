package com.example.praxis.apiquickstart.rulelab;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Fail-closed boundary for HTTP facts that are still supplied by the laboratory caller. */
@Component
final class RuleLabHttpSimulationPolicy {
    private final boolean available;

    RuleLabHttpSimulationPolicy(
            @Value("${praxis.rule-lab.http-simulation-enabled:false}") boolean enabled,
            Environment environment) {
        boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        this.available = enabled && !production;
    }

    void requireAvailable() {
        if (!available) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "Rule Lab HTTP simulation is unavailable: an authoritative FactProvider is required.");
        }
    }

    boolean available() {
        return available;
    }
}
