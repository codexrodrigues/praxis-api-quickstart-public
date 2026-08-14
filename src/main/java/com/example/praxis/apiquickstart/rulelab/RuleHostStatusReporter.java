package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Objects;
import java.util.function.Supplier;
import org.praxisplatform.config.dto.DomainRuleHostStatusRequest;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleHostStatusService;
import org.praxisplatform.config.service.DomainRuleImplementationCatalog;
import org.praxisplatform.config.service.DomainRuleImplementationCatalogFingerprint;
import org.praxisplatform.config.service.DomainRuleImplementationScope;
import org.praxisplatform.rules.contract.RuleRuntimeCompatibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Publishes redacted host readiness outside the rule-evaluation transaction. */
public final class RuleHostStatusReporter {
    private static final Logger LOG = LoggerFactory.getLogger(RuleHostStatusReporter.class);
    private final Supplier<ExtraordinaryGrantRuleSnapshotStatus> statusSupplier;
    private final DomainRuleHostStatusService service;
    private final Clock clock;
    private final DomainRuleGovernancePrincipal principal;
    private final RuleRuntimeCompatibility compatibility;
    private final String implementationCatalogDigest;

    RuleHostStatusReporter(
            Supplier<ExtraordinaryGrantRuleSnapshotStatus> statusSupplier,
            DomainRuleHostStatusService service,
            DomainRuleImplementationCatalog implementationCatalog,
            ObjectMapper objectMapper,
            Clock clock,
            String tenantId,
            String environment,
            String hostActorRef) {
        this.statusSupplier = Objects.requireNonNull(statusSupplier, "statusSupplier is required");
        this.service = Objects.requireNonNull(service, "service is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.principal = new DomainRuleGovernancePrincipal(
                requireText(tenantId, "tenantId"),
                requireText(hostActorRef, "hostActorRef"),
                requireText(environment, "environment"));
        this.compatibility = RuleRuntimeCompatibility.current();
        var scope = new DomainRuleImplementationScope(
                this.principal.tenantId(), this.principal.environment(),
                ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY);
        this.implementationCatalogDigest = DomainRuleImplementationCatalogFingerprint.sha256(
                objectMapper, scope, implementationCatalog.allowedImplementations(scope));
    }

    /** Explicit seam for startup, operations and deterministic tests. */
    public boolean reportNow() {
        ExtraordinaryGrantRuleSnapshotStatus status = statusSupplier.get();
        try {
            service.ingest(new DomainRuleHostStatusRequest(
                    ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY,
                    optionalText(status.activeSnapshotKey()),
                    optionalText(status.activeContentHash()),
                    status.activationRevision() > 0 ? status.activationRevision() : null,
                    status.ready(),
                    ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION,
                    compatibility.engineContractVersion(),
                    compatibility.jsonLogicDialectVersion(),
                    compatibility.jsonLogicCorpusSha256(),
                    implementationCatalogDigest,
                    optionalText(status.lastFailureCode()),
                    clock.instant()), principal);
            return true;
        } catch (RuntimeException failure) {
            LOG.warn("Rule Lab host-status delivery failed without affecting evaluation: {}",
                    failure.getClass().getSimpleName());
            return false;
        }
    }

    @Scheduled(
            initialDelayString = "${praxis.rule-lab.host-status.initial-delay-ms:10000}",
            fixedDelayString = "${praxis.rule-lab.host-status.report-delay-ms:30000}")
    public void scheduledReport() {
        reportNow();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireText(String value, String field) {
        String normalized = optionalText(value);
        if (normalized == null) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }
}
