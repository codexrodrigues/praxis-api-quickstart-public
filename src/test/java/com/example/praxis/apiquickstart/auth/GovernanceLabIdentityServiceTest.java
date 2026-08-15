package com.example.praxis.apiquickstart.auth;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.COMPOSITION_APPROVER;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.DEFINITION_APPROVER;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.DEFINITION_AUTHOR;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.DEFINITION_READER;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.SNAPSHOT_PUBLISHER;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.SNAPSHOT_OPERATOR;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.SNAPSHOT_READER;
import static com.example.praxis.apiquickstart.security.RuleGovernanceAuthorities.OPERATIONAL_TEST_OPERATOR;
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

        assertEquals(java.util.Set.of(DEFINITION_READER, DEFINITION_APPROVER, COMPOSITION_APPROVER), approver.authorities());
        assertEquals(
                java.util.Set.of(DEFINITION_READER, DEFINITION_AUTHOR, SNAPSHOT_PUBLISHER,
                        SNAPSHOT_OPERATOR, SNAPSHOT_READER, OPERATIONAL_TEST_OPERATOR),
                publisher.authorities());
        assertTrue(service.authenticate("approver-a", "wrong").isEmpty());
        assertEquals("approver-b", service.switchIdentity("approver-b", "publisher").orElseThrow().subject());
        assertEquals("publisher", service.switchIdentity("publisher", "approver-a").orElseThrow().subject());
        assertTrue(service.switchIdentity("publisher", "admin").isEmpty());
        assertTrue(service.switchIdentity("unknown", "publisher").isEmpty());
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
        assertTrue(service.switchIdentity("publisher", "approver-a").isEmpty());
    }
}
