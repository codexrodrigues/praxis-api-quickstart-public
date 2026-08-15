package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.core.entity.ResourceActionExecution;
import com.example.praxis.apiquickstart.core.service.ResourceActionExecutionService;
import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioOperationalRunRequest;
import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioOperationalScenarioSelection;
import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioSandboxRunRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.praxisplatform.config.contract.DomainRuleTestRunResponse;
import org.praxisplatform.config.dto.DomainRuleChangeWorkspaceResponse;
import org.praxisplatform.config.http.HttpEntityTagCondition;
import org.praxisplatform.config.service.DomainRuleChangeWorkspaceService;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.uischema.action.ActionScope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Publishes the internal operational executor as a preconditioned host command. */
@Service
final class PolicyStudioOperationalTestRunCommandService {
    private static final String RESOURCE_KEY = "human-resources.extraordinary-benefit-requests";
    private static final String ACTION_ID = "run-policy-studio-operational-test";
    private static final Set<String> SUPPORTED_OPERATION_MODES = Set.of("CREATE", "UPDATE");

    private final DomainRuleChangeWorkspaceService workspaceService;
    private final PolicyStudioOperationalTestRunPort executor;
    private final ExtraordinaryBenefitOperationalScenarioBindingFactory bindingFactory;
    private final ResourceActionExecutionService actionExecutionService;
    private final ObjectMapper objectMapper;

    PolicyStudioOperationalTestRunCommandService(
            DomainRuleChangeWorkspaceService workspaceService,
            PolicyStudioOperationalTestRunPort executor,
            ExtraordinaryBenefitOperationalScenarioBindingFactory bindingFactory,
            ResourceActionExecutionService actionExecutionService,
            ObjectMapper objectMapper) {
        this.workspaceService = workspaceService;
        this.executor = executor;
        this.bindingFactory = bindingFactory;
        this.actionExecutionService = actionExecutionService;
        this.objectMapper = objectMapper;
    }

    ExecutionResult execute(
            PolicyStudioOperationalRunRequest request,
            String ifMatch,
            String idempotencyKey,
            Set<String> actorPermissions,
            String actorSubject,
            String correlationId,
            DomainRuleGovernancePrincipal principal) {
        validate(request, idempotencyKey, actorSubject, correlationId, principal);
        DomainRuleChangeWorkspaceResponse workspace = workspaceService.get(request.workspaceId(), principal);
        requireStrongMatch(ifMatch, workspace.etag());

        List<PolicyStudioOperationalScenarioSelection> selections = List.copyOf(request.scenarios());
        Map<UUID, String> expectedOperations = operationModes(selections);
        List<UUID> scenarioIds = selections.stream()
                .map(PolicyStudioOperationalScenarioSelection::scenarioId)
                .toList();
        var sandboxRequest = new PolicyStudioSandboxRunRequest(
                request.workspaceId(), scenarioIds, request.userTimeZone(),
                idempotencyKey.trim(), request.evaluatedAtUtc());

        var configReceipt = executor.existingSandbox(sandboxRequest, principal);
        if (configReceipt.isPresent()) {
            requireSameOperationalCommand(configReceipt.get(), expectedOperations);
            return new ExecutionResult(configReceipt.get(), workspace.etag());
        }

        ResourceActionExecution execution = actionExecutionService.reserve(
                        RESOURCE_KEY, request.workspaceId(), ACTION_ID, ActionScope.COLLECTION,
                        idempotencyKey, request, correlationId, actorSubject)
                .orElseThrow(() -> new IllegalStateException("Operational idempotency reservation is required"));
        if ("COMPLETED".equals(execution.getExecutionStatus())) {
            DomainRuleTestRunResponse stored = restore(execution);
            requireSameOperationalCommand(stored, expectedOperations);
            return new ExecutionResult(stored, workspace.etag());
        }

        Set<String> effectivePermissions = new LinkedHashSet<>(
                actorPermissions == null ? Set.of() : actorPermissions);
        // The dedicated operational-test authority grants access only to disposable, prefix-bound
        // fixtures. It deliberately supplies the pilot's business permission inside this action.
        effectivePermissions.add("benefit:request");
        try {
            DomainRuleTestRunResponse run = executor.executeSandbox(
                    sandboxRequest,
                    prepared -> bindingFactory.create(
                            prepared, selections, principal.tenantId(), principal.environment()),
                    Set.copyOf(effectivePermissions),
                    actorSubject.trim(), correlationId.trim(), principal);
            requireSameOperationalCommand(run, expectedOperations);
            actionExecutionService.complete(execution, run);
            return new ExecutionResult(run, workspace.etag());
        } catch (RuntimeException failure) {
            actionExecutionService.fail(execution, failure);
            throw failure;
        }
    }

    private Map<UUID, String> operationModes(
            List<PolicyStudioOperationalScenarioSelection> selections) {
        Map<UUID, String> operations = new LinkedHashMap<>();
        selections.forEach(selection -> {
            String operation = selection.operationMode() == null
                    ? "" : selection.operationMode().trim().toUpperCase(Locale.ROOT);
            if (!SUPPORTED_OPERATION_MODES.contains(operation)) {
                throw badRequest("operationMode must be CREATE or UPDATE");
            }
            if (operations.putIfAbsent(selection.scenarioId(), operation) != null) {
                throw badRequest("Operational scenario ids must be unique");
            }
        });
        return Map.copyOf(operations);
    }

    private void requireSameOperationalCommand(
            DomainRuleTestRunResponse run,
            Map<UUID, String> expectedOperations) {
        if (run == null || run.results() == null || run.results().size() != expectedOperations.size()) {
            throw conflict("Recorded Test Run does not match the operational command");
        }
        boolean mismatch = run.results().stream().anyMatch(result -> {
            var evidence = result.operationalEvidence();
            return evidence == null
                    || !expectedOperations.getOrDefault(result.scenarioId(), "")
                            .equals(evidence.operationMode());
        });
        if (mismatch) {
            throw conflict("idempotencyKey was already used with different operational modes");
        }
    }

    private DomainRuleTestRunResponse restore(ResourceActionExecution execution) {
        try {
            return objectMapper.treeToValue(
                    execution.getResponsePayload(), DomainRuleTestRunResponse.class);
        } catch (Exception invalidStoredResult) {
            throw new IllegalStateException(
                    "Unable to restore the idempotent operational Test Run", invalidStoredResult);
        }
    }

    private void validate(
            PolicyStudioOperationalRunRequest request,
            String idempotencyKey,
            String actorSubject,
            String correlationId,
            DomainRuleGovernancePrincipal principal) {
        if (request == null || request.workspaceId() == null || request.scenarios() == null
                || request.scenarios().isEmpty() || request.evaluatedAtUtc() == null) {
            throw badRequest("workspaceId, scenarios and evaluatedAtUtc are required");
        }
        if (request.scenarios().size() > 20
                || request.scenarios().stream().anyMatch(item -> item == null || item.scenarioId() == null)) {
            throw badRequest("Operational scenarios must contain 1 to 20 governed scenario ids");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.trim().length() > 180) {
            throw badRequest("Idempotency-Key must contain 1 to 180 characters");
        }
        if (actorSubject == null || actorSubject.isBlank()) throw badRequest("actorSubject is required");
        if (correlationId == null || correlationId.isBlank() || correlationId.trim().length() > 180) {
            throw badRequest("X-Correlation-ID must contain 1 to 180 characters");
        }
        if (principal == null) throw new IllegalArgumentException("principal is required");
    }

    private void requireStrongMatch(String ifMatch, String currentEtag) {
        HttpEntityTagCondition condition;
        try {
            condition = HttpEntityTagCondition.parse(ifMatch);
        } catch (IllegalArgumentException invalid) {
            throw badRequest(invalid.getMessage());
        }
        if (condition.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "If-Match is required");
        }
        if (condition.wildcard() || !condition.matchesStrong(currentEtag)) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "Workspace changed; reload before running operational proof");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    record ExecutionResult(DomainRuleTestRunResponse run, String workspaceEtag) {}
}
