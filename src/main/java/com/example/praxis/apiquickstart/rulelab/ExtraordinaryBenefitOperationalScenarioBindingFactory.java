package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitAuthoritativeEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReason;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitReevaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioOperationalScenarioSelection;
import com.example.praxis.apiquickstart.rulelab.dto.PolicyStudioSandboxScenarioResult;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.praxisplatform.config.dto.DomainRuleTestScenarioResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Maps governed scenario facts to disposable commands owned by the Quickstart host adapter. */
@Component
class ExtraordinaryBenefitOperationalScenarioBindingFactory {
    private static final String FACT_REFERENCE = "QL10-FICTIONAL-001";
    private static final BigDecimal UPDATE_SEED_AMOUNT = new BigDecimal("2500.00");
    private static final Set<String> SUPPORTED_DECISIONS = Set.of("ALLOW", "DENY");

    List<ExtraordinaryBenefitOperationalScenarioBinding> create(
            PolicyStudioSandboxService.PolicyStudioSandboxPreparedRun prepared,
            List<PolicyStudioOperationalScenarioSelection> selections,
            String tenantId,
            String environment) {
        if (prepared == null || selections == null || selections.isEmpty()) {
            throw badRequest("Operational scenarios are required");
        }
        if (tenantId == null || tenantId.isBlank() || environment == null || environment.isBlank()) {
            throw new IllegalArgumentException("Server-owned tenant and environment are required");
        }
        Map<UUID, PolicyStudioOperationalScenarioSelection> byId = selections.stream()
                .collect(Collectors.toMap(
                        PolicyStudioOperationalScenarioSelection::scenarioId,
                        selection -> selection,
                        (left, right) -> { throw badRequest("Operational scenario ids must be unique"); }));
        Set<UUID> selectedIds = prepared.scenarios().stream()
                .map(DomainRuleTestScenarioResponse::id)
                .collect(Collectors.toUnmodifiableSet());
        if (!selectedIds.equals(byId.keySet())) {
            throw badRequest("Every evaluated scenario requires exactly one explicit operation mode");
        }
        Map<UUID, PolicyStudioSandboxScenarioResult> results = prepared.results().stream()
                .collect(Collectors.toMap(PolicyStudioSandboxScenarioResult::scenarioId, item -> item));
        if (!results.keySet().equals(selectedIds)) {
            throw new IllegalStateException("Sandbox preparation did not preserve scenario identity");
        }
        return prepared.scenarios().stream()
                .map(scenario -> binding(
                        scenario,
                        results.get(scenario.id()),
                        byId.get(scenario.id()),
                        prepared.recordRequest().idempotencyKey(),
                        prepared.recordRequest().userTimeZone(),
                        tenantId.trim(),
                        environment.trim()))
                .toList();
    }

    private ExtraordinaryBenefitOperationalScenarioBinding binding(
            DomainRuleTestScenarioResponse scenario,
            PolicyStudioSandboxScenarioResult result,
            PolicyStudioOperationalScenarioSelection selection,
            String idempotencyKey,
            String userTimeZone,
            String tenantId,
            String environment) {
        String expected = normalizedDecision(scenario.expectedDecision());
        if (!result.candidateMatchesExpected()
                || !result.activeMatchesExpected()
                || !result.candidateOutputMatchesExpected()
                || !result.activeOutputMatchesExpected()
                || !result.candidateReasonCodesMatchExpected()
                || !result.activeReasonCodesMatchExpected()
                || !result.candidateEffectsMatchExpected()
                || !result.activeEffectsMatchExpected()) {
            throw unprocessable(
                    "Operational compatibility proof requires candidate and active to match every governed assertion");
        }
        PolicyStudioOperationalEvidenceAdapter.OperationMode operation = operation(selection.operationMode());
        JsonNode facts = requireObject(scenario.facts(), "scenario facts");
        requireQuickstartAuthoritativeProfile(facts);

        ExtraordinaryBenefitReason reason = reason(facts.path("request").path("reasonCode"));
        LocalDate eventDate = date(facts.path("request").path("eventDate"), "request.eventDate");
        BigDecimal requestedAmount = decimal(
                facts.path("request").path("requestedAmount"), "request.requestedAmount");
        LocalDate paymentDate = date(
                facts.path("payment").path("requestedDate"), "payment.requestedDate");
        String reference = proofReference(tenantId, environment, idempotencyKey, scenario.id());
        boolean mutationExpected = "ALLOW".equals(expected);

        var seed = new ExtraordinaryBenefitAuthoritativeEvaluationRequest(
                reference,
                reason,
                eventDate,
                operation == PolicyStudioOperationalEvidenceAdapter.OperationMode.UPDATE
                        ? UPDATE_SEED_AMOUNT : requestedAmount,
                FACT_REFERENCE,
                paymentDate,
                userTimeZone);
        var update = operation == PolicyStudioOperationalEvidenceAdapter.OperationMode.UPDATE
                ? new ExtraordinaryBenefitReevaluationRequest(
                        reason, eventDate, requestedAmount, FACT_REFERENCE, paymentDate, userTimeZone)
                : null;
        return new ExtraordinaryBenefitOperationalScenarioBinding(
                scenario.id(), operation, seed, update, mutationExpected);
    }

    private void requireQuickstartAuthoritativeProfile(JsonNode facts) {
        if (!"ACTIVE".equals(text(facts.path("worker").path("status"), "worker.status"))
                || booleanValue(facts.path("grant").path("hasDuplicate"), "grant.hasDuplicate")
                || !booleanValue(facts.path("program").path("active"), "program.active")
                || decimal(facts.path("program").path("maxAmount"), "program.maxAmount")
                        .compareTo(new BigDecimal("5000.00")) != 0
                || !booleanValue(
                        facts.path("customer").path("additionalEligible"),
                        "customer.additionalEligible")
                || decimal(facts.path("budget").path("availableAmount"), "budget.availableAmount")
                        .compareTo(new BigDecimal("25000.00")) != 0) {
            throw unprocessable(
                    "Scenario facts do not match the versioned Quickstart authoritative fixture profile");
        }
        String requestedDate = text(
                facts.path("payment").path("requestedDate"), "payment.requestedDate");
        JsonNode allowedDates = facts.path("payment").path("allowedDates");
        if (!allowedDates.isArray() || !containsText(allowedDates, requestedDate)) {
            throw unprocessable(
                    "Scenario payment facts do not match the Quickstart authoritative fixture profile");
        }
        JsonNode permissions = facts.path("actor").path("permissions");
        if (!permissions.isArray() || !containsText(permissions, "benefit:request")) {
            throw unprocessable("Scenario must model the governed benefit:request permission");
        }
    }

    private boolean containsText(JsonNode array, String expected) {
        for (JsonNode item : array) {
            if (item.isTextual() && expected.equals(item.textValue())) return true;
        }
        return false;
    }

    private String normalizedDecision(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
        if (!SUPPORTED_DECISIONS.contains(normalized)) {
            throw unprocessable("Quickstart operational proof supports only ALLOW and DENY scenarios");
        }
        return normalized;
    }

    private PolicyStudioOperationalEvidenceAdapter.OperationMode operation(String value) {
        try {
            return PolicyStudioOperationalEvidenceAdapter.OperationMode.valueOf(
                    value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            throw badRequest("operationMode must be CREATE or UPDATE");
        }
    }

    private ExtraordinaryBenefitReason reason(JsonNode value) {
        try {
            return ExtraordinaryBenefitReason.valueOf(text(value, "request.reasonCode"));
        } catch (IllegalArgumentException invalid) {
            throw unprocessable("request.reasonCode is not supported by the Quickstart host adapter");
        }
    }

    private LocalDate date(JsonNode value, String field) {
        try {
            return LocalDate.parse(text(value, field));
        } catch (RuntimeException invalid) {
            throw unprocessable(field + " must be an ISO-8601 date");
        }
    }

    private BigDecimal decimal(JsonNode value, String field) {
        if (!value.isNumber()) throw unprocessable(field + " must be numeric");
        return value.decimalValue();
    }

    private boolean booleanValue(JsonNode value, String field) {
        if (!value.isBoolean()) throw unprocessable(field + " must be boolean");
        return value.booleanValue();
    }

    private String text(JsonNode value, String field) {
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw unprocessable(field + " must be text");
        }
        return value.textValue();
    }

    private JsonNode requireObject(JsonNode value, String field) {
        if (value == null || !value.isObject()) throw unprocessable(field + " must be an object");
        return value;
    }

    private String proofReference(
            String tenantId,
            String environment,
            String idempotencyKey,
            UUID scenarioId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (tenantId + "|" + environment + "|" + idempotencyKey + "|" + scenarioId)
                            .getBytes(StandardCharsets.UTF_8));
            return ExtraordinaryBenefitOperationalEvidenceProbe.OWNED_REFERENCE_PREFIX
                    + HexFormat.of().formatHex(digest, 0, 20);
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 must be available", unavailable);
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException unprocessable(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
