package com.example.praxis.apiquickstart.config;

import org.springframework.util.StringUtils;

import java.util.List;

public record DomainRuleBackendValidationPolicy(
        String targetArtifactKey,
        String referenceResourceKey,
        String referenceField,
        String statusPropertyPath,
        List<String> blockedStatuses,
        String validationMessageTemplate
) {
    public boolean blocksStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        return blockedStatuses.stream()
                .filter(StringUtils::hasText)
                .anyMatch(blockedStatus -> blockedStatus.equalsIgnoreCase(status.trim()));
    }
}
