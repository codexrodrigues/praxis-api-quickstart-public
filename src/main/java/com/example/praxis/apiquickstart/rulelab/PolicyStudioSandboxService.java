package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioSandboxRunRequest;
import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioSandboxRunResponse;
import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioSandboxScenarioResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.praxisplatform.config.dto.DomainRuleChangeWorkspaceResponse;
import org.praxisplatform.config.dto.DomainRuleTestScenarioResponse;
import org.praxisplatform.config.service.DomainRuleChangeWorkspaceService;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleTestRunService;
import org.praxisplatform.config.dto.DomainRuleTestRunRecordRequest;
import org.praxisplatform.config.dto.DomainRuleTestRunResultRequest;
import org.praxisplatform.config.dto.DomainRuleTestBaselineEvidence;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.plan.PraxisRulePlanCompiler;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.praxisplatform.rules.runtime.PraxisRuleSetEngine;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Read-only candidate versus active sandbox for the neutral Quickstart policy case. */
public class PolicyStudioSandboxService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final DomainRuleChangeWorkspaceService workspaceService;
    private final ExtraordinaryGrantRuleLabService activeService;
    private final ExtraordinaryGrantRuleSnapshotRuntime activeRuntime;
    private final DomainRuleTestRunService testRunService;
    private final PraxisRulePlanCompiler candidateCompiler;
    private final PraxisRuleSetEngine candidateEngine;
    private final Clock clock;

    public PolicyStudioSandboxService(
            DomainRuleChangeWorkspaceService workspaceService,
            ExtraordinaryGrantRuleLabService activeService,
            ExtraordinaryGrantRuleSnapshotRuntime activeRuntime,
            DomainRuleTestRunService testRunService,
            RuleBindingExecutorRegistry registry,
            Clock clock) {
        this.workspaceService = Objects.requireNonNull(workspaceService, "workspaceService is required");
        this.activeService = Objects.requireNonNull(activeService, "activeService is required");
        this.activeRuntime = Objects.requireNonNull(activeRuntime, "activeRuntime is required");
        this.testRunService = Objects.requireNonNull(testRunService, "testRunService is required");
        RuleBindingExecutorRegistry trustedRegistry = Objects.requireNonNull(registry, "registry is required");
        this.candidateCompiler = new PraxisRulePlanCompiler(trustedRegistry);
        this.candidateEngine = new PraxisRuleSetEngine(trustedRegistry);
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public PolicyStudioSandboxRunResponse run(
            PolicyStudioSandboxRunRequest request,
            DomainRuleGovernancePrincipal principal) {
        PolicyStudioSandboxPreparedRun prepared = prepare(request, principal);
        var recorded = testRunService.record(
                prepared.workspaceId(), prepared.recordRequest(), principal);
        return prepared.response(recorded.runId());
    }

    /** Prepara a mesma avaliacao do sandbox sem persistir, para enriquecimento operacional explicito. */
    PolicyStudioSandboxPreparedRun prepare(
            PolicyStudioSandboxRunRequest request,
            DomainRuleGovernancePrincipal principal) {
        if (request == null || request.workspaceId() == null) {
            throw badRequest("workspaceId is required");
        }
        ZoneId timeZone = parseTimeZone(request.userTimeZone());
        DomainRuleChangeWorkspaceResponse workspace = workspaceService.get(request.workspaceId(), principal);
        if (!"OPEN".equals(workspace.status())) {
            throw conflict("Only an OPEN workspace can be evaluated");
        }
        List<DomainRuleTestScenarioResponse> selected = selectScenarios(
                workspaceService.scenarios(workspace.id(), principal), request.scenarioIds());
        if (selected.isEmpty()) {
            throw badRequest("At least one ACTIVE scenario is required");
        }
        RuleDecisionPlan candidatePlan = compileCandidate(workspace);
        Instant frozenNow = clock.instant();
        ExtraordinaryGrantRuleSnapshotSession activeSession = captureActive(frozenNow);
        List<PolicyStudioSandboxScenarioResult> results = selected.stream()
                .map(scenario -> evaluate(scenario, candidatePlan, activeSession, frozenNow, timeZone))
                .toList();
        ActiveEvidence activeEvidence = activeSession == null
                ? new ActiveEvidence(null, null, 0)
                : new ActiveEvidence(activeSession.snapshotKey(), activeSession.snapshotContentHash(),
                        activeSession.activationRevision());
        var recordRequest = new DomainRuleTestRunRecordRequest(
                workspace.revision(), workspace.baseDefinitionHash(), frozenNow, timeZone.getId(),
                activeEvidence.snapshotKey(), activeEvidence.snapshotContentHash(), activeEvidence.activationRevision(),
                new DomainRuleTestBaselineEvidence(
                        "SYNTHETIC_EXPECTED",
                        "config:workspace:" + workspace.id() + ":scenarios@revision:" + workspace.revision(),
                        scenarioExpectationDigest(selected), frozenNow, "ELIGIBLE"),
                results.stream().map(item -> new DomainRuleTestRunResultRequest(
                        item.scenarioId(), item.scenarioKey(), item.candidateDecision(), item.activeDecision(),
                        item.candidateOutput(), item.activeOutput(), item.candidateReasonCodes(), item.activeReasonCodes(),
                        item.candidateEffectIntents(), item.activeEffectIntents(),
                        item.candidatePlanDigest(), item.activePlanDigest(), item.factsDigest())).toList());
        return new PolicyStudioSandboxPreparedRun(workspace.id(), recordRequest, results);
    }

    private String scenarioExpectationDigest(List<DomainRuleTestScenarioResponse> scenarios) {
        String identity = scenarios.stream()
                .map(item -> item.id() + "|" + item.scenarioKey() + "|" + item.expectedDecision()
                        + "|" + Objects.toString(item.expectedOutput(), "null")
                        + "|" + normalized(item.expectedReasonCodes())
                        + "|" + normalized(item.expectedEffectIntents()))
                .sorted()
                .collect(Collectors.joining("\n"));
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 must be available", unavailable);
        }
    }

    private PolicyStudioSandboxScenarioResult evaluate(
            DomainRuleTestScenarioResponse scenario,
            RuleDecisionPlan candidatePlan,
            ExtraordinaryGrantRuleSnapshotSession activeSession,
            Instant frozenNow,
            ZoneId timeZone) {
        RuleEvaluationResult candidate = safeCandidate(candidatePlan, scenario.facts(), frozenNow, timeZone);
        RuleEvaluationResult active = safeActive(activeSession, candidatePlan, scenario.facts(), frozenNow, timeZone);
        String expected = scenario.expectedDecision();
        String candidateDecision = candidate.decision().name();
        String activeDecision = active.decision().name();
        JsonNode candidateOutput = output(candidate);
        JsonNode activeOutput = output(active);
        List<String> expectedReasons = normalized(scenario.expectedReasonCodes());
        List<String> candidateReasons = normalized(candidate.reasonCodes());
        List<String> activeReasons = normalized(active.reasonCodes());
        List<String> expectedEffects = normalized(scenario.expectedEffectIntents());
        List<String> candidateEffects = effectIntents(candidate);
        List<String> activeEffects = effectIntents(active);
        return new PolicyStudioSandboxScenarioResult(
                scenario.id(), scenario.scenarioKey(), expected, candidateDecision, activeDecision,
                comparison(candidateDecision, activeDecision),
                candidateDecision.equals(expected), activeDecision.equals(expected),
                scenario.expectedOutput(), candidateOutput, activeOutput,
                scenario.expectedOutput() == null || Objects.equals(scenario.expectedOutput(), candidateOutput),
                scenario.expectedOutput() == null || Objects.equals(scenario.expectedOutput(), activeOutput),
                expectedReasons, candidateReasons, activeReasons,
                candidateReasons.equals(expectedReasons), activeReasons.equals(expectedReasons),
                expectedEffects, candidateEffects, activeEffects,
                candidateEffects.equals(expectedEffects), activeEffects.equals(expectedEffects),
                candidate.planDigest(), active.planDigest(),
                candidate.factsDigest());
    }

    private JsonNode output(RuleEvaluationResult result) {
        ObjectNode output = JSON.createObjectNode();
        result.bindingResults().stream()
                .filter(binding -> binding.output() != null && !binding.output().isNull())
                .forEach(binding -> output.set(binding.bindingKey(), binding.output()));
        return output.isEmpty() ? null : output;
    }

    private List<String> effectIntents(RuleEvaluationResult result) {
        return result.bindingResults().stream().map(binding -> binding.output())
                .filter(Objects::nonNull)
                .filter(output -> "PLANNED_NOT_EXECUTED".equals(output.path("status").asText()))
                .map(output -> output.path("intentType").asText())
                .filter(value -> !value.isBlank()).distinct().sorted().toList();
    }

    private List<String> normalized(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull)
                .map(String::trim).filter(value -> !value.isBlank()).distinct().sorted().toList();
    }

    private String comparison(String candidate, String active) {
        if ("TECHNICAL_ERROR".equals(candidate) || "TECHNICAL_ERROR".equals(active)) {
            return "TECHNICAL_ERROR";
        }
        if ("INCONCLUSIVE".equals(candidate) || "INCONCLUSIVE".equals(active)) {
            return "INCONCLUSIVE";
        }
        return candidate.equals(active) ? "MATCH" : "MISMATCH";
    }

    private RuleDecisionPlan compileCandidate(DomainRuleChangeWorkspaceResponse workspace) {
        if (workspace.condition() == null || !workspace.condition().isObject()) {
            throw unprocessable("Workspace condition must be a JSON Logic object");
        }
        RuleSetDefinition baseline = ExtraordinaryGrantRuleSetFactory.definition();
        boolean replaceable = ExtraordinaryGrantRuleSetComposer.governedBindings(baseline).stream()
                .anyMatch(binding -> binding.bindingKey().equals(workspace.ruleKey()));
        if (!replaceable) {
            throw unprocessable("Workspace ruleKey is not a replaceable binding in this host RuleSet");
        }
        try {
            RuleSetDefinition materialized = ExtraordinaryGrantRuleSetComposer.withCondition(
                    baseline, workspace.ruleKey(), workspace.condition());
            return candidateCompiler.compile(materialized);
        } catch (RuntimeException exception) {
            throw unprocessable("Candidate RuleSet could not be compiled: " + safeCode(exception));
        }
    }

    private RuleEvaluationResult safeCandidate(
            RuleDecisionPlan plan, JsonNode facts, Instant now, ZoneId timeZone) {
        try {
            return candidateEngine.evaluate(plan, facts, now.toString(), timeZone.getId());
        } catch (RuntimeException exception) {
            return technicalResult(plan, facts, safeCode(exception));
        }
    }

    private RuleEvaluationResult safeActive(
            ExtraordinaryGrantRuleSnapshotSession session,
            RuleDecisionPlan candidatePlan,
            JsonNode facts,
            Instant now,
            ZoneId timeZone) {
        if (session == null) {
            return technicalResult(candidatePlan, facts, "ACTIVE_SNAPSHOT_UNAVAILABLE");
        }
        try {
            return activeService.evaluateSandboxWithSnapshot(session, facts, now, timeZone).result();
        } catch (RuntimeException exception) {
            return technicalResult(candidatePlan, facts, safeCode(exception));
        }
    }

    private RuleEvaluationResult technicalResult(RuleDecisionPlan plan, JsonNode facts, String code) {
        return new RuleEvaluationResult(
                RuleDecision.TECHNICAL_ERROR, plan.definition().ref(), plan.planDigest(),
                List.of(), List.of(code), "0".repeat(64), plan.definition().compatibility(),
                plan.implementationRefs(), plan.definition().failPolicy(), List.of());
    }

    private ExtraordinaryGrantRuleSnapshotSession captureActive(Instant now) {
        try {
            return activeRuntime.captureSnapshot(now);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private List<DomainRuleTestScenarioResponse> selectScenarios(
            List<DomainRuleTestScenarioResponse> scenarios, List<UUID> requestedIds) {
        Set<UUID> requested = requestedIds == null ? Set.of() : requestedIds.stream().collect(Collectors.toUnmodifiableSet());
        return scenarios.stream()
                .filter(item -> "ACTIVE".equals(item.status()))
                .filter(item -> requested.isEmpty() || requested.contains(item.id()))
                .toList();
    }

    private ZoneId parseTimeZone(String value) {
        try {
            return ZoneId.of(value == null || value.isBlank() ? "UTC" : value.trim());
        } catch (RuntimeException exception) {
            throw badRequest("userTimeZone is invalid");
        }
    }

    private String safeCode(RuntimeException exception) {
        return exception.getClass().getSimpleName().toUpperCase();
    }

    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private ResponseStatusException unprocessable(String message) { return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message); }

    private record ActiveEvidence(String snapshotKey, String snapshotContentHash, long activationRevision) {}

    record PolicyStudioSandboxPreparedRun(
            UUID workspaceId,
            DomainRuleTestRunRecordRequest recordRequest,
            List<PolicyStudioSandboxScenarioResult> results) {
        PolicyStudioSandboxRunResponse response(UUID runId) {
            return new PolicyStudioSandboxRunResponse(
                    runId, workspaceId, recordRequest.workspaceRevision(), recordRequest.baseDefinitionHash(),
                    recordRequest.evaluatedAtUtc(), recordRequest.userTimeZone(),
                    recordRequest.activeSnapshotKey(), recordRequest.activeSnapshotContentHash(),
                    recordRequest.activeActivationRevision(), results);
        }
    }
}
