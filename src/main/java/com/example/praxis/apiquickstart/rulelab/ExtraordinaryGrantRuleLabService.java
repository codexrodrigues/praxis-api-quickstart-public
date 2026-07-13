package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.praxisplatform.rules.runtime.PraxisRuleSetEngine;

/**
 * Service-level boundary for the neutral extraordinary-grant Rule Lab pilot.
 *
 * <p>The host resolves and freezes facts before this service is called. The
 * service performs no I/O, persistence, snapshot selection, or effect
 * execution; those responsibilities remain gated for later QL waves.</p>
 */
public final class ExtraordinaryGrantRuleLabService {
    private final PraxisRuleSetEngine engine;
    private final RuleDecisionPlan plan;

    /** Creates the service from one immutable plan and its compatible engine. */
    public ExtraordinaryGrantRuleLabService(PraxisRuleSetEngine engine, RuleDecisionPlan plan) {
        this.engine = Objects.requireNonNull(engine, "engine is required");
        this.plan = Objects.requireNonNull(plan, "plan is required");
    }

    /**
     * Evaluates one frozen business-facts snapshot under explicit temporal context.
     *
     * @param facts host-resolved facts for request, actor, worker, program, customer, payment, and budget
     * @param nowUtc frozen evaluation instant
     * @param userTimeZone explicit business time zone
     * @return deterministic RuleSet result; planned effect data is never executed here
     */
    public RuleEvaluationResult evaluate(JsonNode facts, Instant nowUtc, ZoneId userTimeZone) {
        Objects.requireNonNull(facts, "facts are required");
        Objects.requireNonNull(nowUtc, "nowUtc is required");
        Objects.requireNonNull(userTimeZone, "userTimeZone is required");
        return engine.evaluate(plan, facts, nowUtc.toString(), userTimeZone.getId());
    }
}
