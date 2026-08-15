package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.praxisplatform.config.contract.DomainRuleOperationalTestEvidence;
import org.praxisplatform.config.contract.DomainRuleTestRunRecordRequest;
import org.praxisplatform.config.contract.DomainRuleTestRunResultRequest;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleTestRunService;

class PolicyStudioOperationalTestRunRecorderTest {
    private static final String DIGEST = "A".repeat(64);
    private final DomainRuleTestRunService owner = mock(DomainRuleTestRunService.class);
    private final PolicyStudioOperationalTestRunRecorder recorder =
            new PolicyStudioOperationalTestRunRecorder(owner);

    @Test
    void enrichesEveryScenarioAndDelegatesPersistenceToConfig() {
        UUID workspaceId = UUID.randomUUID();
        UUID scenarioId = UUID.randomUUID();
        DomainRuleGovernancePrincipal principal = principal();
        DomainRuleTestRunResultRequest evaluated = evaluated(scenarioId);
        DomainRuleTestRunRecordRequest run = run(evaluated);
        DomainRuleOperationalTestEvidence evidence =
                new DomainRuleOperationalTestEvidence("UPDATE", DIGEST, DIGEST, false, true, true, DIGEST, 1);

        recorder.record(workspaceId, run, Map.of(scenarioId, evidence), principal);

        ArgumentCaptor<DomainRuleTestRunRecordRequest> captured =
                ArgumentCaptor.forClass(DomainRuleTestRunRecordRequest.class);
        verify(owner).record(eq(workspaceId), captured.capture(), eq(principal));
        assertThat(captured.getValue().results()).singleElement()
                .extracting(DomainRuleTestRunResultRequest::operationalEvidence)
                .isEqualTo(evidence);
        assertThat(captured.getValue().baselineEvidence()).isSameAs(run.baselineEvidence());
        assertThat(captured.getValue().idempotencyKey()).isEqualTo(run.idempotencyKey());
    }

    @Test
    void failsClosedForPartialOrForeignEvidence() {
        UUID scenarioId = UUID.randomUUID();
        DomainRuleTestRunRecordRequest run = run(evaluated(scenarioId));
        DomainRuleOperationalTestEvidence evidence =
                new DomainRuleOperationalTestEvidence("CREATE", null, DIGEST, true, false, true, DIGEST, 0);

        assertThatThrownBy(() -> recorder.record(
                        UUID.randomUUID(), run, Map.of(), principal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly every");
        assertThatThrownBy(() -> recorder.record(
                        UUID.randomUUID(), run, Map.of(UUID.randomUUID(), evidence),
                        principal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly every");
    }

    private DomainRuleTestRunRecordRequest run(DomainRuleTestRunResultRequest result) {
        return new DomainRuleTestRunRecordRequest(
                "recorder:test", 3L, DIGEST, Instant.parse("2026-08-14T12:00:00Z"), "UTC",
                "snapshot-1", DIGEST, 4L, null, List.of(result));
    }

    private DomainRuleTestRunResultRequest evaluated(UUID scenarioId) {
        return new DomainRuleTestRunResultRequest(
                scenarioId, "scenario-1", "ALLOW", "ALLOW", null, null,
                List.of(), List.of(), List.of(), List.of(), DIGEST, DIGEST, DIGEST, null, null);
    }

    private DomainRuleGovernancePrincipal principal() {
        return new DomainRuleGovernancePrincipal("desenv", "policy-proof-agent", "local");
    }
}
