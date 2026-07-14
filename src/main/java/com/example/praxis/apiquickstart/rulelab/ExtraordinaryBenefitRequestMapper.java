package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationOutcome;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestResponse;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/** Converte o agregado persistido sem reavaliar regra nem consultar o control plane. */
@Component
public class ExtraordinaryBenefitRequestMapper {
    public ExtraordinaryBenefitRequestResponse toResponse(ExtraordinaryBenefitRequestEntity entity) {
        ExtraordinaryBenefitEvaluationRequest facts = new ExtraordinaryBenefitEvaluationRequest(
                entity.getRequestReference(),
                entity.getReasonCode(),
                entity.getEventDate(),
                entity.getRequestedAmount(),
                entity.getWorkerStatus(),
                entity.isDuplicateGrant(),
                entity.isProgramActive(),
                entity.getProgramMaximumAmount(),
                entity.getCustomerAdditionalEligible(),
                entity.getRequestedPaymentDate(),
                parseDates(entity.getAllowedPaymentDates()),
                entity.getAvailableBudgetAmount(),
                entity.getUserTimeZone());
        boolean effectExecuted = entity.getEffectStatus() == ExtraordinaryBenefitEffectStatus.EXECUTED;
        ExtraordinaryBenefitEvaluationResponse evaluation = new ExtraordinaryBenefitEvaluationResponse(
                entity.getRequestReference(),
                ExtraordinaryBenefitEvaluationOutcome.ALLOW,
                entity.getEvaluationBusinessMessage(),
                parseReasonCodes(entity.getEvaluationReasonCodes()),
                entity.getEvaluatedAt(),
                entity.getSnapshotKey(),
                entity.getSnapshotContentHash(),
                entity.getSnapshotActivationRevision(),
                entity.getRuleSetKey(),
                entity.getRuleSetVersion(),
                entity.getFactsDigest(),
                entity.getPlanDigest(),
                entity.getRecommendedAmount(),
                entity.getCurrency(),
                entity.getPlannedEffectIntent(),
                effectExecuted ? "EXECUTED_BY_HOST" : "PLANNED_NOT_EXECUTED",
                true,
                effectExecuted);
        return new ExtraordinaryBenefitRequestResponse(
                entity.getId(),
                entity.getLifecycleStatus(),
                facts,
                evaluation,
                entity.getEffectStatus(),
                entity.getVersion(),
                entity.getEvaluatedAt(),
                entity.getSubmittedAt(),
                entity.getApprovedAt(),
                entity.getAppliedAt());
    }

    public ExtraordinaryBenefitEvaluationResponse markPersisted(
            ExtraordinaryBenefitEvaluationResponse evaluation) {
        return new ExtraordinaryBenefitEvaluationResponse(
                evaluation.requestReference(), evaluation.outcome(), evaluation.businessMessage(),
                evaluation.reasonCodes(), evaluation.evaluatedAtUtc(), evaluation.snapshotKey(),
                evaluation.snapshotContentHash(), evaluation.snapshotActivationRevision(),
                evaluation.ruleSetKey(), evaluation.ruleSetVersion(), evaluation.factsDigest(),
                evaluation.planDigest(), evaluation.recommendedAmount(), evaluation.currency(),
                evaluation.plannedEffectIntent(), evaluation.plannedEffectStatus(), true, false);
    }

    private List<LocalDate> parseDates(String dates) {
        if (dates == null || dates.isBlank()) {
            return List.of();
        }
        return Arrays.stream(dates.split(",")).map(LocalDate::parse).toList();
    }

    private List<String> parseReasonCodes(String reasonCodes) {
        if (reasonCodes == null || reasonCodes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(reasonCodes.split(",")).filter(code -> !code.isBlank()).toList();
    }
}
