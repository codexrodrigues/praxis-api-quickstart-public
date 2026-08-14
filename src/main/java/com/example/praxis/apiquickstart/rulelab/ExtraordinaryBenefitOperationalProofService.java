package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitAuthoritativeEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationCommandResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReevaluationRequest;
import java.util.Objects;
import java.util.Set;
import org.praxisplatform.config.dto.DomainRuleOperationalTestEvidence;
import org.springframework.stereotype.Service;

/** Orquestra provas descartaveis usando o mesmo workflow autoritativo do recurso piloto. */
@Service
class ExtraordinaryBenefitOperationalProofService {
    private final ExtraordinaryBenefitWorkflowService workflow;
    private final ExtraordinaryBenefitOperationalEvidenceProbe probe;
    private final PolicyStudioOperationalEvidenceAdapter evidenceAdapter;

    ExtraordinaryBenefitOperationalProofService(
            ExtraordinaryBenefitWorkflowService workflow,
            ExtraordinaryBenefitOperationalEvidenceProbe probe,
            PolicyStudioOperationalEvidenceAdapter evidenceAdapter) {
        this.workflow = workflow;
        this.probe = probe;
        this.evidenceAdapter = evidenceAdapter;
    }

    DomainRuleOperationalTestEvidence proveCreate(
            ExtraordinaryBenefitAuthoritativeEvaluationRequest request,
            boolean mutationExpected,
            int baselineCallCount,
            Set<String> permissions,
            String actorSubject,
            String correlationId) {
        Objects.requireNonNull(request, "request is required");
        String reference = request.requestReference();
        var cleanState = probe.capture(reference);
        return evidenceAdapter.observe(
                PolicyStudioOperationalEvidenceAdapter.OperationMode.CREATE,
                mutationExpected,
                baselineCallCount,
                cleanState,
                () -> probe.capture(reference),
                () -> workflow.evaluateAndPersist(
                        request, permissions, actorSubject, correlationId),
                () -> probe.cleanup(reference));
    }

    DomainRuleOperationalTestEvidence proveUpdate(
            ExtraordinaryBenefitAuthoritativeEvaluationRequest seed,
            ExtraordinaryBenefitReevaluationRequest update,
            boolean mutationExpected,
            int baselineCallCount,
            Set<String> permissions,
            String actorSubject,
            String correlationId) {
        Objects.requireNonNull(seed, "seed is required");
        Objects.requireNonNull(update, "update is required");
        String reference = seed.requestReference();
        var cleanState = probe.capture(reference);
        try {
            ExtraordinaryBenefitEvaluationCommandResponse seeded = workflow.evaluateAndPersist(
                    seed, permissions, actorSubject, correlationId + ":seed");
            if (seeded.resource() == null || seeded.resource().id() == null) {
                throw new IllegalStateException("UPDATE proof requires an ALLOW seed persisted by the host");
            }
            Long id = seeded.resource().id();
            return evidenceAdapter.observe(
                    PolicyStudioOperationalEvidenceAdapter.OperationMode.UPDATE,
                    mutationExpected,
                    baselineCallCount,
                    cleanState,
                    () -> probe.capture(reference),
                    () -> workflow.reEvaluate(
                            id, update, permissions, actorSubject, correlationId),
                    () -> probe.cleanup(reference));
        } finally {
            // Covers seed failures before the evidence adapter owns the cleanup; deletion is idempotent.
            probe.cleanup(reference);
        }
    }
}
