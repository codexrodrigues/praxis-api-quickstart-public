package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationOutcome;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.praxisplatform.rules.contract.RuleBindingResult;
import org.praxisplatform.rules.contract.RuleEvaluationResult;

/** Monta fatos confiaveis do host e projeta o resultado puro do engine para o contrato de negocio. */
public final class ExtraordinaryBenefitEvaluationService {
    private final ExtraordinaryGrantRuleLabService ruleLabService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ExtraordinaryBenefitEvaluationService(
            ExtraordinaryGrantRuleLabService ruleLabService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.ruleLabService = Objects.requireNonNull(ruleLabService, "ruleLabService is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    /** Avalia sem persistir pedido e sem executar o efeito planejado pelo RuleSet. */
    public ExtraordinaryBenefitEvaluationResponse evaluate(
            ExtraordinaryBenefitEvaluationRequest request,
            Set<String> actorPermissions) {
        Objects.requireNonNull(request, "request is required");
        return evaluateAt(request, actorPermissions, clock.instant());
    }

    /** Reusa um instante ja congelado pelo orquestrador shadow, sem alterar a semantica do engine. */
    ExtraordinaryBenefitEvaluationResponse evaluateAt(
            ExtraordinaryBenefitEvaluationRequest request,
            Set<String> actorPermissions,
            Instant evaluatedAt) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt is required");
        ZoneId userTimeZone = ZoneId.of(request.userTimeZone());
        ObjectNode facts = freezeFacts(request, actorPermissions);
        ExtraordinaryGrantRuleEvaluation evaluation =
                ruleLabService.evaluateWithSnapshot(facts, evaluatedAt, userTimeZone);
        return toResponse(request, evaluatedAt, evaluation);
    }

    private ObjectNode freezeFacts(
            ExtraordinaryBenefitEvaluationRequest request,
            Set<String> actorPermissions) {
        ObjectNode facts = objectMapper.createObjectNode();
        ObjectNode requestFacts = facts.putObject("request");
        requestFacts.put("requestReference", request.requestReference());
        requestFacts.put("reasonCode", request.reasonCode().name());
        requestFacts.put("eventDate", request.eventDate().toString());
        requestFacts.put("requestedAmount", request.requestedAmount());
        requestFacts.put("currency", "BRL");

        ArrayNode permissions = facts.putObject("actor").putArray("permissions");
        safePermissions(actorPermissions).forEach(permissions::add);
        facts.putObject("worker").put("status", request.workerStatus());
        facts.putObject("grant").put("hasDuplicate", request.duplicateGrant());
        facts.putObject("program")
                .put("active", request.programActive())
                .put("maxAmount", request.programMaximumAmount());
        ObjectNode customer = facts.putObject("customer");
        if (request.customerAdditionalEligible() != null) {
            customer.put("additionalEligible", request.customerAdditionalEligible());
        }
        ObjectNode payment = facts.putObject("payment");
        payment.put("requestedDate", request.requestedPaymentDate().toString());
        ArrayNode allowedDates = payment.putArray("allowedDates");
        request.allowedPaymentDates().forEach(date -> allowedDates.add(date.toString()));
        facts.putObject("budget").put("availableAmount", request.availableBudgetAmount());
        return facts;
    }

    private Set<String> safePermissions(Set<String> permissions) {
        return permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    private ExtraordinaryBenefitEvaluationResponse toResponse(
            ExtraordinaryBenefitEvaluationRequest request,
            Instant evaluatedAt,
            ExtraordinaryGrantRuleEvaluation evaluation) {
        RuleEvaluationResult result = evaluation.result();
        JsonNode calculation = output(result, "grant.amount-calculation");
        JsonNode effectPlan = output(result, "grant.effect-plan");
        BigDecimal recommendedAmount = calculation == null
                ? null
                : calculation.path("recommendedAmount").decimalValue();
        String currency = textOrNull(calculation, "currency");
        ExtraordinaryBenefitEvaluationOutcome outcome =
                ExtraordinaryBenefitEvaluationOutcome.valueOf(result.decision().name());
        return new ExtraordinaryBenefitEvaluationResponse(
                request.requestReference(),
                outcome,
                businessMessage(outcome),
                result.reasonCodes(),
                evaluatedAt,
                evaluation.snapshotKey(),
                evaluation.snapshotContentHash(),
                evaluation.activationRevision(),
                result.ruleSetRef().ruleSetKey(),
                result.ruleSetRef().version(),
                result.factsDigest(),
                result.planDigest(),
                recommendedAmount,
                currency,
                textOrNull(effectPlan, "intentType"),
                textOrNull(effectPlan, "status"),
                false,
                false);
    }

    private JsonNode output(RuleEvaluationResult result, String bindingKey) {
        return result.bindingResults().stream()
                .filter(binding -> bindingKey.equals(binding.bindingKey()))
                .map(RuleBindingResult::output)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        return node.path(field).asText();
    }

    private String businessMessage(ExtraordinaryBenefitEvaluationOutcome outcome) {
        return switch (outcome) {
            case ALLOW -> "A solicitacao atende as regras avaliadas e pode seguir para persistencia e aprovacao.";
            case DENY -> "A solicitacao foi negada por uma ou mais restricoes de negocio.";
            case NOT_APPLICABLE -> "O programa avaliado nao se aplica ao contexto informado.";
            case INCONCLUSIVE -> "Faltam fatos obrigatorios para concluir a avaliacao com seguranca.";
            case TECHNICAL_ERROR -> "A avaliacao terminou em falha tecnica controlada e nenhum efeito foi executado.";
        };
    }
}
