package com.example.praxis.apiquickstart.auth;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.COMPOSITION_APPROVER;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.DEFINITION_APPROVER;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.DEFINITION_AUTHOR;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.SNAPSHOT_PUBLISHER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GovernanceLabIdentityServiceTest {
    @Test
    void shouldResolveSegregatedAuthoritiesOnlyWhenExplicitlyEnabled() {
        var service = new GovernanceLabIdentityService(
                true,
                "admin",
                "approver-a", "password-a",
                "approver-b", "password-b",
                "publisher", "password-p");
        service.validateConfiguration();

        var approver = service.authenticate("approver-a", "password-a").orElseThrow();
        var publisher = service.authenticate("publisher", "password-p").orElseThrow();

        assertEquals(java.util.Set.of(DEFINITION_APPROVER, COMPOSITION_APPROVER), approver.authorities());
        assertEquals(java.util.Set.of(DEFINITION_AUTHOR, SNAPSHOT_PUBLISHER), publisher.authorities());
        assertTrue(service.authenticate("approver-a", "wrong").isEmpty());
    }

    @Test
    void shouldFailStartupWhenMakerCheckerSubjectsAreNotDistinct() {
        var service = new GovernanceLabIdentityService(
                true,
                "admin",
                "same", "password-a",
                "same", "password-b",
                "publisher", "password-p");

        assertThrows(IllegalStateException.class, service::validateConfiguration);
    }

    @Test
    void shouldExposeNoLabIdentityWhileDisabled() {
        var service = new GovernanceLabIdentityService(false, "admin", "", "", "", "", "", "");
        service.validateConfiguration();
        Optional<GovernanceLabIdentityService.AuthenticatedIdentity> result =
                service.authenticate("approver-a", "password-a");
        assertTrue(result.isEmpty());
    }
}
