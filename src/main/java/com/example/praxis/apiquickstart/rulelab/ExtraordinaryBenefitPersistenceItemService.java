package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationResponse;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste um único ALLOW na transação corrente.
 *
 * <p>O lote público chama este bean sem transação externa e obtém uma unidade por item. A prova
 * `STATEMENT_ATOMIC` fornece uma transação externa para que todos os itens compartilhem a mesma
 * unidade, sem duplicar o mapeamento de persistência.</p>
 */
@Service
public class ExtraordinaryBenefitPersistenceItemService {
    private final ExtraordinaryBenefitRequestRepository repository;
    private final ExtraordinaryBenefitRequestMapper mapper;
    private final ExtraordinaryBenefitTransformationAuditStore transformationAuditStore;

    public ExtraordinaryBenefitPersistenceItemService(
            ExtraordinaryBenefitRequestRepository repository,
            ExtraordinaryBenefitRequestMapper mapper,
            ExtraordinaryBenefitTransformationAuditStore transformationAuditStore) {
        this.repository = repository;
        this.mapper = mapper;
        this.transformationAuditStore = transformationAuditStore;
    }

    @Transactional
    public com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestResponse persistAllowed(
            ExtraordinaryBenefitEvaluationRequest request,
            ExtraordinaryBenefitEvaluatedDecision decision,
            String factReference,
            RuleFactProvenance factProvenance,
            String actorSubject,
            String correlationId,
            UUID operationId,
            RuleLabOperationCardinality cardinality) {
        ExtraordinaryBenefitEvaluationResponse evaluation = Objects.requireNonNull(
                decision, "decision is required").response();
        ExtraordinaryBenefitTransformationAuditEvidence evidence = Objects.requireNonNull(
                decision.transformationEvidence(), "ALLOW transformation evidence is required");
        ExtraordinaryBenefitRequestEntity entity = new ExtraordinaryBenefitRequestEntity();
        entity.setRequestReference(request.requestReference());
        entity.setReasonCode(request.reasonCode());
        entity.setEventDate(request.eventDate());
        entity.setRequestedAmount(request.requestedAmount());
        entity.setWorkerStatus(request.workerStatus());
        entity.setDuplicateGrant(request.duplicateGrant());
        entity.setProgramActive(request.programActive());
        entity.setProgramMaximumAmount(request.programMaximumAmount());
        entity.setCustomerAdditionalEligible(request.customerAdditionalEligible());
        entity.setRequestedPaymentDate(request.requestedPaymentDate());
        entity.setAllowedPaymentDates(request.allowedPaymentDates().stream()
                .map(Object::toString).collect(Collectors.joining(",")));
        entity.setAvailableBudgetAmount(request.availableBudgetAmount());
        entity.setUserTimeZone(request.userTimeZone());
        entity.setLifecycleStatus(ExtraordinaryBenefitLifecycleStatus.EVALUATED);
        entity.setRecommendedAmount(evaluation.recommendedAmount());
        entity.setCurrency(evaluation.currency());
        entity.setSnapshotKey(evaluation.snapshotKey());
        entity.setSnapshotContentHash(evaluation.snapshotContentHash());
        entity.setSnapshotActivationRevision(evaluation.snapshotActivationRevision());
        entity.setRuleSetKey(evaluation.ruleSetKey());
        entity.setRuleSetVersion(evaluation.ruleSetVersion());
        entity.setFactsDigest(evaluation.factsDigest());
        if (factProvenance != null) {
            entity.setFactReference(requireText(factReference, "factReference"));
            entity.setFactProviderKey(factProvenance.providerKey());
            entity.setFactSourceRecordDigest(factProvenance.sourceRecordDigest());
            entity.setFactSourceVersion(factProvenance.sourceVersion());
            entity.setFactSourceRecordedAt(factProvenance.sourceRecordedAt());
            entity.setFactScopeDigest(factProvenance.scopeDigest());
            entity.setFactAsOf(factProvenance.asOf());
        }
        entity.setPlanDigest(evaluation.planDigest());
        entity.setPlannedEffectIntent(evaluation.plannedEffectIntent());
        entity.setEvaluationBusinessMessage(evaluation.businessMessage());
        entity.setEvaluationReasonCodes(String.join(",", evaluation.reasonCodes()));
        entity.setEffectStatus(ExtraordinaryBenefitEffectStatus.PLANNED);
        entity.setEvaluatedAt(evaluation.evaluatedAtUtc());
        entity.setCreatedBy(actorSubject);
        entity.setLastTransitionBy(actorSubject);
        ExtraordinaryBenefitRequestEntity persisted = repository.saveAndFlush(entity);
        transformationAuditStore.append(new ExtraordinaryBenefitTransformationAudit(
                UUID.randomUUID(), persisted.getId(), operationId,
                Objects.requireNonNull(cardinality, "cardinality is required"), evidence, evaluation,
                requireText(correlationId, "correlationId")));
        return mapper.toResponse(persisted);
    }

    /**
     * Compatibility boundary for the host-only QL-08 statement atomicity proof.
     * Rows created here deliberately have no authoritative provenance and therefore fail closed
     * in the public apply action. Remove this boundary when QL-08 acquires facts through the host
     * provider in its own coordinated migration.
     */
    @Transactional
    com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestResponse
            persistNonApplicableStatementProof(
                    ExtraordinaryBenefitEvaluationRequest request,
                    ExtraordinaryBenefitEvaluatedDecision decision,
                    String actorSubject,
                    String correlationId,
                    UUID operationId,
                    RuleLabOperationCardinality cardinality) {
        return persistAllowed(
                request, decision, null, null, actorSubject, correlationId, operationId, cardinality);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
