package com.example.praxis.apiquickstart.config;

import org.springframework.util.StringUtils;

import java.util.List;

public record DomainRuleWorkflowActionPolicy(
        String targetArtifactKey,
        String resourceKey,
        String actionId,
        List<String> requiredStates,
        String message
) {
    public boolean appliesToState(String state) {
        if (requiredStates == null || requiredStates.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(state)) {
            return false;
        }
        return requiredStates.stream()
                .filter(StringUtils::hasText)
                .anyMatch(requiredState -> requiredState.equalsIgnoreCase(state.trim()));
    }
}
