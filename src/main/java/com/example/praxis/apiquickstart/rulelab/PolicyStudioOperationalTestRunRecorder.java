package com.example.praxis.apiquickstart.rulelab;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.praxisplatform.config.dto.DomainRuleOperationalTestEvidence;
import org.praxisplatform.config.dto.DomainRuleTestRunRecordRequest;
import org.praxisplatform.config.dto.DomainRuleTestRunResponse;
import org.praxisplatform.config.dto.DomainRuleTestRunResultRequest;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleTestRunService;
import org.springframework.stereotype.Service;

/** Anexa evidencia host-owned a um resultado avaliado e delega a persistencia ao owner Config. */
@Service
class PolicyStudioOperationalTestRunRecorder {
    private final DomainRuleTestRunService testRunService;

    PolicyStudioOperationalTestRunRecorder(DomainRuleTestRunService testRunService) {
        this.testRunService = testRunService;
    }

    DomainRuleTestRunResponse record(
            UUID workspaceId,
            DomainRuleTestRunRecordRequest evaluatedRun,
            Map<UUID, DomainRuleOperationalTestEvidence> evidenceByScenario,
            DomainRuleGovernancePrincipal principal) {
        Objects.requireNonNull(workspaceId, "workspaceId is required");
        Objects.requireNonNull(evaluatedRun, "evaluatedRun is required");
        Objects.requireNonNull(evidenceByScenario, "evidenceByScenario is required");
        List<DomainRuleTestRunResultRequest> source = Objects.requireNonNull(
                evaluatedRun.results(), "evaluatedRun.results is required");
        Set<UUID> resultIds = new HashSet<>();
        source.forEach(result -> {
            if (result == null || result.scenarioId() == null || !resultIds.add(result.scenarioId())) {
                throw new IllegalArgumentException("Evaluated results require unique scenario ids");
            }
            if (result.operationalEvidence() != null) {
                throw new IllegalArgumentException("Evaluated result is already enriched with operational evidence");
            }
        });
        if (source.isEmpty() || !resultIds.equals(evidenceByScenario.keySet())
                || evidenceByScenario.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Operational evidence must cover exactly every evaluated scenario");
        }

        List<DomainRuleTestRunResultRequest> enriched = source.stream()
                .map(result -> enrich(result, evidenceByScenario.get(result.scenarioId())))
                .toList();
        var record = new DomainRuleTestRunRecordRequest(
                evaluatedRun.workspaceRevision(), evaluatedRun.baseDefinitionHash(),
                evaluatedRun.evaluatedAtUtc(), evaluatedRun.userTimeZone(),
                evaluatedRun.activeSnapshotKey(), evaluatedRun.activeSnapshotContentHash(),
                evaluatedRun.activeActivationRevision(), evaluatedRun.baselineEvidence(), enriched);
        return testRunService.record(workspaceId, record, principal);
    }

    private DomainRuleTestRunResultRequest enrich(
            DomainRuleTestRunResultRequest result,
            DomainRuleOperationalTestEvidence evidence) {
        return new DomainRuleTestRunResultRequest(
                result.scenarioId(), result.scenarioKey(), result.candidateDecision(), result.activeDecision(),
                result.candidateOutput(), result.activeOutput(), result.candidateReasonCodes(),
                result.activeReasonCodes(), result.candidateEffectIntents(), result.activeEffectIntents(),
                result.candidatePlanDigest(), result.activePlanDigest(), result.factsDigest(), evidence);
    }
}
