package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitAuthoritativeEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationCommandResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReevaluationRequest;
import java.util.Objects;
import java.util.Set;
import org.praxisplatform.config.contract.DomainRuleOperationalTestEvidence;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Orquestra provas descartaveis usando o mesmo workflow autoritativo do recurso piloto.
 *
 * <p>Cada cenario roda numa transacao rollback-only do datasource operacional. A evidencia e
 * capturada dentro da transacao e o estado limpo e verificado depois do rollback. Isso preserva os
 * mesmos triggers e FKs corporativos sem apagar ou contornar ledgers append-only.</p>
 */
@Service
class ExtraordinaryBenefitOperationalProofService {
    private final ExtraordinaryBenefitWorkflowService workflow;
    private final ExtraordinaryBenefitOperationalEvidenceProbe probe;
    private final PolicyStudioOperationalEvidenceAdapter evidenceAdapter;
    private final PolicyStudioBaselineCallCounter baselineCalls;
    private final TransactionTemplate transaction;

    ExtraordinaryBenefitOperationalProofService(
            ExtraordinaryBenefitWorkflowService workflow,
            ExtraordinaryBenefitOperationalEvidenceProbe probe,
            PolicyStudioOperationalEvidenceAdapter evidenceAdapter,
            PolicyStudioBaselineCallCounter baselineCalls,
            @Qualifier("apiTransactionManager") PlatformTransactionManager transactionManager) {
        this.workflow = workflow;
        this.probe = probe;
        this.evidenceAdapter = evidenceAdapter;
        this.baselineCalls = baselineCalls;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    DomainRuleOperationalTestEvidence proveCreate(
            ExtraordinaryBenefitAuthoritativeEvaluationRequest request,
            boolean mutationExpected,
            Set<String> permissions,
            String actorSubject,
            String correlationId) {
        Objects.requireNonNull(request, "request is required");
        String reference = request.requestReference();
        var cleanState = probe.capture(reference);
        int baselineBefore = baselineCalls.current();
        try {
            CapturedProof captured = transaction.execute(status -> {
                try {
                    var before = probe.capture(reference);
                    workflow.evaluateAndPersist(request, permissions, actorSubject, correlationId);
                    return new CapturedProof(
                            before, probe.capture(reference), baselineCalls.deltaSince(baselineBefore));
                } finally {
                    status.setRollbackOnly();
                }
            });
            return verifiedEvidence(
                    PolicyStudioOperationalEvidenceAdapter.OperationMode.CREATE,
                    mutationExpected, cleanState, reference, captured);
        } finally {
            baselineCalls.clear();
        }
    }

    DomainRuleOperationalTestEvidence proveUpdate(
            ExtraordinaryBenefitAuthoritativeEvaluationRequest seed,
            ExtraordinaryBenefitReevaluationRequest update,
            boolean mutationExpected,
            Set<String> permissions,
            String actorSubject,
            String correlationId) {
        Objects.requireNonNull(seed, "seed is required");
        Objects.requireNonNull(update, "update is required");
        String reference = seed.requestReference();
        var cleanState = probe.capture(reference);
        int baselineBefore = baselineCalls.current();
        try {
            CapturedProof captured = transaction.execute(status -> {
                try {
                    ExtraordinaryBenefitEvaluationCommandResponse seeded = workflow.evaluateAndPersist(
                            seed, permissions, actorSubject, correlationId + ":seed");
                    if (seeded.resource() == null || seeded.resource().id() == null) {
                        throw new IllegalStateException("UPDATE proof requires an ALLOW seed persisted by the host");
                    }
                    Long id = seeded.resource().id();
                    var before = probe.capture(reference);
                    workflow.reEvaluate(id, update, permissions, actorSubject, correlationId);
                    return new CapturedProof(
                            before, probe.capture(reference), baselineCalls.deltaSince(baselineBefore));
                } finally {
                    status.setRollbackOnly();
                }
            });
            return verifiedEvidence(
                    PolicyStudioOperationalEvidenceAdapter.OperationMode.UPDATE,
                    mutationExpected, cleanState, reference, captured);
        } finally {
            baselineCalls.clear();
        }
    }

    private DomainRuleOperationalTestEvidence verifiedEvidence(
            PolicyStudioOperationalEvidenceAdapter.OperationMode operationMode,
            boolean mutationExpected,
            PolicyStudioOperationalEvidenceAdapter.OperationalState cleanState,
            String reference,
            CapturedProof captured) {
        if (captured == null) throw new IllegalStateException("Operational proof transaction returned no result");
        boolean cleanupVerified = cleanState.equals(probe.capture(reference));
        return evidenceAdapter.evidence(
                operationMode, mutationExpected, captured.baselineCalls(),
                captured.before(), captured.after(), cleanupVerified);
    }

    private record CapturedProof(
            PolicyStudioOperationalEvidenceAdapter.OperationalState before,
            PolicyStudioOperationalEvidenceAdapter.OperationalState after,
            int baselineCalls) {}
}
