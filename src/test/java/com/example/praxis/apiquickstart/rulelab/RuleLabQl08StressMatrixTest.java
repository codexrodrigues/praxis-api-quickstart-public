package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Executable governance gate for the QL-08 Ergon-like stress report.
 *
 * <p>The matrix does not pretend that a synthetic host is a legacy baseline. It binds every
 * supported or partial claim to an executable Quickstart test and keeps unproved capabilities
 * explicitly owned and blocked.</p>
 */
class RuleLabQl08StressMatrixTest {
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "VERIFIED_CURRENT_CONTRACT",
            "PARTIAL_REQUIRES_CONTRACT",
            "PARTIAL_REQUIRES_IMPLEMENTATION",
            "PARTIAL_REQUIRES_EXTERNAL_EVIDENCE",
            "BLOCKED_EXTERNAL_EVIDENCE");
    private static final Set<String> REQUIRED_COMPLEXITIES = Set.of(
            "layered_product_customer_security_composition",
            "deterministic_dependency_order",
            "regional_suppression_without_global_bypass",
            "structured_business_and_technical_outcomes",
            "typed_pre_write_transformation",
            "item_and_statement_cardinality",
            "explicit_operation_state_and_cleanup",
            "transactional_effect_and_independent_audit",
            "post_write_visibility_barrier",
            "database_backed_shadow_baseline",
            "tenant_environment_isolation",
            "atomic_version_activation",
            "combined_concurrency_idempotency_and_effect_failure");

    private static JsonNode matrix;

    @BeforeAll
    static void loadMatrix() throws Exception {
        try (InputStream stream = RuleLabQl08StressMatrixTest.class.getResourceAsStream(
                "/rule-lab/rule-lab-ql08-stress-matrix.json")) {
            assertThat(stream).as("QL-08 stress matrix must be packaged with tests").isNotNull();
            matrix = new ObjectMapper().readTree(stream);
        }
    }

    @Test
    void coversEveryErgonLikeComplexityWithUniqueStableIdentity() {
        Set<String> ids = new HashSet<>();
        Set<String> complexities = new HashSet<>();

        for (JsonNode scenario : matrix.path("scenarios")) {
            assertThat(ids.add(scenario.path("id").asText())).as("duplicate scenario id").isTrue();
            assertThat(complexities.add(scenario.path("complexity").asText()))
                    .as("duplicate complexity classification")
                    .isTrue();
            assertThat(ALLOWED_STATUSES).contains(scenario.path("status").asText());
            assertThat(scenario.path("canonicalOwner").asText()).isNotBlank();
            assertThat(scenario.path("finding").asText()).isNotBlank();
        }

        assertThat(complexities).containsExactlyInAnyOrderElementsOf(REQUIRED_COMPLEXITIES);
    }

    @Test
    void bindsEveryFindingToExecutableEvidence() throws Exception {
        for (JsonNode scenario : matrix.path("scenarios")) {
            assertThat(scenario.path("evidence").isArray()).as(scenario.path("id").asText()).isTrue();
            assertThat(scenario.path("evidence")).as(scenario.path("id").asText()).isNotEmpty();
            for (JsonNode evidence : scenario.path("evidence")) {
                assertExecutableTestMethod(evidence.asText());
            }
        }
    }

    @Test
    void keepsEveryUnprovedCapabilityOwnedAndActionable() {
        for (JsonNode scenario : matrix.path("scenarios")) {
            if (!"VERIFIED_CURRENT_CONTRACT".equals(scenario.path("status").asText())) {
                assertThat(scenario.path("residualDecision").asText())
                        .as(scenario.path("id").asText())
                        .isNotBlank();
            }
        }
    }

    @Test
    void doesNotAuthorizePhase9OrRuleAuthority() {
        assertThat(matrix.path("classification").asText()).isEqualTo("P2F_13_REVIEW");
        assertThat(matrix.path("phase9Status").asText()).isEqualTo("BLOCKED");
        assertThat(matrix.path("authorityMode").asText()).isEqualTo("NONE");
        assertThat(matrix.path("fixtures").asText()).isEqualTo("FICTIONAL_ONLY");
        assertThat(matrix.path("scenarios").findValuesAsText("status"))
                .contains("BLOCKED_EXTERNAL_EVIDENCE");
    }

    private static void assertExecutableTestMethod(String selector) throws Exception {
        String[] parts = selector.split("#", 2);
        assertThat(parts).as("evidence selector must use class#method").hasSize(2);
        Class<?> testClass = Class.forName(parts[0]);
        Method method = testClass.getDeclaredMethod(parts[1]);
        assertThat(method.isAnnotationPresent(Test.class))
                .as(selector + " must reference a JUnit test")
                .isTrue();
    }
}
