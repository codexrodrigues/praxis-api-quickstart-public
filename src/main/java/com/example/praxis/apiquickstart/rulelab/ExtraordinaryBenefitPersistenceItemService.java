package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationResponse;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persiste um unico ALLOW em transacao independente, permitindo lote com falha parcial explicita. */
@Service
public class ExtraordinaryBenefitPersistenceItemService {
    private final ExtraordinaryBenefitRequestRepository repository;
    private final ExtraordinaryBenefitRequestMapper mapper;

    public ExtraordinaryBenefitPersistenceItemService(
            ExtraordinaryBenefitRequestRepository repository,
            ExtraordinaryBenefitRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestResponse persistAllowed(
            ExtraordinaryBenefitEvaluationRequest request,
            ExtraordinaryBenefitEvaluationResponse evaluation,
            String actorSubject) {
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
        entity.setPlanDigest(evaluation.planDigest());
        entity.setPlannedEffectIntent(evaluation.plannedEffectIntent());
        entity.setEvaluationBusinessMessage(evaluation.businessMessage());
        entity.setEvaluationReasonCodes(String.join(",", evaluation.reasonCodes()));
        entity.setEffectStatus(ExtraordinaryBenefitEffectStatus.PLANNED);
        entity.setEvaluatedAt(evaluation.evaluatedAtUtc());
        entity.setCreatedBy(actorSubject);
        entity.setLastTransitionBy(actorSubject);
        return mapper.toResponse(repository.saveAndFlush(entity));
    }
}
