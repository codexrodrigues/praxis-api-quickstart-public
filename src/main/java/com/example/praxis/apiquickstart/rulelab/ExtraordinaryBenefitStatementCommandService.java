package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.core.entity.ResourceActionExecution;
import com.example.praxis.apiquickstart.core.service.ResourceActionExecutionService;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.praxisplatform.uischema.action.ActionScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Non-transactional command facade for statement reservation, replay and failure recording.
 *
 * <p>The reservation commits before the domain transaction. The operation service then commits
 * items, outbox and the completed replay payload atomically. A rollback is recorded as FAILED in a
 * separate transaction without preserving partial business writes.</p>
 */
@Service
final class ExtraordinaryBenefitStatementCommandService {
    static final String RESOURCE_KEY = "extraordinary-benefit-requests";
    static final String ACTION_ID = "evaluate-statement";

    private final ResourceActionExecutionService executionService;
    private final ExtraordinaryBenefitStatementOperationService operationService;
    private final ObjectMapper objectMapper;
    private final String targetId;

    ExtraordinaryBenefitStatementCommandService(
            ResourceActionExecutionService executionService,
            ExtraordinaryBenefitStatementOperationService operationService,
            ObjectMapper objectMapper,
            @Value("${praxis.rule-lab.snapshot.tenant-id}") String tenantId,
            @Value("${praxis.rule-lab.snapshot.environment}") String environment) {
        this.executionService = executionService;
        this.operationService = operationService;
        this.objectMapper = objectMapper;
        this.targetId = scopeTarget(
                requireText(tenantId, "tenantId"), requireText(environment, "environment"));
    }

    ExtraordinaryBenefitStatementOperationResult execute(
            List<ExtraordinaryBenefitEvaluationRequest> requestedItems,
            Set<String> actorPermissions,
            String actorSubject,
            String correlationId,
            String idempotencyKey) {
        List<ExtraordinaryBenefitEvaluationRequest> items = List.copyOf(
                Objects.requireNonNull(requestedItems, "requestedItems are required"));
        String actor = requireText(actorSubject, "actorSubject");
        String correlation = requireText(correlationId, "correlationId");
        String key = requireText(idempotencyKey, "idempotencyKey");
        if (actor.length() > 255 || correlation.length() > 255) {
            throw new IllegalArgumentException("actorSubject and correlationId cannot exceed 255 characters");
        }
        if (key.length() > 255) {
            throw new IllegalArgumentException("idempotencyKey cannot exceed 255 characters");
        }
        Set<String> frozenPermissions = actorPermissions == null
                ? Set.of()
                : java.util.Collections.unmodifiableSet(new TreeSet<>(actorPermissions));
        ExtraordinaryBenefitStatementCommandFingerprint command =
                ExtraordinaryBenefitStatementCommandFingerprint.of(items, frozenPermissions);

        var replay = executionService.findCompletedReplay(
                RESOURCE_KEY, targetId, ACTION_ID, key, command, actor);
        if (replay.isPresent()) {
            return restore(replay.get());
        }

        ResourceActionExecution execution = executionService.reserve(
                        RESOURCE_KEY, targetId, ACTION_ID, ActionScope.COLLECTION, key,
                        command, correlation, actor)
                .orElseThrow(() -> new IllegalStateException("STATEMENT_ATOMIC requires idempotent reservation"));
        if ("COMPLETED".equals(execution.getExecutionStatus())) {
            return restore(execution);
        }

        try {
            return operationService.executeReserved(
                    execution, items, frozenPermissions, actor, correlation);
        } catch (RuntimeException failure) {
            try {
                executionService.fail(
                        execution,
                        "STATEMENT_TRANSACTION_FAILED",
                        "STATEMENT_ATOMIC rolled back before local commit");
            } catch (RuntimeException failureRecordingError) {
                failure.addSuppressed(failureRecordingError);
            }
            throw failure;
        }
    }

    private ExtraordinaryBenefitStatementOperationResult restore(ResourceActionExecution execution) {
        try {
            return objectMapper.treeToValue(
                    execution.getResponsePayload(), ExtraordinaryBenefitStatementOperationResult.class);
        } catch (Exception invalidStoredResult) {
            throw new IllegalStateException("Unable to restore STATEMENT_ATOMIC replay result", invalidStoredResult);
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String scopeTarget(String tenantId, String environment) {
        try {
            byte[] identity = (tenantId + "\u0000" + environment).getBytes(StandardCharsets.UTF_8);
            return "scope:" + java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(identity));
        } catch (Exception unavailableDigest) {
            throw new IllegalStateException("Unable to create tenant/environment idempotency scope", unavailableDigest);
        }
    }
}
