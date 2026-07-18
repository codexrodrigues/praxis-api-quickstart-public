package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import java.util.Objects;
import java.util.Set;

/** Monta a avaliacao a partir de command facts e facts autoritativos adquiridos pelo host. */
final class ExtraordinaryBenefitAuthoritativeEvaluationService {
    private final ExtraordinaryBenefitFactProvider factProvider;
    private final ExtraordinaryBenefitEvaluationService evaluationService;

    ExtraordinaryBenefitAuthoritativeEvaluationService(
            ExtraordinaryBenefitFactProvider factProvider,
            ExtraordinaryBenefitEvaluationService evaluationService) {
        this.factProvider = Objects.requireNonNull(factProvider, "factProvider is required");
        this.evaluationService = Objects.requireNonNull(evaluationService, "evaluationService is required");
    }

    ExtraordinaryBenefitAuthoritativeEvaluationResult evaluate(
            ExtraordinaryBenefitAuthoritativeEvaluationCommand command,
            RuleFactLookup lookup,
            Set<String> actorPermissions) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(lookup, "lookup is required");
        if (!command.factReference().equals(lookup.factReference())) {
            throw new IllegalArgumentException("command and lookup fact references must match");
        }
        ExtraordinaryBenefitFactSnapshot snapshot = factProvider.load(lookup);
        ExtraordinaryBenefitEvaluationRequest request = toEvaluationRequest(command, snapshot);
        return new ExtraordinaryBenefitAuthoritativeEvaluationResult(
                request,
                evaluationService.evaluateDecision(request, actorPermissions),
                snapshot.provenance());
    }

    static ExtraordinaryBenefitEvaluationRequest toEvaluationRequest(
            ExtraordinaryBenefitAuthoritativeEvaluationCommand command,
            ExtraordinaryBenefitFactSnapshot snapshot) {
        ExtraordinaryBenefitAuthoritativeFacts facts = snapshot.facts();
        return new ExtraordinaryBenefitEvaluationRequest(
                command.requestReference(), command.reasonCode(), command.eventDate(), command.requestedAmount(),
                facts.workerStatus(), facts.duplicateGrant(), facts.programActive(), facts.programMaximumAmount(),
                facts.customerAdditionalEligible(), command.requestedPaymentDate(), facts.allowedPaymentDates(),
                facts.availableBudgetAmount(), command.userTimeZone());
    }
}
