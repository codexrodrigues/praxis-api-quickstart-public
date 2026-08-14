package com.example.praxis.apiquickstart.hr.dto.determination;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.praxisplatform.uischema.annotation.AiControlledUseMode;
import org.praxisplatform.uischema.annotation.AiTrainingUseMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.AiVisibilityMode;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;

/**
 * Entrada minima da determinacao reativa de endereco do host de referencia.
 *
 * <p>A operacao consulta fatos do host e nao cria nem altera um endereco.</p>
 */
@Schema(
        name = "PostalAddressDeterminationRequest",
        description = "CEP informado durante a edicao de um endereco para obter uma derivacao cadastral nao persistente.")
public record PostalAddressDeterminationRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP deve ser no formato 00000-000")
        @DomainGovernance(
                kind = DomainGovernanceKind.PRIVACY,
                classification = DomainClassification.CONFIDENTIAL,
                dataCategory = DomainDataCategory.PERSONAL,
                complianceTags = {"LGPD", "PHYSICAL_SECURITY"},
                reason = "O CEP usado na determinacao pode revelar uma area residencial pequena.",
                aiUsage = @AiUsagePolicy(
                        visibility = AiVisibilityMode.MASK,
                        trainingUse = AiTrainingUseMode.DENY,
                        ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
                        reasoningUse = AiControlledUseMode.ALLOW))
        @Schema(
                description = "CEP brasileiro com oito digitos, com ou sem hifen, usado como fato de entrada da determinacao.",
                example = "01310-100")
        String cep
) {
}
