package com.example.praxis.apiquickstart.hr.dto.determination;

import io.swagger.v3.oas.annotations.media.Schema;
import org.praxisplatform.uischema.annotation.AiControlledUseMode;
import org.praxisplatform.uischema.annotation.AiTrainingUseMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.AiVisibilityMode;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;

import java.math.BigDecimal;

@Schema(description = "Resultado financeiro deterministico calculado pelo backend.")
public record PayrollNetSalaryDeterminationResponse(
        @DomainGovernance(
                kind = DomainGovernanceKind.PRIVACY,
                classification = DomainClassification.CONFIDENTIAL,
                dataCategory = DomainDataCategory.FINANCIAL,
                complianceTags = {"LGPD", "INTERNAL_POLICY"},
                reason = "O salario liquido determinado e dado financeiro pessoal.",
                aiUsage = @AiUsagePolicy(
                        visibility = AiVisibilityMode.MASK,
                        trainingUse = AiTrainingUseMode.DENY,
                        ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
                        reasoningUse = AiControlledUseMode.ALLOW))
        @Schema(description = "Salario bruto menos descontos, arredondado em duas casas.", example = "7549.65")
        BigDecimal salarioLiquido,
        @Schema(description = "Versao auditavel da formula demonstrativa.", example = "payroll-net-v1")
        String decisionVersion
) {
}
