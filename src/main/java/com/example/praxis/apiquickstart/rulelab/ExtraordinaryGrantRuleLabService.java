package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import org.praxisplatform.rules.contract.RuleEvaluationResult;

/**
 * Service-level boundary for the neutral extraordinary-grant Rule Lab pilot.
 *
 * <p>The host resolves and freezes facts before this service is called. The
 * service performs no I/O, persistence, snapshot selection, or effect
 * execution. It evaluates one atomic last-known-good plan selected by the
 * host snapshot runtime.</p>
 */
public final class ExtraordinaryGrantRuleLabService {
    private final ExtraordinaryGrantRuleSnapshotRuntime runtime;

    /** Creates the service over the host's atomic last-known-good snapshot runtime. */
    public ExtraordinaryGrantRuleLabService(ExtraordinaryGrantRuleSnapshotRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime is required");
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
        return evaluateWithSnapshot(facts, nowUtc, userTimeZone).result();
    }

    /** Evaluates while preserving the exact snapshot identity selected for this call. */
    public ExtraordinaryGrantRuleEvaluation evaluateWithSnapshot(
            JsonNode facts, Instant nowUtc, ZoneId userTimeZone) {
        Objects.requireNonNull(facts, "facts are required");
        Objects.requireNonNull(nowUtc, "nowUtc is required");
        Objects.requireNonNull(userTimeZone, "userTimeZone is required");
        return runtime.evaluateWithSnapshot(facts, nowUtc, userTimeZone);
    }

    /** Returns safe loader/cache diagnostics for operational readiness checks. */
    public ExtraordinaryGrantRuleSnapshotStatus snapshotStatus() {
        return runtime.status();
    }
}
