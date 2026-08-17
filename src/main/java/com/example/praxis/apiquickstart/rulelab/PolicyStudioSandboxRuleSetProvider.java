package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.praxisplatform.rules.runtime.PraxisRuleSetEngine;

/** Host-owned executable boundary for one governed Policy Studio RuleSet family. */
interface PolicyStudioSandboxRuleSetProvider {
    Set<String> ruleKeys();

    PolicyStudioSandboxRuleSetSession prepare(String ruleKey, JsonNode condition, Instant nowUtc);

    record PolicyStudioSandboxRuleSetSession(
            RuleDecisionPlan candidatePlan,
            RuleDecisionPlan activePlan,
            PraxisRuleSetEngine engine,
            String activeSnapshotKey,
            String activeSnapshotContentHash,
            long activeActivationRevision) {

        public PolicyStudioSandboxRuleSetSession {
            Objects.requireNonNull(candidatePlan, "candidatePlan is required");
            Objects.requireNonNull(activePlan, "activePlan is required");
            Objects.requireNonNull(engine, "engine is required");
            if (activeSnapshotKey == null || activeSnapshotKey.isBlank()
                    || activeSnapshotContentHash == null || activeSnapshotContentHash.isBlank()
                    || activeActivationRevision < 1) {
                throw new IllegalArgumentException("active snapshot evidence must be complete");
            }
        }

        RuleEvaluationResult evaluateCandidate(JsonNode facts, Instant nowUtc, ZoneId timeZone) {
            return engine.evaluate(candidatePlan, facts, nowUtc.toString(), timeZone.getId());
        }

        RuleEvaluationResult evaluateActive(JsonNode facts, Instant nowUtc, ZoneId timeZone) {
            return engine.evaluate(activePlan, facts, nowUtc.toString(), timeZone.getId());
        }
    }
}
