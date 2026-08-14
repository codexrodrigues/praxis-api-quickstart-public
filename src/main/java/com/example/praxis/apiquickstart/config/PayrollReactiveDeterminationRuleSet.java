package com.example.praxis.apiquickstart.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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

/**
 * Host-owned aggregate contract for the two ordered payroll determinations.
 *
 * <p>The immutable RuleSet is the governed selection unit. The first binding selects the net
 * salary determination and the second depends on it before selecting the payment-date
 * determination. Form metadata remains only the tenant-neutral structural projection.</p>
 */
final class PayrollReactiveDeterminationRuleSet {
    static final String RULE_SET_KEY = "human-resources.payroll.reactive-determinations";
    static final String OWNER_SERVICE_KEY = "praxis-api-quickstart";
    static final String HOST_CONTRACT_VERSION = "quickstart/1.0";
    static final String NET_SALARY_KEY = "human-resources.payroll.net-salary";
    static final String PAYMENT_DATE_KEY = "human-resources.payroll.payment-date";
    static final String NET_SALARY_OPERATION = "determinePayrollNetSalary";
    static final String PAYMENT_DATE_OPERATION = "determinePayrollPaymentDate";

    private static final ObjectMapper JSON = new ObjectMapper();

    private PayrollReactiveDeterminationRuleSet() {
    }

    /** Returns the complete aggregate under one immutable RuleSet version. */
    static RuleSetDefinition definition(int version) {
        if (version < 1) {
            throw new IllegalArgumentException("RuleSet version must be positive");
        }
        return new RuleSetDefinition(
                new RuleSetRef(
                        "human-resources",
                        "human-resources.payroll",
                        RULE_SET_KEY,
                        "determine-payroll-derived-fields",
                        version),
                List.of("payroll"),
                List.of(
                        slot(NET_SALARY_KEY),
                        slot(PAYMENT_DATE_KEY)),
                List.of(
                        binding(
                                NET_SALARY_KEY,
                                NET_SALARY_KEY,
                                NET_SALARY_OPERATION,
                                "{\">=\":[{\"var\":\"payroll.salarioBruto\"},{\"var\":\"payroll.totalDescontos\"}]}",
                                List.of(),
                                10,
                                List.of("payroll.salarioBruto", "payroll.totalDescontos")),
                        binding(
                                PAYMENT_DATE_KEY,
                                PAYMENT_DATE_KEY,
                                PAYMENT_DATE_OPERATION,
                                "{\"and\":[{\">=\":[{\"var\":\"payroll.ano\"},1900]},{\">=\":[{\"var\":\"payroll.mes\"},1]}]}",
                                List.of(NET_SALARY_KEY),
                                20,
                                List.of("payroll.ano", "payroll.mes", "payroll.salarioLiquido"))),
                RuleRuntimeCompatibility.current(),
                RuleFailPolicy.FAIL_CLOSED);
    }

    private static DecisionSlot slot(String key) {
        return new DecisionSlot(
                key,
                DecisionStage.DOMAIN_DECISION,
                SlotCardinality.SINGLE,
                OverridePolicy.FORBIDDEN,
                DecisionAggregationPolicy.SINGLE_RESULT);
    }

    private static DecisionBinding binding(
            String bindingKey,
            String slotKey,
            String operationId,
            String expression,
            List<String> dependencies,
            int order,
            List<String> requiredFacts) {
        return new DecisionBinding(
                bindingKey,
                slotKey,
                DecisionSource.PRODUCT,
                null,
                RuleExecutorRef.jsonLogic(expression(expression)),
                dependencies,
                order,
                true,
                RuleDecision.INCONCLUSIVE,
                operationId + ":unavailable",
                requiredFacts);
    }

    private static JsonNode expression(String json) {
        try {
            return JSON.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
