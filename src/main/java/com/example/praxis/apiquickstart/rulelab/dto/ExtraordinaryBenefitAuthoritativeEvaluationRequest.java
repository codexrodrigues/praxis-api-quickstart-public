package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "ExtraordinaryBenefitAuthoritativeEvaluationRequest",
        description = "Comando de negocio para avaliar um beneficio. Estados funcionais, limites, calendario e saldo sao adquiridos pelo host a partir da referencia autoritativa.")
public record ExtraordinaryBenefitAuthoritativeEvaluationRequest(
        @NotBlank @Size(max = 80)
        @Schema(description = "Referencia externa idempotente da solicitacao de beneficio.", example = "BEN-2026-000184")
        @UISchema(label = "Referencia da solicitacao", controlType = FieldControlType.INPUT, order = 10, maxLength = 80)
        String requestReference,

        @NotNull
        @Schema(description = "Categoria da necessidade excepcional que fundamenta a solicitacao.")
        @UISchema(label = "Motivo do beneficio", controlType = FieldControlType.SELECT, order = 20)
        ExtraordinaryBenefitReason reasonCode,

        @NotNull
        @Schema(description = "Data do evento que originou a necessidade.", example = "2026-07-13")
        @UISchema(label = "Data do evento", controlType = FieldControlType.DATE_PICKER, order = 30)
        LocalDate eventDate,

        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2)
        @Schema(description = "Valor solicitado antes da aplicacao dos limites e disponibilidade adquiridos pelo host.", example = "2500.00")
        @UISchema(label = "Valor solicitado", controlType = FieldControlType.CURRENCY_INPUT, order = 40)
        BigDecimal requestedAmount,

        @NotBlank @Size(max = 120)
        @Schema(description = "Referencia opaca ao registro funcional e orcamentario que o host consultara no datasource autoritativo.", example = "FACT-001")
        @UISchema(label = "Referencia dos fatos", controlType = FieldControlType.INPUT, order = 50, maxLength = 120)
        String factReference,

        @NotNull
        @Schema(description = "Data de pagamento pretendida, validada contra o calendario autoritativo.", example = "2026-07-20")
        @UISchema(label = "Data pretendida de pagamento", controlType = FieldControlType.DATE_PICKER, order = 60)
        LocalDate requestedPaymentDate,

        @NotBlank @Size(max = 80)
        @Schema(description = "Fuso IANA usado na avaliacao temporal.", example = "America/Sao_Paulo")
        @UISchema(label = "Fuso horario", controlType = FieldControlType.INPUT, order = 70, maxLength = 80)
        String userTimeZone) {
}
