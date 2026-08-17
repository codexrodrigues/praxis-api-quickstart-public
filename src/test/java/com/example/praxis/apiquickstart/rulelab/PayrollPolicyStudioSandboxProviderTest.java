package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.praxis.apiquickstart.config.AppliedReactiveDeterminationResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.praxisplatform.rules.contract.DecisionAggregationPolicy;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.DecisionSlot;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.DecisionStage;
import org.praxisplatform.rules.contract.OverridePolicy;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleExecutorRef;
import org.praxisplatform.rules.contract.RuleFailPolicy;
import org.praxisplatform.rules.contract.RuleRuntimeCompatibility;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.contract.RuleSetRef;
import org.praxisplatform.rules.contract.SlotCardinality;
import org.praxisplatform.rules.plan.PraxisRulePlanCompiler;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;

class PayrollPolicyStudioSandboxProviderTest {
    private static final Instant NOW = Instant.parse("2026-08-16T18:00:00Z");
    private static final String RULE_KEY =
            AppliedReactiveDeterminationResolver.PAYROLL_PAYMENT_DATE_DETERMINATION_KEY;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void replacesOnlyThePayrollCandidateWhilePreservingTheCapturedActivePlanAndEvidence()
            throws Exception {
        var resolver = mock(AppliedReactiveDeterminationResolver.class);
        var activePlan = new PraxisRulePlanCompiler(RuleBindingExecutorRegistry.empty())
                .compile(payrollRuleSet());
        when(resolver.capturePolicyStudioSandboxSnapshot()).thenReturn(
                new AppliedReactiveDeterminationResolver.PayrollSandboxSnapshot(
                        activePlan, "payroll-active-v1", "A".repeat(64), 7));
        var provider = new PayrollPolicyStudioSandboxProvider(resolver);

        var session = provider.prepare(
                RULE_KEY, json.readTree("{\"===\":[true,false]}"), NOW);
        var facts = json.readTree("{\"payroll\":{\"ano\":2026,\"mes\":8,\"salarioLiquido\":7250.50}}");

        assertThat(session.evaluateActive(facts, NOW, ZoneId.of("America/Sao_Paulo")).decision())
                .isEqualTo(RuleDecision.ALLOW);
        assertThat(session.evaluateCandidate(facts, NOW, ZoneId.of("America/Sao_Paulo")).decision())
                .isEqualTo(RuleDecision.DENY);
        assertThat(session.activeSnapshotKey()).isEqualTo("payroll-active-v1");
        assertThat(session.activeActivationRevision()).isEqualTo(7);
        assertThat(session.activePlan().planDigest()).isNotEqualTo(session.candidatePlan().planDigest());
    }

    @Test
    void registryRejectsAmbiguousCanonicalRuleOwnership() {
        var resolver = mock(AppliedReactiveDeterminationResolver.class);
        var provider = new PayrollPolicyStudioSandboxProvider(resolver);

        assertThatThrownBy(() -> new PolicyStudioSandboxRuleSetRegistry(List.of(provider, provider)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unique");
    }

    private RuleSetDefinition payrollRuleSet() throws Exception {
        var slot = new DecisionSlot(
                RULE_KEY, DecisionStage.DOMAIN_DECISION, SlotCardinality.SINGLE,
                OverridePolicy.FORBIDDEN, DecisionAggregationPolicy.SINGLE_RESULT);
        var binding = new DecisionBinding(
                RULE_KEY, RULE_KEY, DecisionSource.PRODUCT, null,
                RuleExecutorRef.jsonLogic(json.readTree(
                        "{\"and\":[{\">=\":[{\"var\":\"payroll.ano\"},1900]},{\">=\":[{\"var\":\"payroll.mes\"},1]}]}")),
                List.of(), 10, true, RuleDecision.DENY, "PAYROLL_PERIOD_INVALID",
                List.of("payroll.ano", "payroll.mes", "payroll.salarioLiquido"));
        return new RuleSetDefinition(
                new RuleSetRef("human-resources", "human-resources.payroll",
                        "human-resources.payroll.reactive-determinations",
                        "determine-payroll-derived-fields", 1),
                List.of("payroll"), List.of(slot), List.of(binding),
                RuleRuntimeCompatibility.current(), RuleFailPolicy.FAIL_CLOSED);
    }
}
