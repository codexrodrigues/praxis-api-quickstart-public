package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.core.service.ResourceActionTransitionService;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitBatchEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitBatchEvaluationResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitBatchItemResult;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitAuthoritativeEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationCommandResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationOutcome;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitTransitionRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitTransitionResponse;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import org.praxisplatform.uischema.action.ActionScope;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Orquestra persistencia e lifecycle; nenhuma decisao deterministica e reimplementada aqui. */
@Service
public class ExtraordinaryBenefitWorkflowService {
    private static final String RESOURCE_KEY = "human-resources.extraordinary-benefit-requests";

    private final ExtraordinaryBenefitPersistenceItemService persistenceItemService;
    private final ExtraordinaryBenefitRequestRepository requestRepository;
    private final ExtraordinaryBenefitGrantEffectRepository effectRepository;
    private final ExtraordinaryBenefitRequestMapper mapper;
    private final ResourceActionTransitionService transitionService;
    private final ObjectProvider<ExtraordinaryBenefitAuthoritativeEvaluationService> authoritativeEvaluationProvider;
    private final ObjectProvider<ExtraordinaryBenefitFactLookupFactory> factLookupFactoryProvider;
    private final Clock clock;

    public ExtraordinaryBenefitWorkflowService(
            ExtraordinaryBenefitPersistenceItemService persistenceItemService,
            ExtraordinaryBenefitRequestRepository requestRepository,
            ExtraordinaryBenefitGrantEffectRepository effectRepository,
            ExtraordinaryBenefitRequestMapper mapper,
            ResourceActionTransitionService transitionService,
            ObjectProvider<ExtraordinaryBenefitAuthoritativeEvaluationService> authoritativeEvaluationProvider,
            ObjectProvider<ExtraordinaryBenefitFactLookupFactory> factLookupFactoryProvider,
            @org.springframework.beans.factory.annotation.Qualifier("extraordinaryGrantRuleClock") Clock clock) {
        this.persistenceItemService = persistenceItemService;
        this.requestRepository = requestRepository;
        this.effectRepository = effectRepository;
        this.mapper = mapper;
        this.transitionService = transitionService;
        this.authoritativeEvaluationProvider = authoritativeEvaluationProvider;
        this.factLookupFactoryProvider = factLookupFactoryProvider;
        this.clock = clock;
    }

    public ExtraordinaryBenefitEvaluationCommandResponse evaluateAndPersist(
            ExtraordinaryBenefitAuthoritativeEvaluationRequest request,
            Set<String> permissions,
            String actorSubject,
            String correlationId) {
        return evaluateAndPersist(
                request, permissions, actorSubject, correlationId, RuleLabOperationCardinality.SINGLE_ITEM);
    }

    private ExtraordinaryBenefitEvaluationCommandResponse evaluateAndPersist(
            ExtraordinaryBenefitAuthoritativeEvaluationRequest request,
            Set<String> permissions,
            String actorSubject,
            String correlationId,
            RuleLabOperationCardinality cardinality) {
        Instant evaluatedAt = clock.instant();
        ExtraordinaryBenefitAuthoritativeEvaluationResult authoritative = evaluateAuthoritatively(
                command(request), request.factReference(), evaluatedAt, permissions);
        ExtraordinaryBenefitEvaluatedDecision decision = authoritative.decision();
        ExtraordinaryBenefitEvaluationResponse evaluation = decision.response();
        if (evaluation.outcome() != ExtraordinaryBenefitEvaluationOutcome.ALLOW) {
            return new ExtraordinaryBenefitEvaluationCommandResponse(evaluation, null);
        }
        var resource = persistenceItemService.persistAllowed(
                authoritative.resolvedRequest(), decision, request.factReference(), authoritative.factProvenance(),
                actorSubject, correlationId, null, cardinality);
        return new ExtraordinaryBenefitEvaluationCommandResponse(mapper.markPersisted(evaluation), resource);
    }

    public ExtraordinaryBenefitBatchEvaluationResponse evaluateBatch(
            ExtraordinaryBenefitBatchEvaluationRequest batch,
            Set<String> permissions,
            String actorSubject,
            String correlationId) {
        var items = new ArrayList<ExtraordinaryBenefitBatchItemResult>();
        int persisted = 0;
        for (ExtraordinaryBenefitAuthoritativeEvaluationRequest request : batch.requests()) {
            try {
                ExtraordinaryBenefitEvaluationCommandResponse result =
                        evaluateAndPersist(
                                request, permissions, actorSubject, correlationId,
                                RuleLabOperationCardinality.ITEM_INDEPENDENT);
                boolean saved = result.resource() != null;
                if (saved) persisted++;
                items.add(new ExtraordinaryBenefitBatchItemResult(
                        request.requestReference(), result.evaluation().outcome(), saved,
                        saved ? result.resource().id() : null,
                        saved ? "PERSISTED" : result.evaluation().outcome().name(),
                        result.evaluation().businessMessage()));
            } catch (DataIntegrityViolationException duplicate) {
                items.add(new ExtraordinaryBenefitBatchItemResult(
                        request.requestReference(), null, false, null, "DUPLICATE_REFERENCE",
                        "A referencia ja identifica outra solicitacao persistida."));
            } catch (DateTimeException invalidTimeZone) {
                items.add(new ExtraordinaryBenefitBatchItemResult(
                        request.requestReference(), null, false, null, "INVALID_TIME_ZONE",
                        "O fuso informado nao e um identificador IANA valido."));
            }
        }
        return new ExtraordinaryBenefitBatchEvaluationResponse(
                false, items.size(), persisted, items.size() - persisted, items);
    }

    @Transactional
    public ExtraordinaryBenefitTransitionResponse submit(
            Long id, ExtraordinaryBenefitTransitionRequest command, String actor, String correlationId) {
        return transition(id, command, actor, correlationId,
                ExtraordinaryBenefitLifecycleStatus.EVALUATED,
                ExtraordinaryBenefitLifecycleStatus.SUBMITTED, "submit", false, Set.of());
    }

    @Transactional
    public ExtraordinaryBenefitTransitionResponse approve(
            Long id, ExtraordinaryBenefitTransitionRequest command, String actor, String correlationId) {
        return transition(id, command, actor, correlationId,
                ExtraordinaryBenefitLifecycleStatus.SUBMITTED,
                ExtraordinaryBenefitLifecycleStatus.APPROVED, "approve", false, Set.of());
    }

    @Transactional
    public ExtraordinaryBenefitTransitionResponse apply(
            Long id,
            ExtraordinaryBenefitTransitionRequest command,
            Set<String> permissions,
            String actor,
            String correlationId) {
        return transition(id, command, actor, correlationId,
                ExtraordinaryBenefitLifecycleStatus.APPROVED,
                ExtraordinaryBenefitLifecycleStatus.APPLIED, "apply", true, permissions);
    }

    private ExtraordinaryBenefitTransitionResponse transition(
            Long id,
            ExtraordinaryBenefitTransitionRequest command,
            String actor,
            String correlationId,
            ExtraordinaryBenefitLifecycleStatus expected,
            ExtraordinaryBenefitLifecycleStatus target,
            String actionId,
            boolean executeEffect,
            Set<String> permissions) {
        ExtraordinaryBenefitRequestEntity entity = requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benefit request not found."));
        ExtraordinaryBenefitLifecycleStatus previous = entity.getLifecycleStatus();
        if (previous != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Action " + actionId + " requires state " + expected + " but found " + previous + ".");
        }
        Long versionBefore = entity.getVersion();
        Instant now = clock.instant();
        UUID effectExecutionId = null;
        if (executeEffect) {
            ExtraordinaryBenefitAuthoritativeEvaluationResult revalidated =
                    revalidateBeforeApply(entity, now, permissions);
            ExtraordinaryBenefitEvaluationResponse current = revalidated.decision().response();
            RuleFactProvenance provenance = revalidated.factProvenance();
            effectExecutionId = UUID.randomUUID();
            effectRepository.saveAndFlush(new ExtraordinaryBenefitGrantEffect(
                    effectExecutionId, entity.getId(), entity.getRequestReference(),
                    entity.getPlannedEffectIntent(), entity.getRecommendedAmount(), entity.getCurrency(), now, actor,
                    current.snapshotKey(), current.snapshotContentHash(), current.factsDigest(),
                    provenance.providerKey(), provenance.sourceRecordDigest(), provenance.sourceVersion(),
                    provenance.sourceRecordedAt(), provenance.asOf(), provenance.scopeDigest()));
            entity.setEffectStatus(ExtraordinaryBenefitEffectStatus.EXECUTED);
            entity.setAppliedAt(now);
        } else if (target == ExtraordinaryBenefitLifecycleStatus.SUBMITTED) {
            entity.setSubmittedAt(now);
        } else if (target == ExtraordinaryBenefitLifecycleStatus.APPROVED) {
            entity.setApprovedAt(now);
        }
        entity.setLifecycleStatus(target);
        entity.setLastTransitionBy(actor);
        ExtraordinaryBenefitRequestEntity saved = requestRepository.saveAndFlush(entity);
        UUID transitionId = transitionService.record(
                RESOURCE_KEY, id, actionId, ActionScope.ITEM.name(), previous.name(), target.name(),
                actionId.toUpperCase(), command.justification(), command.effectiveAt(), actor,
                correlationId, versionBefore, saved.getVersion());
        return new ExtraordinaryBenefitTransitionResponse(
                id, previous, target, saved.getVersion(), transitionId, effectExecutionId);
    }

    private ExtraordinaryBenefitAuthoritativeEvaluationResult revalidateBeforeApply(
            ExtraordinaryBenefitRequestEntity entity, Instant now, Set<String> permissions) {
        if (entity.getFactReference() == null || entity.getFactReference().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "Apply requires a request created from authoritative host facts.");
        }
        ExtraordinaryBenefitAuthoritativeEvaluationResult result = evaluateAuthoritatively(
                new ExtraordinaryBenefitAuthoritativeEvaluationCommand(
                        entity.getRequestReference(), entity.getReasonCode(), entity.getEventDate(),
                        entity.getRequestedAmount(), entity.getFactReference(), entity.getRequestedPaymentDate(),
                        entity.getUserTimeZone()),
                entity.getFactReference(), now, permissions);
        ExtraordinaryBenefitEvaluationResponse current = result.decision().response();
        boolean unchangedAuthorization = current.outcome() == ExtraordinaryBenefitEvaluationOutcome.ALLOW
                && entity.getSnapshotKey().equals(current.snapshotKey())
                && entity.getSnapshotContentHash().equals(current.snapshotContentHash())
                && entity.getSnapshotActivationRevision() == current.snapshotActivationRevision()
                && entity.getRuleSetKey().equals(current.ruleSetKey())
                && entity.getRuleSetVersion() == current.ruleSetVersion()
                && entity.getFactsDigest().equals(current.factsDigest())
                && entity.getRecommendedAmount().compareTo(current.recommendedAmount()) == 0
                && entity.getCurrency().equals(current.currency())
                && entity.getPlannedEffectIntent().equals(current.plannedEffectIntent())
                && entity.getPlanDigest().equals(current.planDigest());
        if (!unchangedAuthorization) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "Authoritative facts or the governed snapshot no longer authorize the approved effect.");
        }
        return result;
    }

    private ExtraordinaryBenefitAuthoritativeEvaluationResult evaluateAuthoritatively(
            ExtraordinaryBenefitAuthoritativeEvaluationCommand command,
            String factReference,
            Instant asOf,
            Set<String> permissions) {
        ExtraordinaryBenefitAuthoritativeEvaluationService service = authoritativeEvaluationProvider.getIfAvailable();
        ExtraordinaryBenefitFactLookupFactory lookupFactory = factLookupFactoryProvider.getIfAvailable();
        if (service == null || lookupFactory == null) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "Authoritative fact acquisition is not configured for this host.");
        }
        return service.evaluate(command, lookupFactory.create(factReference, asOf), permissions);
    }

    private ExtraordinaryBenefitAuthoritativeEvaluationCommand command(
            ExtraordinaryBenefitAuthoritativeEvaluationRequest request) {
        return new ExtraordinaryBenefitAuthoritativeEvaluationCommand(
                request.requestReference(), request.reasonCode(), request.eventDate(), request.requestedAmount(),
                request.factReference(), request.requestedPaymentDate(), request.userTimeZone());
    }
}
