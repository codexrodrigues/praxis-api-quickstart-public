package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.dto.DomainRuleHostStatusRequest;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleHostStatusService;
import org.praxisplatform.config.service.DomainRuleImplementationCatalog;
import org.praxisplatform.config.service.DomainRuleImplementationCatalogFingerprint;
import org.praxisplatform.config.service.DomainRuleImplementationScope;
import org.praxisplatform.rules.contract.RuleImplementationRef;
import org.praxisplatform.rules.contract.RuleRuntimeCompatibility;

class RuleHostStatusReporterTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

    @Test
    void reportsRuntimeIdentityWithDeploymentOwnedPrincipal() {
        var service = mock(DomainRuleHostStatusService.class);
        var status = new ExtraordinaryGrantRuleSnapshotStatus(
                true, "snap-2", "A".repeat(64), "etag", 7L, NOW, NOW, null, null);
        var mapper = new ObjectMapper();
        DomainRuleImplementationCatalog catalog = scope -> List.of(
                new RuleImplementationRef("benefits:amount", "1.0.0"));
        var reporter = new RuleHostStatusReporter(
                () -> status, service, catalog, mapper, Clock.fixed(NOW, ZoneOffset.UTC),
                "tenant-a", "dev", "service:quickstart-a");
        var compatibility = RuleRuntimeCompatibility.current();
        var scope = new DomainRuleImplementationScope(
                "tenant-a", "dev", ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY);
        String catalogDigest = DomainRuleImplementationCatalogFingerprint.sha256(
                mapper, scope, catalog.allowedImplementations(scope));

        assertThat(reporter.reportNow()).isTrue();
        verify(service).ingest(
                new DomainRuleHostStatusRequest(
                        ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY,
                        "snap-2", "A".repeat(64), 7L, true,
                        ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION,
                        compatibility.engineContractVersion(), compatibility.jsonLogicDialectVersion(),
                        compatibility.jsonLogicCorpusSha256(), catalogDigest, null, NOW),
                new DomainRuleGovernancePrincipal("tenant-a", "service:quickstart-a", "dev"));
    }

    @Test
    void isolatesDeliveryFailureFromRuntime() {
        var service = mock(DomainRuleHostStatusService.class);
        var status = new ExtraordinaryGrantRuleSnapshotStatus(
                false, null, null, null, 0L, NOW, null, "HEAD_NOT_FOUND", "not found");
        when(service.ingest(any(), any())).thenThrow(new IllegalStateException("control plane down"));
        var reporter = new RuleHostStatusReporter(
                () -> status, service, scope -> List.of(), new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                "tenant-a", "dev", "service:quickstart-a");

        assertThat(reporter.reportNow()).isFalse();
    }
}
