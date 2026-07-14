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
import org.praxisplatform.config.service.DomainRuleSnapshotReader;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleExecutorResult;
import org.praxisplatform.rules.runtime.RuleBindingExecutor;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.runtime.RuleExecutorContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Spring host wiring for the QL-03 snapshot loader; no engine or control-plane semantics are redefined here. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ExtraordinaryGrantRuleLabConfiguration {
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Registers exact, pure host implementations referenced by the pilot RuleSet. */
    @Bean("extraordinaryGrantRuleExecutorRegistry")
    RuleBindingExecutorRegistry extraordinaryGrantRuleExecutorRegistry() {
        return new RuleBindingExecutorRegistry(List.of(
                new ExtraordinaryGrantAmountExecutor(),
                new ExtraordinaryGrantEffectPlanExecutor()));
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
        return new ExtraordinaryBenefitEvaluationService(ruleLabService, objectMapper, clock);
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

    private static final class ExtraordinaryGrantAmountExecutor implements RuleBindingExecutor {
        @Override
        public String implementationKey() {
            return "benefits:extraordinary-grant-amount";
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
            ObjectNode output = JSON.createObjectNode();
            output.put("recommendedAmount", amount);
            output.put("currency", "BRL");
            output.put("roundingMode", RoundingMode.HALF_EVEN.name());
            return new RuleExecutorResult(RuleDecision.ALLOW, List.of(), output);
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
