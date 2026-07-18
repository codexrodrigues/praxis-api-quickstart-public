package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.praxisplatform.config.service.DomainRuleImplementationCatalog;
import org.praxisplatform.config.service.DomainRuleSnapshotReader;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleExecutorResult;
import org.praxisplatform.rules.contract.RuleExtensionTrust;
import org.praxisplatform.rules.contract.RuleImplementationRef;
import org.praxisplatform.rules.contract.TransformationDraft;
import org.praxisplatform.rules.contract.TransformationOperation;
import org.praxisplatform.rules.contract.TransformationValue;
import org.praxisplatform.rules.contract.TransformationValueType;
import org.praxisplatform.rules.runtime.RuleBindingExecutor;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.runtime.RuleExecutorContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Spring host wiring for the QL-03 snapshot loader; no engine or control-plane semantics are redefined here. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ExtraordinaryGrantRuleLabConfiguration {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CUSTOMER_ELIGIBILITY_KEY =
            "customer:extraordinary-grant-additional-eligibility";
    private static final RuleExtensionTrust CUSTOMER_ELIGIBILITY_LAB_TRUST = new RuleExtensionTrust(
            "14999949F81083650CF9CDB1636EB73AC987422D1E36794C70875AD21F2D5763",
            "lab-fixture:customer-policy-signer",
            "lab-policy:customer-extension-v1",
            "4486DB22584B4E667D87A9E0FA20A208A13CB2580F53931950DC5855975B9EC2");

    /** Registers exact, pure host implementations referenced by the pilot RuleSet. */
    @Bean("extraordinaryGrantRuleExecutorRegistry")
    RuleBindingExecutorRegistry extraordinaryGrantRuleExecutorRegistry() {
        return new RuleBindingExecutorRegistry(List.of(
                new ExtraordinaryGrantCustomerEligibilityExecutor(),
                new ExtraordinaryGrantAmountTransformationExecutor(),
                new ExtraordinaryGrantEffectPlanExecutor()));
    }

    /**
     * Admits only the exact Java supply chain for the configured pilot scope.
     *
     * <p>The customer evidence is an explicit laboratory fixture. A corporate host must replace
    * it with digests and signer evidence produced by its artifact verification pipeline.</p>
     */
    @Bean
    DomainRuleImplementationCatalog extraordinaryGrantRuleImplementationCatalog(
            @Value("${praxis.rule-lab.snapshot.tenant-id:desenv}") String tenantId,
            @Value("${praxis.rule-lab.snapshot.environment:local}") String environment) {
        List<RuleImplementationRef> admitted = List.of(
                new RuleImplementationRef(CUSTOMER_ELIGIBILITY_KEY, "1.0.0", CUSTOMER_ELIGIBILITY_LAB_TRUST),
                new RuleImplementationRef("benefits:extraordinary-grant-amount-transformation", "1.0.0"),
                new RuleImplementationRef("benefits:extraordinary-grant-effect-plan", "1.0.0"));
        return scope -> {
            boolean exactScope = tenantId.equals(scope.tenantId())
                    && environment.equals(scope.environment())
                    && ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY.equals(scope.ownerServiceKey());
            return exactScope ? admitted : List.of();
        };
    }

    /** Owns the host's atomic last-known-good compiled snapshot reference. */
    @Bean
    ExtraordinaryGrantRuleSnapshotRuntime extraordinaryGrantRuleSnapshotRuntime(
            @Qualifier("extraordinaryGrantRuleExecutorRegistry") RuleBindingExecutorRegistry registry) {
        return new ExtraordinaryGrantRuleSnapshotRuntime(registry);
    }

    @Bean("extraordinaryGrantRuleClock")
    Clock extraordinaryGrantRuleClock() {
        return Clock.systemUTC();
    }

    /** Loads only the explicitly configured tenant/environment head from the Config Starter boundary. */
    @Bean
    @ConditionalOnProperty(
            prefix = "praxis.rule-lab.snapshot",
            name = "enabled",
            havingValue = "true")
    ExtraordinaryGrantRuleSnapshotLoader extraordinaryGrantRuleSnapshotLoader(
            DomainRuleSnapshotReader reader,
            ExtraordinaryGrantRuleSnapshotRuntime runtime,
            @Value("${praxis.rule-lab.snapshot.tenant-id}") String tenantId,
            @Value("${praxis.rule-lab.snapshot.environment}") String environment,
            @Qualifier("extraordinaryGrantRuleClock") Clock clock) {
        return new ExtraordinaryGrantRuleSnapshotLoader(reader, runtime, tenantId, environment, clock);
    }

    /** Attempts the first load after the application context is ready without making startup depend on head availability. */
    @Bean
    @ConditionalOnProperty(
            prefix = "praxis.rule-lab.snapshot",
            name = "enabled",
            havingValue = "true")
    ApplicationRunner extraordinaryGrantRuleSnapshotInitialLoad(ExtraordinaryGrantRuleSnapshotLoader loader) {
        return arguments -> loader.refreshNow();
    }

    /** Exposes the narrow evaluation boundary backed exclusively by the active governed snapshot. */
    @Bean
    ExtraordinaryGrantRuleLabService extraordinaryGrantRuleLabService(
            ExtraordinaryGrantRuleSnapshotRuntime runtime) {
        return new ExtraordinaryGrantRuleLabService(runtime);
    }

    /** Projects the neutral engine boundary into the QL-04 business evaluation contract. */
    @Bean
    ExtraordinaryBenefitEvaluationService extraordinaryBenefitEvaluationService(
            ExtraordinaryGrantRuleLabService ruleLabService,
            ObjectMapper objectMapper,
            @Qualifier("extraordinaryGrantRuleClock") Clock clock) {
        return new ExtraordinaryBenefitEvaluationService(
                ruleLabService,
                objectMapper,
                clock,
                new ExtraordinaryBenefitTransformationMaterializer());
    }

    /**
     * Acquires host-owned facts from the operational datasource under repeatable-read isolation.
     * The adapter is opt-in and does not expose a new HTTP endpoint or enable shadow execution.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "praxis.rule-lab.authoritative-facts",
            name = "enabled",
            havingValue = "true")
    ExtraordinaryBenefitFactProvider extraordinaryBenefitFactProvider(
            @Qualifier("apiJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("apiTransactionManager") PlatformTransactionManager transactionManager,
            @Value("${praxis.rule-lab.authoritative-facts.scope-hmac-key}") String scopeHmacKey) {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        transactions.setReadOnly(true);
        transactions.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return new JdbcExtraordinaryBenefitFactProvider(
                jdbcTemplate, transactions, new RuleFactScopeDigester(scopeHmacKey));
    }

    /** Internal composition boundary that prevents server-owned eligibility facts from entering through the caller DTO. */
    @Bean
    @ConditionalOnProperty(
            prefix = "praxis.rule-lab.authoritative-facts",
            name = "enabled",
            havingValue = "true")
    ExtraordinaryBenefitAuthoritativeEvaluationService extraordinaryBenefitAuthoritativeEvaluationService(
            ExtraordinaryBenefitFactProvider factProvider,
            ExtraordinaryBenefitEvaluationService evaluationService) {
        return new ExtraordinaryBenefitAuthoritativeEvaluationService(factProvider, evaluationService);
    }

    /** Resolves tenant, environment and organization only from host configuration. */
    @Bean
    @ConditionalOnProperty(
            prefix = "praxis.rule-lab.authoritative-facts",
            name = "enabled",
            havingValue = "true")
    ExtraordinaryBenefitFactLookupFactory extraordinaryBenefitFactLookupFactory(
            @Value("${praxis.rule-lab.snapshot.tenant-id}") String tenantId,
            @Value("${praxis.rule-lab.snapshot.environment}") String environment,
            @Value("${praxis.rule-lab.authoritative-facts.organization-key}") String organizationKey) {
        return new ExtraordinaryBenefitFactLookupFactory(tenantId, environment, organizationKey);
    }

    /** Baseline independente: nao usa RuleSet, snapshot, banco nem resposta do candidato. */
    @Bean("extraordinaryBenefitSyntheticBaseline")
    ExtraordinaryBenefitShadowDecisionEvaluator extraordinaryBenefitSyntheticBaseline() {
        return new ExtraordinaryBenefitSyntheticBaseline();
    }

    /** Adapta a avaliacao Praxis para a projecao minima comparavel. */
    @Bean("extraordinaryBenefitCandidateEvaluator")
    ExtraordinaryBenefitShadowDecisionEvaluator extraordinaryBenefitCandidateEvaluator(
            ExtraordinaryBenefitEvaluationService evaluationService) {
        return (request, permissions, nowUtc, userTimeZone) ->
                ExtraordinaryBenefitShadowDecision.candidate(
                        evaluationService.evaluateAt(request, permissions, nowUtc));
    }

    /** Pool virtual limitado evita que uma rajada shadow consuma recursos sem bound. */
    @Bean(value = "extraordinaryBenefitShadowExecutor", destroyMethod = "shutdownNow")
    ExecutorService extraordinaryBenefitShadowExecutor() {
        return new ThreadPoolExecutor(
                2,
                8,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(128),
                Thread.ofVirtual().name("extraordinary-benefit-shadow-", 0).factory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** Orquestra comparacao limitada e observabilidade de baixa cardinalidade. */
    @Bean
    ExtraordinaryBenefitShadowComparisonService extraordinaryBenefitShadowComparisonService(
            @Qualifier("extraordinaryBenefitSyntheticBaseline") ExtraordinaryBenefitShadowDecisionEvaluator baseline,
            @Qualifier("extraordinaryBenefitCandidateEvaluator") ExtraordinaryBenefitShadowDecisionEvaluator candidate,
            @Qualifier("extraordinaryBenefitShadowExecutor") ExecutorService executor,
            MeterRegistry meterRegistry,
            @Qualifier("extraordinaryGrantRuleClock") Clock clock,
            @Value("${praxis.rule-lab.shadow.timeout-ms:1000}") long timeoutMs) {
        return new ExtraordinaryBenefitShadowComparisonService(
                baseline, candidate, executor, meterRegistry, clock, Duration.ofMillis(timeoutMs));
    }

    /** Contributes safe readiness diagnostics when the Rule Lab is explicitly enabled. */
    @Bean
    @ConditionalOnProperty(
            prefix = "praxis.rule-lab.snapshot",
            name = "enabled",
            havingValue = "true")
    ExtraordinaryGrantRuleSnapshotHealthIndicator extraordinaryGrantRuleSnapshotHealthIndicator(
            ExtraordinaryGrantRuleSnapshotRuntime runtime) {
        return new ExtraordinaryGrantRuleSnapshotHealthIndicator(runtime);
    }

    private static final class ExtraordinaryGrantAmountTransformationExecutor implements RuleBindingExecutor {
        @Override
        public String implementationKey() {
            return "benefits:extraordinary-grant-amount-transformation";
        }

        @Override
        public String implementationVersion() {
            return "1.0.0";
        }

        @Override
        public RuleExecutorResult evaluate(RuleExecutorContext context) {
            var amount = context.facts()
                    .path("request")
                    .path("requestedAmount")
                    .decimalValue()
                    .setScale(2, RoundingMode.HALF_EVEN);
            TransformationDraft proposal = new TransformationDraft(
                    ExtraordinaryBenefitTransformationMaterializer.PROPOSAL_KEY,
                    ExtraordinaryBenefitTransformationMaterializer.TARGET_PATH,
                    ExtraordinaryBenefitTransformationMaterializer.SCHEMA_REF,
                    TransformationOperation.SET,
                    TransformationValue.absent(),
                    TransformationValue.of(
                            TransformationValueType.NUMBER,
                            JSON.getNodeFactory().numberNode(amount)),
                    ExtraordinaryBenefitTransformationMaterializer.REASON_CODE);
            return new RuleExecutorResult(RuleDecision.ALLOW, List.of(), null, List.of(proposal));
        }
    }

    private static final class ExtraordinaryGrantCustomerEligibilityExecutor implements RuleBindingExecutor {
        @Override
        public String implementationKey() {
            return CUSTOMER_ELIGIBILITY_KEY;
        }

        @Override
        public String implementationVersion() {
            return "1.0.0";
        }

        @Override
        public RuleExtensionTrust extensionTrust() {
            return CUSTOMER_ELIGIBILITY_LAB_TRUST;
        }

        @Override
        public RuleExecutorResult evaluate(RuleExecutorContext context) {
            return context.facts().path("customer").path("additionalEligible").asBoolean(false)
                    ? RuleExecutorResult.allow()
                    : RuleExecutorResult.of(RuleDecision.DENY, "CUSTOMER_POLICY_RESTRICTED");
        }
    }

    private static final class ExtraordinaryGrantEffectPlanExecutor implements RuleBindingExecutor {
        @Override
        public String implementationKey() {
            return "benefits:extraordinary-grant-effect-plan";
        }

        @Override
        public String implementationVersion() {
            return "1.0.0";
        }

        @Override
        public RuleExecutorResult evaluate(RuleExecutorContext context) {
            ObjectNode output = JSON.createObjectNode();
            output.put("intentType", "REGISTER_EXTRAORDINARY_GRANT");
            output.put("operationKey", context.ruleSetRef().operationKey());
            output.put("status", "PLANNED_NOT_EXECUTED");
            return new RuleExecutorResult(RuleDecision.ALLOW, List.of(), output);
        }
    }
}
