package com.example.praxis.apiquickstart.hr.dto.determination;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

@Schema(description = "Fatos autoritativos usados pela politica demonstrativa versionada de pagamento.")
public record PayrollPaymentDateDeterminationRequest(
        @NotNull
        @Min(1900)
        @Max(2100)
        @Schema(description = "Ano da competencia da folha.", example = "2026")
        Integer ano,
        @NotNull
        @Min(1)
        @Max(12)
        @Schema(description = "Mes da competencia da folha.", example = "4")
        Integer mes,
        @NotNull
        @DecimalMin("0.00")
        @DomainGovernance(
                kind = DomainGovernanceKind.PRIVACY,
                classification = DomainClassification.CONFIDENTIAL,
                dataCategory = DomainDataCategory.FINANCIAL,
                complianceTags = {"LGPD", "INTERNAL_POLICY"},
                reason = "O salario liquido e dado financeiro pessoal usado somente na determinacao da data.",
                aiUsage = @AiUsagePolicy(
                        visibility = AiVisibilityMode.MASK,
                        trainingUse = AiTrainingUseMode.DENY,
                        ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
                        reasoningUse = AiControlledUseMode.ALLOW))
        @Schema(description = "Salario liquido autoritativo produzido pela determinacao anterior.", example = "7549.65")
        BigDecimal salarioLiquido
) {}
