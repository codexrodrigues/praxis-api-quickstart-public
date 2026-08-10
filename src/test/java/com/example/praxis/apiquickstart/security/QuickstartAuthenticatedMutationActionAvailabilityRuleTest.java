package com.example.praxis.apiquickstart.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.action.ActionAvailabilityContext;
import org.praxisplatform.uischema.action.ActionDefinition;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.openapi.CanonicalOperationRef;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickstartAuthenticatedMutationActionAvailabilityRuleTest {

    private final QuickstartAuthenticatedMutationActionAvailabilityRule rule =
            new QuickstartAuthenticatedMutationActionAvailabilityRule();

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotAdvertiseMutatingWorkflowActionsToAnonymousSessions() {
        var decision = rule.evaluate(postAction(), actionContext());

        assertFalse(decision.allowed());
        assertEquals("authentication-required", decision.reason());
    }

    @Test
    void shouldAdvertiseMutatingWorkflowActionsToAuthenticatedSessions() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        assertTrue(rule.evaluate(postAction(), actionContext()).allowed());
    }

    private ActionDefinition postAction() {
        return new ActionDefinition(
                "deactivate",
                "human-resources.funcionarios",
                "/api/human-resources/funcionarios",
                "api-human-resources-funcionarios",
                ActionScope.ITEM,
                "Inativar funcionário",
                "Inativa o vínculo do funcionário.",
                new CanonicalOperationRef(
                        "api-human-resources-funcionarios",
                        "deactivate",
                        "/api/human-resources/funcionarios/{id}/actions/deactivate",
                        "POST"
                ),
                null,
                null,
                0,
                "Funcionário inativado",
                List.of(),
                List.of("ATIVO"),
                List.of(),
                null
        );
    }

    private ActionAvailabilityContext actionContext() {
        return new ActionAvailabilityContext(
                "human-resources.funcionarios",
                "/api/human-resources/funcionarios",
                1,
                null,
                null,
                null,
                Set.of(),
                null
        );
    }
}
