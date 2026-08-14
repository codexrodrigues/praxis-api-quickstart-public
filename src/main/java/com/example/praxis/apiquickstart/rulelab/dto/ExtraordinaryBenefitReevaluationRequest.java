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

/** Command input for re-evaluating an existing, still-editable benefit request. */
@Schema(
        name = "ExtraordinaryBenefitReevaluationRequest",
        description = "Nova intenção de negócio para uma solicitação ainda avaliada. A identidade é preservada e os fatos corporativos são readquiridos pelo host.")
public record ExtraordinaryBenefitReevaluationRequest(
        @NotNull
        @Schema(description = "Categoria atualizada da necessidade excepcional.")
        @UISchema(label = "Motivo do benefício", controlType = FieldControlType.SELECT, order = 10)
        ExtraordinaryBenefitReason reasonCode,

        @NotNull
        @Schema(description = "Data atualizada do evento que fundamenta a solicitação.")
        @UISchema(label = "Data do evento", controlType = FieldControlType.DATE_PICKER, order = 20)
        LocalDate eventDate,

        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2)
        @Schema(description = "Novo valor solicitado, sujeito às políticas e aos fatos vigentes.")
        @UISchema(label = "Valor solicitado", controlType = FieldControlType.CURRENCY_INPUT, order = 30)
        BigDecimal requestedAmount,

        @NotBlank @Size(max = 120)
        @Schema(description = "Referência opaca dos fatos autoritativos que o host deve readquirir.")
        @UISchema(label = "Referência dos fatos", controlType = FieldControlType.INPUT, order = 40, maxLength = 120)
        String factReference,

        @NotNull
        @Schema(description = "Nova data pretendida de pagamento, validada contra o calendário autoritativo.")
        @UISchema(label = "Data pretendida de pagamento", controlType = FieldControlType.DATE_PICKER, order = 50)
        LocalDate requestedPaymentDate,

        @NotBlank @Size(max = 80)
        @Schema(description = "Fuso IANA usado para congelar a avaliação temporal.")
        @UISchema(label = "Fuso horário", controlType = FieldControlType.INPUT, order = 60, maxLength = 80)
        String userTimeZone) {
}
