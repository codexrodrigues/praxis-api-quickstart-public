package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHead;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHeadActivationType;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHeadReader;
import org.praxisplatform.config.contract.PublishedRuleSnapshotHeadScope;
import org.praxisplatform.config.service.AiPrincipalContext;
import org.praxisplatform.config.service.AiPrincipalContextResolver;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AppliedReactiveDeterminationResolverTest {

    @Mock PublishedRuleSnapshotHeadReader headReader;
    @Mock AiPrincipalContextResolver principalResolver;
    private Instant now = Instant.parse("2026-08-13T18:00:00Z");

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void pinsOneCompleteAggregateForBothDeterminationsInTheSameRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "caller-tenant-must-not-win");
        request.addHeader("X-Env", "caller-env-must-not-win");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(principalResolver.resolve(request, null, null, null))
                .thenReturn(new AiPrincipalContext("tenant-a", "payroll-user", "prod", true));
        PublishedRuleSnapshotHead v1 = head("tenant-a", "prod", 1, 1, "head-a-1", 7, 9);
        when(headReader.findActive(scope("tenant-a", "prod"))).thenReturn(Optional.of(v1));
        AppliedReactiveDeterminationResolver resolver = resolver();

        var netSalary = resolver.requirePayrollSelection();
        var paymentDate = resolver.requirePayrollPaymentDateSelection();

        assertThat(netSalary.ruleKey()).isEqualTo(PayrollReactiveDeterminationRuleSet.NET_SALARY_KEY);
        assertThat(netSalary.ruleVersion()).isEqualTo(7);
        assertThat(netSalary.operationId()).isEqualTo("determinePayrollNetSalary");
        assertThat(paymentDate.ruleKey()).isEqualTo(PayrollReactiveDeterminationRuleSet.PAYMENT_DATE_KEY);
        assertThat(paymentDate.ruleVersion()).isEqualTo(9);
        assertThat(paymentDate.operationId()).isEqualTo("determinePayrollPaymentDate");
        verify(headReader, times(1)).findActive(scope("tenant-a", "prod"));
    }

    @Test
    void exposesTheSameVerifiedAggregateAsReadOnlyPolicyStudioSandboxEvidence() {
        when(principalResolver.resolve(isNull(HttpServletRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(new AiPrincipalContext("tenant-a", "policy-author", "prod", true));
        PublishedRuleSnapshotHead active = head("tenant-a", "prod", 1, 4, "head-payroll-4", 7, 9);
        when(headReader.findActive(scope("tenant-a", "prod"))).thenReturn(Optional.of(active));

        var sandbox = resolver().capturePolicyStudioSandboxSnapshot();

        assertThat(sandbox.activePlan().definition().ref().ruleSetKey())
                .isEqualTo(PayrollReactiveDeterminationRuleSet.RULE_SET_KEY);
        assertThat(sandbox.snapshotKey()).isEqualTo(active.snapshot().snapshotKey());
        assertThat(sandbox.snapshotContentHash()).isEqualTo(active.snapshotContentHash());
        assertThat(sandbox.activationRevision()).isEqualTo(4);
    }

    @Test
    void keepsTenantHeadsIndependent() {
        when(principalResolver.resolve(isNull(HttpServletRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(
                        new AiPrincipalContext("tenant-a", "user-a", "prod", true),
                        new AiPrincipalContext("tenant-b", "user-b", "prod", true));
        PublishedRuleSnapshotHead tenantAHead = head("tenant-a", "prod", 1, 1, "head-a", 4, 5);
        PublishedRuleSnapshotHead tenantBHead = head("tenant-b", "prod", 2, 1, "head-b", 14, 15);
        when(headReader.findActive(scope("tenant-a", "prod")))
                .thenReturn(Optional.of(tenantAHead));
        when(headReader.findActive(scope("tenant-b", "prod")))
                .thenReturn(Optional.of(tenantBHead));
        AppliedReactiveDeterminationResolver resolver = resolver();

        var tenantA = resolver.requirePayrollSelection();
        var tenantB = resolver.requirePayrollSelection();

        assertThat(tenantA.ruleVersion()).isEqualTo(4);
        assertThat(tenantB.ruleVersion()).isEqualTo(14);
    }

    @Test
    void acceptsV2AndRollbackToV1OnlyThroughANewerOpaqueHead() {
        when(principalResolver.resolve(isNull(HttpServletRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(new AiPrincipalContext("tenant-a", "payroll-user", "prod", true));
        PublishedRuleSnapshotHead v1 = head("tenant-a", "prod", 1, 1, "head-1", 7, 9);
        PublishedRuleSnapshotHead v2 = head("tenant-a", "prod", 2, 2, "head-2", 8, 10);
        PublishedRuleSnapshotHead rollbackV1 = head("tenant-a", "prod", 1, 3, "head-3", 7, 9);
        PublishedRuleSnapshotHead staleV2 = head("tenant-a", "prod", 2, 2, "head-stale", 8, 10);
        when(headReader.findActive(scope("tenant-a", "prod")))
                .thenReturn(Optional.of(v1), Optional.of(v2), Optional.of(rollbackV1), Optional.of(staleV2));
        AppliedReactiveDeterminationResolver resolver = resolver();

        assertThat(resolver.requirePayrollSelection().ruleVersion()).isEqualTo(7);
        assertThat(resolver.requirePayrollSelection().ruleVersion()).isEqualTo(8);
        assertThat(resolver.requirePayrollSelection().ruleVersion()).isEqualTo(7);
        assertThatThrownBy(resolver::requirePayrollSelection)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("stale or reordered payroll head");
    }

    @Test
    void rejectsCrossTenantSnapshotWithoutReplacingTheAcceptedHead() {
        when(principalResolver.resolve(isNull(HttpServletRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(new AiPrincipalContext("tenant-a", "payroll-user", "prod", true));
        PublishedRuleSnapshotHead tenantA = head("tenant-a", "prod", 1, 1, "head-1", 7, 9);
        PublishedRuleSnapshotHead tenantB = head("tenant-b", "prod", 2, 2, "head-2", 8, 10);
        when(headReader.findActive(scope("tenant-a", "prod")))
                .thenReturn(Optional.of(tenantA))
                .thenReturn(Optional.of(tenantB))
                .thenThrow(new IllegalStateException("database unavailable"));
        AppliedReactiveDeterminationResolver resolver = resolver();

        assertThat(resolver.requirePayrollSelection().ruleVersion()).isEqualTo(7);
        assertThatThrownBy(resolver::requirePayrollSelection)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("incompatible with this host");
        assertThat(resolver.requirePayrollSelection().ruleVersion()).isEqualTo(7);
    }

    @Test
    void rejectsMissingHeadCorruptHashAndIncompleteAggregateProvenance() {
        when(principalResolver.resolve(isNull(HttpServletRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(new AiPrincipalContext("tenant-a", "payroll-user", "prod", true));
        PublishedRuleSnapshotHead valid = head("tenant-a", "prod", 1, 1, "head-1", 7, 9);
        PublishedRuleSnapshotHead corrupt = new PublishedRuleSnapshotHead(
                valid.snapshot(), "A".repeat(64), "head-2", 2,
                PublishedRuleSnapshotHeadActivationType.ACTIVE);
        PublishedRuleSnapshot incompleteSnapshot = snapshot(
                "tenant-a", "prod", 2, List.of(source(
                        PayrollReactiveDeterminationRuleSet.NET_SALARY_KEY, 8, "C")));
        PublishedRuleSnapshotHead incomplete = compiledHead(incompleteSnapshot, "head-3", 3);
        when(headReader.findActive(scope("tenant-a", "prod")))
                .thenReturn(Optional.empty(), Optional.of(corrupt), Optional.of(incomplete));
        AppliedReactiveDeterminationResolver resolver = resolver();

        assertThatThrownBy(resolver::requirePayrollSelection)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("head is required");
        assertThatThrownBy(resolver::requirePayrollSelection)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("incompatible with this host");
        assertThatThrownBy(resolver::requirePayrollSelection)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("incompatible with this host");
    }

    @Test
    void usesVerifiedLkgDuringABoundedTransientOutageAndRecoversToV2() {
        when(principalResolver.resolve(isNull(HttpServletRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(new AiPrincipalContext("tenant-a", "payroll-user", "prod", true));
        PublishedRuleSnapshotHead v1 = head("tenant-a", "prod", 1, 1, "head-1", 7, 9);
        PublishedRuleSnapshotHead v2 = head("tenant-a", "prod", 2, 2, "head-2", 8, 10);
        when(headReader.findActive(scope("tenant-a", "prod")))
                .thenReturn(Optional.of(v1))
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenReturn(Optional.of(v2));
        AppliedReactiveDeterminationResolver resolver = resolver();

        assertThat(resolver.requirePayrollSelection().ruleVersion()).isEqualTo(7);
        now = now.plusSeconds(60);
        assertThat(resolver.requirePayrollSelection().ruleVersion()).isEqualTo(7);
        assertThat(resolver.lkgStatus().mode()).isEqualTo("lkg");
        now = now.plusSeconds(60);
        assertThat(resolver.requirePayrollSelection().ruleVersion()).isEqualTo(8);
        assertThat(resolver.lkgStatus().mode()).isEqualTo("fresh");
    }

    @Test
    void expiresLkgAfterMaxStalenessAndNeverCrossesTenantScope() {
        when(principalResolver.resolve(isNull(HttpServletRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(
                        new AiPrincipalContext("tenant-a", "user-a", "prod", true),
                        new AiPrincipalContext("tenant-b", "user-b", "prod", true),
                        new AiPrincipalContext("tenant-a", "user-a", "prod", true));
        when(headReader.findActive(scope("tenant-a", "prod")))
                .thenReturn(Optional.of(head("tenant-a", "prod", 1, 1, "head-a", 7, 9)))
                .thenThrow(new IllegalStateException("database unavailable"));
        when(headReader.findActive(scope("tenant-b", "prod")))
                .thenThrow(new IllegalStateException("database unavailable"));
        AppliedReactiveDeterminationResolver resolver = resolver();

        assertThat(resolver.requirePayrollSelection().ruleVersion()).isEqualTo(7);
        now = now.plusSeconds(60);
        assertThatThrownBy(resolver::requirePayrollSelection)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no eligible last-known-good");
        now = now.plusSeconds(300);
        assertThatThrownBy(resolver::requirePayrollSelection)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no eligible last-known-good");
    }

    @Test
    void governedValidityExpiresBeforeTheConfiguredStalenessWindow() {
        when(principalResolver.resolve(isNull(HttpServletRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(new AiPrincipalContext("tenant-a", "user-a", "prod", true));
        PublishedRuleSnapshot expiring = snapshot(
                "tenant-a", "prod", 1,
                List.of(
                        source(PayrollReactiveDeterminationRuleSet.NET_SALARY_KEY, 7, "B"),
                        source(PayrollReactiveDeterminationRuleSet.PAYMENT_DATE_KEY, 9, "C")),
                now.plusSeconds(30).toString());
        when(headReader.findActive(scope("tenant-a", "prod")))
                .thenReturn(Optional.of(compiledHead(expiring, "head-expiring", 1)))
                .thenThrow(new IllegalStateException("database unavailable"));
        AppliedReactiveDeterminationResolver resolver = resolver();

        assertThat(resolver.requirePayrollSelection().ruleVersion()).isEqualTo(7);
        now = now.plusSeconds(31);
        assertThatThrownBy(resolver::requirePayrollSelection)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no eligible last-known-good");
    }

    private AppliedReactiveDeterminationResolver resolver() {
        Clock clock = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now; }
        };
        return new AppliedReactiveDeterminationResolver(
                headReader, principalResolver, Duration.ofMinutes(5),
                new ReactiveDeterminationLkgTelemetry(new SimpleMeterRegistry()), clock);
    }

    private static PublishedRuleSnapshotHeadScope scope(String tenant, String environment) {
        return new PublishedRuleSnapshotHeadScope(
                tenant, environment, PayrollReactiveDeterminationRuleSet.RULE_SET_KEY);
    }

    private static PublishedRuleSnapshotHead head(
            String tenant,
            String environment,
            int ruleSetVersion,
            long activationRevision,
            String headEtag,
            int netSalaryVersion,
            int paymentDateVersion) {
        PublishedRuleSnapshot snapshot = snapshot(
                tenant,
                environment,
                ruleSetVersion,
                List.of(
                        source(PayrollReactiveDeterminationRuleSet.NET_SALARY_KEY, netSalaryVersion, "B"),
                        source(PayrollReactiveDeterminationRuleSet.PAYMENT_DATE_KEY, paymentDateVersion, "C")));
        return compiledHead(snapshot, headEtag, activationRevision);
    }

    private static PublishedRuleSnapshotHead compiledHead(
            PublishedRuleSnapshot snapshot, String headEtag, long activationRevision) {
        var compiled = new PraxisRuleSnapshotCompiler(RuleBindingExecutorRegistry.empty())
                .compile(snapshot, PayrollReactiveDeterminationRuleSet.HOST_CONTRACT_VERSION);
        return new PublishedRuleSnapshotHead(
                compiled.snapshot(),
                compiled.snapshotContentHash(),
                headEtag,
                activationRevision,
                PublishedRuleSnapshotHeadActivationType.ACTIVE);
    }

    private static PublishedRuleSnapshot snapshot(
            String tenant,
            String environment,
            int ruleSetVersion,
            List<RuleSnapshotSource> sources) {
        return snapshot(tenant, environment, ruleSetVersion, sources, "2099-01-01T00:00:00Z");
    }

    private static PublishedRuleSnapshot snapshot(
            String tenant,
            String environment,
            int ruleSetVersion,
            List<RuleSnapshotSource> sources,
            String validUntilUtc) {
        return new PublishedRuleSnapshot(
                PublishedRuleSnapshot.SNAPSHOT_CONTRACT_VERSION,
                "payroll-" + tenant + "-v" + ruleSetVersion,
                tenant,
                environment,
                PayrollReactiveDeterminationRuleSet.OWNER_SERVICE_KEY,
                ruleSetVersion,
                "2026-08-13T12:00:00Z",
                ruleSetVersion == 1 ? null : "payroll-" + tenant + "-v" + (ruleSetVersion - 1),
                PayrollReactiveDeterminationRuleSet.HOST_CONTRACT_VERSION,
                "2020-01-01T00:00:00Z",
                validUntilUtc,
                sources,
                List.of(
                        new RuleSnapshotApproval(
                                "approval-a-" + ruleSetVersion,
                                "RULE_COMPOSITION_APPROVER",
                                "approver-a",
                                "2026-08-13T12:00:00Z",
                                "D".repeat(64)),
                        new RuleSnapshotApproval(
                                "approval-b-" + ruleSetVersion,
                                "RULE_COMPOSITION_APPROVER",
                                "approver-b",
                                "2026-08-13T12:00:00Z",
                                "D".repeat(64))),
                PayrollReactiveDeterminationRuleSet.definition(ruleSetVersion));
    }

    private static RuleSnapshotSource source(String key, int version, String hashCharacter) {
        return new RuleSnapshotSource(
                key + "-definition-" + version,
                key,
                version,
                hashCharacter.repeat(64));
    }
}
