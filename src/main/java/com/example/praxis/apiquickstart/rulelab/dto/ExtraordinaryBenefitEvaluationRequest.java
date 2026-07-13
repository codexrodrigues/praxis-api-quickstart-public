package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "ExtraordinaryBenefitEvaluationRequest",
        description = "Fatos de negocio congelados pelo host para simular a elegibilidade de uma solicitacao de beneficio extraordinario sem persistir o pedido.")
public record ExtraordinaryBenefitEvaluationRequest(
        @NotBlank
        @Size(max = 80)
        @Schema(description = "Referencia externa usada para correlacionar a simulacao com atendimento, chamado ou processo de RH, sem criar identidade persistida.", example = "BEN-2026-000184")
        @UISchema(label = "Referencia da solicitacao", controlType = FieldControlType.INPUT, order = 10, maxLength = 80, icon = "tag")
        String requestReference,

        @NotNull
        @Schema(description = "Categoria da necessidade excepcional que fundamenta a solicitacao e sua futura trilha de auditoria.")
        @UISchema(label = "Motivo do beneficio", controlType = FieldControlType.SELECT, order = 20, icon = "category")
        ExtraordinaryBenefitReason reasonCode,

        @NotNull
        @Schema(description = "Data do evento que originou a necessidade; ancora competencia, vigencia e verificacoes temporais futuras.", example = "2026-07-13")
        @UISchema(label = "Data do evento", controlType = FieldControlType.DATE_PICKER, order = 30, icon = "event")
        LocalDate eventDate,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 13, fraction = 2)
        @Schema(description = "Valor monetario solicitado antes da aplicacao do limite do programa e da disponibilidade orcamentaria.", example = "2500.00")
        @UISchema(label = "Valor solicitado", controlType = FieldControlType.CURRENCY_INPUT, order = 40, icon = "payments")
        BigDecimal requestedAmount,

        @NotNull
        @Pattern(regexp = "ACTIVE|LEAVE|TERMINATED")
        @Schema(description = "Estado funcional considerado na elegibilidade legal do colaborador no instante da avaliacao.", allowableValues = {"ACTIVE", "LEAVE", "TERMINATED"})
        @UISchema(label = "Situacao funcional", controlType = FieldControlType.SELECT, order = 50, icon = "badge")
        String workerStatus,

        @NotNull
        @Schema(description = "Indica se ja existe concessao equivalente para o mesmo fato gerador, evitando duplicidade de beneficio.")
        @UISchema(label = "Concessao duplicada", controlType = FieldControlType.TOGGLE, order = 60, icon = "content_copy")
        Boolean duplicateGrant,

        @NotNull
        @Schema(description = "Indica se o programa corporativo que financia o beneficio esta vigente no contexto avaliado.")
        @UISchema(label = "Programa vigente", controlType = FieldControlType.TOGGLE, order = 70, icon = "policy")
        Boolean programActive,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 13, fraction = 2)
        @Schema(description = "Teto monetario autorizado pelo programa para uma unica concessao extraordinaria.", example = "5000.00")
        @UISchema(label = "Limite do programa", controlType = FieldControlType.CURRENCY_INPUT, order = 80, icon = "price_check")
        BigDecimal programMaximumAmount,

        @Schema(description = "Resultado da restricao adicional definida pelo cliente; ausencia deliberada produz avaliacao inconclusiva em vez de assumir elegibilidade.")
        @UISchema(label = "Elegibilidade adicional", controlType = FieldControlType.TOGGLE, order = 90, icon = "rule")
        Boolean customerAdditionalEligible,

        @NotNull
        @Schema(description = "Data de pagamento pretendida, confrontada com o calendario autorizado pelo programa.", example = "2026-07-20")
        @UISchema(label = "Data pretendida de pagamento", controlType = FieldControlType.DATE_PICKER, order = 100, icon = "event_available")
        LocalDate requestedPaymentDate,

        @NotEmpty
        @Size(max = 24)
        @Schema(description = "Datas de pagamento efetivamente abertas para o ciclo avaliado; a data pretendida deve pertencer a esta lista.")
        @UISchema(label = "Datas de pagamento permitidas", controlType = FieldControlType.ARRAY, order = 110, icon = "date_range")
        List<@NotNull LocalDate> allowedPaymentDates,

        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 13, fraction = 2)
        @Schema(description = "Saldo orcamentario disponivel para suportar a concessao no instante congelado da avaliacao.", example = "100000.00")
        @UISchema(label = "Saldo orcamentario", controlType = FieldControlType.CURRENCY_INPUT, order = 120, icon = "account_balance")
        BigDecimal availableBudgetAmount,

        @NotBlank
        @Size(max = 80)
        @Schema(description = "Fuso IANA usado para interpretar datas e politicas temporais do usuario sem depender do timezone do servidor.", example = "America/Sao_Paulo")
        @UISchema(label = "Fuso horario", controlType = FieldControlType.INPUT, order = 130, maxLength = 80, icon = "schedule")
        String userTimeZone) {

    public ExtraordinaryBenefitEvaluationRequest {
        allowedPaymentDates = allowedPaymentDates == null ? List.of() : List.copyOf(allowedPaymentDates);
    }
}
