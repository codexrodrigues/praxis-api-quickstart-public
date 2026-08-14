package com.example.praxis.apiquickstart.hr.dto.determination;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.praxisplatform.uischema.annotation.AiControlledUseMode;
import org.praxisplatform.uischema.annotation.AiTrainingUseMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.AiVisibilityMode;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;

import java.math.BigDecimal;

@Schema(description = "Fatos financeiros minimos para determinar o salario liquido sem persistir a folha.")
public record PayrollNetSalaryDeterminationRequest(
        @NotNull
        @DecimalMin("0.00")
        @DomainGovernance(
                kind = DomainGovernanceKind.PRIVACY,
                classification = DomainClassification.CONFIDENTIAL,
                dataCategory = DomainDataCategory.FINANCIAL,
                complianceTags = {"LGPD", "INTERNAL_POLICY"},
                reason = "O salario bruto e dado financeiro pessoal usado somente na determinacao.",
                aiUsage = @AiUsagePolicy(
                        visibility = AiVisibilityMode.MASK,
                        trainingUse = AiTrainingUseMode.DENY,
                        ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
                        reasoningUse = AiControlledUseMode.ALLOW))
        @Schema(description = "Total bruto governado da competencia.", example = "10000.00")
        BigDecimal salarioBruto,
        @NotNull
        @DecimalMin("0.00")
        @DomainGovernance(
                kind = DomainGovernanceKind.PRIVACY,
                classification = DomainClassification.CONFIDENTIAL,
                dataCategory = DomainDataCategory.FINANCIAL,
                complianceTags = {"LGPD", "INTERNAL_POLICY"},
                reason = "O total de descontos e dado financeiro pessoal usado somente na determinacao.",
                aiUsage = @AiUsagePolicy(
                        visibility = AiVisibilityMode.MASK,
                        trainingUse = AiTrainingUseMode.DENY,
                        ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
                        reasoningUse = AiControlledUseMode.ALLOW))
        @Schema(description = "Total de descontos legais e contratuais.", example = "2450.35")
        BigDecimal totalDescontos
) {
}
