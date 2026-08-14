package com.example.praxis.apiquickstart.rulelab.dto;

import java.util.List;
import java.util.UUID;

public record PolicyStudioSandboxRunRequest(
        UUID workspaceId,
        List<UUID> scenarioIds,
        String userTimeZone) {
}
