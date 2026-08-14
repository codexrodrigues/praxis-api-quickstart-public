package com.example.praxis.apiquickstart.rulelab.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PolicyStudioSandboxRunResponse(
        UUID runId,
        UUID workspaceId,
        long workspaceRevision,
        String baseDefinitionHash,
        Instant evaluatedAtUtc,
        String userTimeZone,
        String activeSnapshotKey,
        String activeSnapshotContentHash,
        long activeActivationRevision,
        List<PolicyStudioSandboxScenarioResult> results) {
}
