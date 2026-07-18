package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.config.dto.DomainRuleSnapshotActivationResponse;
import org.praxisplatform.config.service.DomainRuleImplementationScope;
import org.praxisplatform.config.service.DomainRuleSnapshotReader;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleExtensionTrust;
import org.praxisplatform.rules.contract.RuleImplementationRef;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.runtime.PraxisRuleSetEngine;
import org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler;
import org.springframework.boot.actuate.health.Status;

class ExtraordinaryGrantRuleSnapshotRuntimeTest {
    private static final Instant NOW = Instant.parse("2026-07-13T15:00:00Z");

    private RuleBindingExecutorRegistry registry;
    private ExtraordinaryGrantRuleSnapshotRuntime runtime;

    @BeforeEach
    void setUp() {
        registry = new ExtraordinaryGrantRuleLabConfiguration().extraordinaryGrantRuleExecutorRegistry();
        runtime = new ExtraordinaryGrantRuleSnapshotRuntime(registry);
    }

    @Test
    void hostCatalogAdmitsAttestedCustomerExtensionOnlyForTheConfiguredScope() {
        var catalog = new ExtraordinaryGrantRuleLabConfiguration()
                .extraordinaryGrantRuleImplementationCatalog("desenv", "local");

        var admitted = catalog.allowedImplementations(new DomainRuleImplementationScope(
                "desenv", "local", ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY));

        assertThat(admitted).hasSize(3);
        assertThat(admitted).filteredOn(ref -> ref.implementationKey().startsWith("customer:"))
                .singleElement()
                .satisfies(ref -> {
                    assertThat(ref.extensionTrust()).isNotNull();
                    assertThat(ref.extensionTrust().signatureIdentity())
                            .isEqualTo("lab-fixture:customer-policy-signer");
                });
        assertThat(catalog.allowedImplementations(new DomainRuleImplementationScope(
                "another-tenant", "local", ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY)))
                .isEmpty();
    }

    @Test
    void activatesCompiledSnapshotAndTreatsSameHeadAsCacheHit() {
        var activation = activation(snapshot("snapshot-v1", "quickstart/1.0", 1), "head-1", 1);

        var first = runtime.activate(activation, "desenv", "local", NOW);
        var second = runtime.activate(activation, "desenv", "local", NOW.plusSeconds(30));

        assertThat(first.ready()).isTrue();
        assertThat(first.activeSnapshotKey()).isEqualTo("snapshot-v1");
        assertThat(second.activeHeadEtag()).isEqualTo("head-1");
        assertThat(second.lastAttemptAtUtc()).isEqualTo(NOW.plusSeconds(30));
        assertThat(second.lastActivatedAtUtc()).isEqualTo(NOW);
        assertThat(second.lastFailureCode()).isNull();
    }

    @Test
    void incompatibleCandidatePreservesLastKnownGoodSnapshot() {
        runtime.activate(
                activation(snapshot("snapshot-v1", "quickstart/1.0", 1), "head-1", 1),
                "desenv", "local", NOW);
        PublishedRuleSnapshot incompatible = snapshot("snapshot-v2", "quickstart/2.0", 2);

        var status = runtime.activate(
                new DomainRuleSnapshotActivationResponse(
                        incompatible, "C".repeat(64), "head-2", 2, "PUBLISHED"),
                "desenv", "local", NOW.plusSeconds(30));

        assertThat(status.ready()).isTrue();
        assertThat(status.activeSnapshotKey()).isEqualTo("snapshot-v1");
        assertThat(status.activeHeadEtag()).isEqualTo("head-1");
        assertThat(status.lastFailureCode()).isEqualTo("SNAPSHOT_HOST_INCOMPATIBLE");
    }

    @Test
    void customerAttestationSubstitutionFailsBeforeExtensionExecution() throws Exception {
        PublishedRuleSnapshot candidate = snapshot("snapshot-v2", "quickstart/1.0", 2);
        var substitutedTrust = new RuleExtensionTrust(
                "A".repeat(64),
                "untrusted-fixture:substituted-signer",
                "lab-policy:customer-extension-v1",
                "B".repeat(64));
        var substitutedCatalog = RuleBindingExecutorRegistry.planning(List.of(
                new RuleImplementationRef(
                        "customer:extraordinary-grant-additional-eligibility", "1.0.0", substitutedTrust),
                new RuleImplementationRef("benefits:extraordinary-grant-amount-transformation", "1.0.0"),
                new RuleImplementationRef("benefits:extraordinary-grant-effect-plan", "1.0.0")));
        var substitutedPlan = new PraxisRuleSnapshotCompiler(substitutedCatalog)
                .compile(candidate, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION)
                .plan();

        var result = new PraxisRuleSetEngine(registry).evaluate(
                substitutedPlan,
                new ObjectMapper().readTree("""
                        {
                          "actor": {"permissions": ["benefit:request"]},
                          "worker": {"status": "ACTIVE"},
                          "grant": {"hasDuplicate": false},
                          "program": {"active": true},
                          "customer": {"additionalEligible": true}
                        }
                        """),
                NOW.toString(),
                "America/Sao_Paulo");

        assertThat(result.decision()).isEqualTo(RuleDecision.TECHNICAL_ERROR);
        assertThat(result.reasonCodes()).containsExactly("IMPLEMENTATION_TRUST_MISMATCH");
        assertThat(result.bindingResults()).hasSize(6);
        assertThat(result.bindingResults().getLast().bindingKey())
                .isEqualTo("customer.additional-eligibility");
        assertThat(result.bindingResults().getLast().reasonCodes())
                .containsExactly("IMPLEMENTATION_TRUST_MISMATCH");
    }

    @Test
    void capturedOperationSessionRemainsPinnedWhenActiveHeadChanges() throws Exception {
        runtime.activate(
                activation(snapshot("snapshot-v1", "quickstart/1.0", 1), "head-1", 1),
                "desenv", "local", NOW);
        ExtraordinaryGrantRuleSnapshotSession session = runtime.captureSnapshot(NOW);

        runtime.activate(
                activation(snapshot("snapshot-v2", "quickstart/1.0", 2), "head-2", 2),
                "desenv", "local", NOW.plusSeconds(30));
        var evaluation = runtime.evaluateWithSnapshot(
                session,
                new ObjectMapper().readTree("""
                        {
                          "request": {"requestedAmount": 2500.00},
                          "actor": {"permissions": ["benefit:request"]},
                          "worker": {"status": "ACTIVE"},
                          "grant": {"hasDuplicate": false},
                          "program": {"active": true, "maxAmount": 5000.00},
                          "customer": {"additionalEligible": true},
                          "payment": {
                            "requestedDate": "2026-07-20",
                            "allowedDates": ["2026-07-20", "2026-08-05"]
                          },
                          "budget": {"availableAmount": 100000.00}
                        }
                        """),
                NOW.plusSeconds(30),
                ZoneId.of("America/Sao_Paulo"));

        assertThat(runtime.status().activeSnapshotKey()).isEqualTo("snapshot-v2");
        assertThat(evaluation.snapshotKey()).isEqualTo("snapshot-v1");
        assertThat(evaluation.activationRevision()).isEqualTo(1);
        assertThat(evaluation.result().ruleSetRef().version()).isEqualTo(1);
    }

    @Test
    void rejectsCrossBoundaryCandidatesAndPreservesLastKnownGoodSnapshot() {
        runtime.activate(
                activation(snapshot("snapshot-v1", "quickstart/1.0", 1), "head-1", 1),
                "desenv", "local", NOW);
        PublishedRuleSnapshot base = snapshot("snapshot-v2", "quickstart/1.0", 2);
        PublishedRuleSnapshot crossTenant = new PublishedRuleSnapshot(
                base.snapshotContractVersion(), base.snapshotKey(), "tenant-b", base.environment(),
                base.ownerServiceKey(), base.publicationRevision(), base.publishedAtUtc(),
                base.supersedesSnapshotKey(), base.requiredHostContractVersion(), base.validFromUtc(),
                base.validUntilUtc(), base.sources(), base.approvals(), base.ruleSet());
        PublishedRuleSnapshot crossEnvironment = new PublishedRuleSnapshot(
                base.snapshotContractVersion(), base.snapshotKey(), base.tenantId(), "production",
                base.ownerServiceKey(), base.publicationRevision(), base.publishedAtUtc(),
                base.supersedesSnapshotKey(), base.requiredHostContractVersion(), base.validFromUtc(),
                base.validUntilUtc(), base.sources(), base.approvals(), base.ruleSet());

        for (PublishedRuleSnapshot crossBoundary : List.of(crossTenant, crossEnvironment)) {
            var status = runtime.activate(
                    activation(crossBoundary, "head-2", 2),
                    "desenv", "local", NOW.plusSeconds(30));

            assertThat(status.ready()).isTrue();
            assertThat(status.activeSnapshotKey()).isEqualTo("snapshot-v1");
            assertThat(status.activeHeadEtag()).isEqualTo("head-1");
            assertThat(status.lastFailureCode()).isEqualTo("SNAPSHOT_REJECTED");
        }
    }

    @Test
    void rollbackToSameImmutableContentStillAcceptsNewerOpaqueHead() {
        PublishedRuleSnapshot v1 = snapshot("snapshot-v1", "quickstart/1.0", 1);
        var original = activation(v1, "head-1", 1);
        runtime.activate(original, "desenv", "local", NOW);
        runtime.activate(
                activation(snapshot("snapshot-v2", "quickstart/1.0", 2), "head-2", 2),
                "desenv", "local", NOW.plusSeconds(30));

        var rolledBack = runtime.activate(
                new DomainRuleSnapshotActivationResponse(
                        v1, original.snapshotContentHash(), "head-3", 3, "ROLLED_BACK"),
                "desenv", "local", NOW.plusSeconds(60));

        assertThat(rolledBack.activeSnapshotKey()).isEqualTo("snapshot-v1");
        assertThat(rolledBack.activeHeadEtag()).isEqualTo("head-3");
        assertThat(rolledBack.activationRevision()).isEqualTo(3);
        assertThat(rolledBack.activeContentHash()).isEqualTo(original.snapshotContentHash());
    }

    @Test
    void loaderFailureDoesNotEvictActiveSnapshot() {
        var activation = activation(snapshot("snapshot-v1", "quickstart/1.0", 1), "head-1", 1);
        DomainRuleSnapshotReader reader = mock(DomainRuleSnapshotReader.class);
        when(reader.findActive("desenv", "local", ExtraordinaryGrantRuleSnapshotRuntime.RULE_SET_KEY))
                .thenReturn(Optional.of(activation))
                .thenThrow(new IllegalStateException("database unavailable"));
        var loader = new ExtraordinaryGrantRuleSnapshotLoader(
                reader, runtime, "desenv", "local", Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(loader.refreshNow().ready()).isTrue();
        var failedRefresh = loader.refreshNow();

        assertThat(failedRefresh.ready()).isTrue();
        assertThat(failedRefresh.activeSnapshotKey()).isEqualTo("snapshot-v1");
        assertThat(failedRefresh.lastFailureCode()).isEqualTo("CONTROL_PLANE_UNAVAILABLE");
    }

    @Test
    void unchangedHeadBecomesNotReadyAfterItsExclusiveValidityLimit() {
        PublishedRuleSnapshot base = snapshot("snapshot-v1", "quickstart/1.0", 1);
        PublishedRuleSnapshot expiring = new PublishedRuleSnapshot(
                base.snapshotContractVersion(), base.snapshotKey(), base.tenantId(), base.environment(),
                base.ownerServiceKey(), base.publicationRevision(), base.publishedAtUtc(),
                base.supersedesSnapshotKey(), base.requiredHostContractVersion(), base.validFromUtc(),
                NOW.plusSeconds(10).toString(), base.sources(), base.approvals(), base.ruleSet());
        var activation = activation(expiring, "head-1", 1);
        runtime.activate(activation, "desenv", "local", NOW);

        var expired = runtime.activate(activation, "desenv", "local", NOW.plusSeconds(10));

        assertThat(expired.ready()).isFalse();
        assertThat(expired.activeSnapshotKey()).isEqualTo("snapshot-v1");
        assertThat(expired.lastFailureCode()).isEqualTo("ACTIVE_SNAPSHOT_NOT_EFFECTIVE");
    }

    @Test
    void healthProjectionExposesReadinessWithoutRulePayloadsOrFailureMessages() {
        var healthIndicator = new ExtraordinaryGrantRuleSnapshotHealthIndicator(runtime);
        assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);

        runtime.activate(
                activation(snapshot("snapshot-v1", "quickstart/1.0", 1), "head-1", 1),
                "desenv", "local", NOW);
        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("activeSnapshotKey", "snapshot-v1");
        assertThat(health.getDetails()).doesNotContainKeys("lastFailureMessage", "snapshot", "facts", "ruleSet");
    }

    private DomainRuleSnapshotActivationResponse activation(
            PublishedRuleSnapshot snapshot, String headEtag, long activationRevision) {
        String contentHash = new PraxisRuleSnapshotCompiler(registry)
                .compile(snapshot, ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION)
                .snapshotContentHash();
        return new DomainRuleSnapshotActivationResponse(
                snapshot, contentHash, headEtag, activationRevision, "ACTIVE");
    }

    private PublishedRuleSnapshot snapshot(String snapshotKey, String hostContractVersion, int version) {
        var definition = ExtraordinaryGrantRuleSetFactory.definition();
        var versionedDefinition = new org.praxisplatform.rules.contract.RuleSetDefinition(
                new org.praxisplatform.rules.contract.RuleSetRef(
                        definition.ref().domainKey(),
                        definition.ref().boundedContextKey(),
                        definition.ref().ruleSetKey(),
                        definition.ref().operationKey(),
                        version),
                definition.availableRoots(),
                definition.slots(),
                definition.bindings(),
                definition.compatibility(),
                definition.failPolicy());
        return new PublishedRuleSnapshot(
                PublishedRuleSnapshot.SNAPSHOT_CONTRACT_VERSION,
                snapshotKey,
                "desenv",
                "local",
                ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY,
                version,
                NOW.minusSeconds(3600).toString(),
                version == 1 ? null : "snapshot-v" + (version - 1),
                hostContractVersion,
                NOW.minusSeconds(3600).toString(),
                null,
                List.of(
                        new RuleSnapshotSource("definition-1-v" + version, "grant:eligibility", version, hash('A', version)),
                        new RuleSnapshotSource("definition-2-v" + version, "grant:amount", version, hash('B', version))),
                List.of(
                        new RuleSnapshotApproval(
                                "approval-1-v" + version, "RULE_DEFINITION_APPROVER", "approver-a",
                                NOW.minusSeconds(7200).toString(), hash('A', version)),
                        new RuleSnapshotApproval(
                                "approval-2-v" + version, "RULE_DEFINITION_APPROVER", "approver-b",
                                NOW.minusSeconds(7100).toString(), hash('B', version))),
                versionedDefinition);
    }

    private String hash(char prefix, int version) {
        char value = (char) (prefix + version - 1);
        return String.valueOf(value).repeat(64);
    }
}
