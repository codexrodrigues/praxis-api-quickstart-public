package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.core.entity.ResourceActionExecution;
import com.example.praxis.apiquickstart.core.service.ResourceActionExecutionService;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationOutcome;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitRequestResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Host-only P2F-ADR-10 proof for a statement-level atomic unit.
 *
 * <p>This service intentionally has no controller. It demonstrates that every item captures one
 * immutable snapshot reference and instant, joins one transaction, crosses an aggregate local-flush barrier and leaves
 * no implicit operation state after success or rollback.</p>
 */
@Service
public class ExtraordinaryBenefitStatementOperationService {
    private static final int MAX_ITEMS = 50;

    private final ExtraordinaryBenefitEvaluationService evaluationService;
    private final ExtraordinaryBenefitPersistenceItemService persistenceItemService;
    private final ExtraordinaryBenefitStatementBarrier statementBarrier;
    private final ExtraordinaryGrantRuleSnapshotRuntime snapshotRuntime;
    private final RuleLabOperationScopeRegistry scopeRegistry;
    private final ExtraordinaryBenefitStatementOutboxWriter outboxWriter;
    private final ResourceActionExecutionService executionService;
    private final Clock clock;
    private final String tenantId;
    private final String environment;

    public ExtraordinaryBenefitStatementOperationService(
            ExtraordinaryBenefitEvaluationService evaluationService,
            ExtraordinaryBenefitPersistenceItemService persistenceItemService,
            ExtraordinaryBenefitStatementBarrier statementBarrier,
            ExtraordinaryGrantRuleSnapshotRuntime snapshotRuntime,
            RuleLabOperationScopeRegistry scopeRegistry,
            ExtraordinaryBenefitStatementOutboxWriter outboxWriter,
            ResourceActionExecutionService executionService,
            @Qualifier("extraordinaryGrantRuleClock") Clock clock,
            @Value("${praxis.rule-lab.snapshot.tenant-id}") String tenantId,
            @Value("${praxis.rule-lab.snapshot.environment}") String environment) {
        this.evaluationService = evaluationService;
        this.persistenceItemService = persistenceItemService;
        this.statementBarrier = statementBarrier;
        this.snapshotRuntime = snapshotRuntime;
        this.scopeRegistry = scopeRegistry;
        this.outboxWriter = outboxWriter;
        this.executionService = executionService;
        this.clock = clock;
        this.tenantId = requireText(tenantId, "tenantId");
        this.environment = requireText(environment, "environment");
    }

    @Transactional
    public ExtraordinaryBenefitStatementOperationResult executeReserved(
            ResourceActionExecution execution,
            List<ExtraordinaryBenefitEvaluationRequest> requestedItems,
            Set<String> actorPermissions,
            String actorSubject,
            String correlationId) {
        List<ExtraordinaryBenefitEvaluationRequest> items = List.copyOf(
                Objects.requireNonNull(requestedItems, "requestedItems are required"));
        if (items.isEmpty() || items.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("STATEMENT_ATOMIC requires between 1 and 50 items");
        }
        ZoneId userTimeZone = oneTimeZone(items);
        Instant nowUtc = clock.instant();
        ExtraordinaryGrantRuleSnapshotSession snapshot = snapshotRuntime.captureSnapshot(nowUtc);

        RuleLabOperationContext context = new RuleLabOperationContext(
                Objects.requireNonNull(execution, "execution is required").getExecutionId(),
                correlationId, tenantId, environment, actorSubject,
                nowUtc, userTimeZone, snapshot.snapshotKey(), snapshot.snapshotContentHash(),
                snapshot.activationRevision(), RuleLabOperationCardinality.STATEMENT_ATOMIC, items.size());

        try (RuleLabOperationScopeRegistry.Scope scope = scopeRegistry.open(context)) {
            List<ExtraordinaryBenefitEvaluatedDecision> decisions = items.stream()
                    .map(item -> evaluationService.evaluateDecisionAt(
                            item, actorPermissions, context.nowUtc(), snapshot))
                    .toList();
            List<ExtraordinaryBenefitEvaluationResponse> evaluations = decisions.stream()
                    .map(ExtraordinaryBenefitEvaluatedDecision::response)
                    .toList();
            scope.advance(RuleLabOperationStage.OPENED, RuleLabOperationStage.EVALUATED);
            if (evaluations.stream().anyMatch(evaluation -> evaluation.outcome() != ExtraordinaryBenefitEvaluationOutcome.ALLOW)) {
                throw new IllegalStateException("STATEMENT_ATOMIC cannot persist a partially allowed statement");
            }

            List<ExtraordinaryBenefitRequestResponse> persisted = new ArrayList<>(items.size());
            for (int index = 0; index < items.size(); index++) {
                persisted.add(persistenceItemService.persistNonApplicableStatementProof(
                        items.get(index), decisions.get(index), context.actorSubject(),
                        context.correlationId(), context.operationId(), context.cardinality()));
            }
            scope.advance(RuleLabOperationStage.EVALUATED, RuleLabOperationStage.LOCAL_FLUSHED);
            int aggregateVisible = statementBarrier.verify(context, evaluations, persisted);
            scope.advance(RuleLabOperationStage.LOCAL_FLUSHED, RuleLabOperationStage.AGGREGATE_VERIFIED);

            var outboxMessageId = outboxWriter.append(context, persisted);
            scope.advance(RuleLabOperationStage.AGGREGATE_VERIFIED, RuleLabOperationStage.OUTBOX_APPENDED);
            var result = new ExtraordinaryBenefitStatementOperationResult(
                    context.operationId(), outboxMessageId,
                    context.cardinality(), RuleLabOperationBarrier.LOCAL_COMMITTED,
                    context.itemCount(), aggregateVisible, persisted);
            executionService.complete(execution, result);
            scope.advance(RuleLabOperationStage.OUTBOX_APPENDED, RuleLabOperationStage.IDEMPOTENCY_COMPLETED);
            return result;
        }
    }

    private ZoneId oneTimeZone(List<ExtraordinaryBenefitEvaluationRequest> items) {
        ZoneId selected = ZoneId.of(items.getFirst().userTimeZone());
        if (items.stream().map(ExtraordinaryBenefitEvaluationRequest::userTimeZone)
                .map(ZoneId::of).anyMatch(zoneId -> !selected.equals(zoneId))) {
            throw new IllegalArgumentException("STATEMENT_ATOMIC requires one userTimeZone for every item");
        }
        return selected;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
