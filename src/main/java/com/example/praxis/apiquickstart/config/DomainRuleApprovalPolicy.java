package com.example.praxis.apiquickstart.config;

import org.springframework.util.StringUtils;

import java.util.List;

public record DomainRuleApprovalPolicy(
        String targetArtifactKey,
        String resourceKey,
        String actionId,
        List<String> requiredApprovals,
        List<String> approvalGroups,
        String approverContext,
        String message
) {
    public String effectiveMessage() {
        return StringUtils.hasText(message)
                ? message
                : "Action requires approval by published semantic decision";
    }
}
