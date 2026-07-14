package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.core.service.ResourceActionTransitionService;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitBatchEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitBatchEvaluationResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitBatchItemResult;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationCommandResponse;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationOutcome;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Orquestra persistencia e lifecycle; nenhuma decisao deterministica e reimplementada aqui. */
@Service
public class ExtraordinaryBenefitWorkflowService {
    private static final String RESOURCE_KEY = "human-resources.extraordinary-benefit-requests";

    private final ExtraordinaryBenefitEvaluationService evaluationService;
    private final ExtraordinaryBenefitPersistenceItemService persistenceItemService;
    private final ExtraordinaryBenefitRequestRepository requestRepository;
    private final ExtraordinaryBenefitGrantEffectRepository effectRepository;
    private final ExtraordinaryBenefitRequestMapper mapper;
    private final ResourceActionTransitionService transitionService;
    private final Clock clock;

    public ExtraordinaryBenefitWorkflowService(
            ExtraordinaryBenefitEvaluationService evaluationService,
            ExtraordinaryBenefitPersistenceItemService persistenceItemService,
            ExtraordinaryBenefitRequestRepository requestRepository,
            ExtraordinaryBenefitGrantEffectRepository effectRepository,
            ExtraordinaryBenefitRequestMapper mapper,
            ResourceActionTransitionService transitionService,
            @org.springframework.beans.factory.annotation.Qualifier("extraordinaryGrantRuleClock") Clock clock) {
        this.evaluationService = evaluationService;
        this.persistenceItemService = persistenceItemService;
        this.requestRepository = requestRepository;
        this.effectRepository = effectRepository;
        this.mapper = mapper;
        this.transitionService = transitionService;
        this.clock = clock;
    }

    public ExtraordinaryBenefitEvaluationCommandResponse evaluateAndPersist(
            ExtraordinaryBenefitEvaluationRequest request, Set<String> permissions, String actorSubject) {
        ExtraordinaryBenefitEvaluationResponse evaluation = evaluationService.evaluate(request, permissions);
        if (evaluation.outcome() != ExtraordinaryBenefitEvaluationOutcome.ALLOW) {
            return new ExtraordinaryBenefitEvaluationCommandResponse(evaluation, null);
        }
        var resource = persistenceItemService.persistAllowed(request, evaluation, actorSubject);
        return new ExtraordinaryBenefitEvaluationCommandResponse(mapper.markPersisted(evaluation), resource);
    }

    public ExtraordinaryBenefitBatchEvaluationResponse evaluateBatch(
            ExtraordinaryBenefitBatchEvaluationRequest batch, Set<String> permissions, String actorSubject) {
        var items = new ArrayList<ExtraordinaryBenefitBatchItemResult>();
        int persisted = 0;
        for (ExtraordinaryBenefitEvaluationRequest request : batch.requests()) {
            try {
                ExtraordinaryBenefitEvaluationCommandResponse result =
                        evaluateAndPersist(request, permissions, actorSubject);
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
                ExtraordinaryBenefitLifecycleStatus.SUBMITTED, "submit", false);
    }

    @Transactional
    public ExtraordinaryBenefitTransitionResponse approve(
            Long id, ExtraordinaryBenefitTransitionRequest command, String actor, String correlationId) {
        return transition(id, command, actor, correlationId,
                ExtraordinaryBenefitLifecycleStatus.SUBMITTED,
                ExtraordinaryBenefitLifecycleStatus.APPROVED, "approve", false);
    }

    @Transactional
    public ExtraordinaryBenefitTransitionResponse apply(
            Long id, ExtraordinaryBenefitTransitionRequest command, String actor, String correlationId) {
        return transition(id, command, actor, correlationId,
                ExtraordinaryBenefitLifecycleStatus.APPROVED,
                ExtraordinaryBenefitLifecycleStatus.APPLIED, "apply", true);
    }

    private ExtraordinaryBenefitTransitionResponse transition(
            Long id,
            ExtraordinaryBenefitTransitionRequest command,
            String actor,
            String correlationId,
            ExtraordinaryBenefitLifecycleStatus expected,
            ExtraordinaryBenefitLifecycleStatus target,
            String actionId,
            boolean executeEffect) {
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
            effectExecutionId = UUID.randomUUID();
            effectRepository.saveAndFlush(new ExtraordinaryBenefitGrantEffect(
                    effectExecutionId, entity.getId(), entity.getRequestReference(),
                    entity.getPlannedEffectIntent(), entity.getRecommendedAmount(), entity.getCurrency(), now, actor));
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
}
