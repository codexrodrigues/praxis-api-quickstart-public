package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioSandboxRunRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.praxisplatform.config.contract.DomainRuleOperationalTestEvidence;
import org.praxisplatform.config.contract.DomainRuleTestRunRecordRequest;
import org.praxisplatform.config.contract.DomainRuleTestRunResponse;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.springframework.stereotype.Service;

/** Executa avaliacao e prova operacional numa unica chamada interna, sem alterar o sandbox read-only. */
@Service
final class PolicyStudioOperationalTestRunExecutor implements PolicyStudioOperationalTestRunPort {
    private final ExtraordinaryBenefitOperationalProofService proofService;
    private final PolicyStudioOperationalTestRunRecorder recorder;
    private final PolicyStudioSandboxService sandbox;

    PolicyStudioOperationalTestRunExecutor(
            ExtraordinaryBenefitOperationalProofService proofService,
            PolicyStudioOperationalTestRunRecorder recorder,
            PolicyStudioSandboxService sandbox) {
        this.proofService = proofService;
        this.recorder = recorder;
        this.sandbox = sandbox;
    }

    @Override
    public Optional<DomainRuleTestRunResponse> existingSandbox(
            PolicyStudioSandboxRunRequest request,
            DomainRuleGovernancePrincipal principal) {
        return sandbox.existingRecord(request, principal);
    }

    @Override
    public DomainRuleTestRunResponse executeSandbox(
            PolicyStudioSandboxRunRequest request,
            Function<PolicyStudioSandboxService.PolicyStudioSandboxPreparedRun,
                    List<ExtraordinaryBenefitOperationalScenarioBinding>> bindingFactory,
            Set<String> permissions,
            String actorSubject,
            String correlationId,
            DomainRuleGovernancePrincipal principal) {
        var replay = existingSandbox(request, principal);
        if (replay.isPresent()) {
            return replay.get();
        }
        PolicyStudioSandboxService.PolicyStudioSandboxPreparedRun prepared = sandbox.prepare(request, principal);
        if (bindingFactory == null) {
            throw new IllegalArgumentException("bindingFactory is required");
        }
        return execute(
                prepared.workspaceId(), prepared::recordRequest, bindingFactory.apply(prepared),
                permissions, actorSubject, correlationId, principal);
    }

    DomainRuleTestRunResponse execute(
            UUID workspaceId,
            Supplier<DomainRuleTestRunRecordRequest> evaluatedRunSupplier,
            List<ExtraordinaryBenefitOperationalScenarioBinding> bindings,
            Set<String> permissions,
            String actorSubject,
            String correlationId,
            DomainRuleGovernancePrincipal principal) {
        Objects.requireNonNull(workspaceId, "workspaceId is required");
        Objects.requireNonNull(principal, "principal is required");
        Set<String> resolvedPermissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        if (actorSubject == null || actorSubject.isBlank()) {
            throw new IllegalArgumentException("actorSubject is required");
        }
        if (correlationId == null || correlationId.isBlank() || correlationId.length() > 180) {
            throw new IllegalArgumentException("correlationId must contain 1 to 180 characters");
        }
        if (evaluatedRunSupplier == null) throw new IllegalArgumentException("evaluatedRunSupplier is required");
        DomainRuleTestRunRecordRequest evaluatedRun = evaluatedRunSupplier.get();
        if (evaluatedRun == null || evaluatedRun.results() == null || evaluatedRun.results().isEmpty()) {
            throw new IllegalArgumentException("Evaluation must produce at least one scenario result");
        }
        Set<UUID> resultIds = evaluatedRun.results().stream()
                .map(result -> result.scenarioId())
                .collect(Collectors.toSet());
        if (resultIds.contains(null) || resultIds.size() != evaluatedRun.results().size()) {
            throw new IllegalArgumentException("Evaluation results require unique scenario ids");
        }
        if (bindings == null || bindings.size() != resultIds.size()) {
            throw new IllegalArgumentException("Operational bindings must cover exactly every evaluated scenario");
        }
        Set<UUID> bindingIds = new HashSet<>();
        bindings.forEach(binding -> {
            if (binding == null || !bindingIds.add(binding.scenarioId())) {
                throw new IllegalArgumentException("Operational bindings require unique scenario ids");
            }
        });
        if (!bindingIds.equals(resultIds)) {
            throw new IllegalArgumentException("Operational bindings must cover exactly every evaluated scenario");
        }

        Map<UUID, DomainRuleOperationalTestEvidence> evidence = bindings.stream().collect(Collectors.toMap(
                ExtraordinaryBenefitOperationalScenarioBinding::scenarioId,
                binding -> prove(binding, resolvedPermissions, actorSubject,
                        correlationId + ":scenario:" + binding.scenarioId())));
        return recorder.record(workspaceId, evaluatedRun, evidence, principal);
    }

    private DomainRuleOperationalTestEvidence prove(
            ExtraordinaryBenefitOperationalScenarioBinding binding,
            Set<String> permissions,
            String actorSubject,
            String correlationId) {
        if (binding.operationMode() == PolicyStudioOperationalEvidenceAdapter.OperationMode.CREATE) {
            return proofService.proveCreate(
                    binding.seed(), binding.mutationExpected(),
                    permissions, actorSubject, correlationId);
        }
        return proofService.proveUpdate(
                binding.seed(), binding.update(), binding.mutationExpected(),
                permissions, actorSubject, correlationId);
    }
}
