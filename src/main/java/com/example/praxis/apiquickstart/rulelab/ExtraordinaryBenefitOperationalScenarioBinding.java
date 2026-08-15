package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitAuthoritativeEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReevaluationRequest;
import java.util.Objects;
import java.util.UUID;

/** Binding host-owned explicito entre um cenario governado e seu comando operacional descartavel. */
record ExtraordinaryBenefitOperationalScenarioBinding(
        UUID scenarioId,
        PolicyStudioOperationalEvidenceAdapter.OperationMode operationMode,
        ExtraordinaryBenefitAuthoritativeEvaluationRequest seed,
        ExtraordinaryBenefitReevaluationRequest update,
        boolean mutationExpected) {

    ExtraordinaryBenefitOperationalScenarioBinding {
        Objects.requireNonNull(scenarioId, "scenarioId is required");
        Objects.requireNonNull(operationMode, "operationMode is required");
        Objects.requireNonNull(seed, "seed is required");
        if (operationMode == PolicyStudioOperationalEvidenceAdapter.OperationMode.UPDATE && update == null) {
            throw new IllegalArgumentException("UPDATE binding requires an update command");
        }
        if (operationMode == PolicyStudioOperationalEvidenceAdapter.OperationMode.CREATE && update != null) {
            throw new IllegalArgumentException("CREATE binding cannot carry an update command");
        }
    }
}
