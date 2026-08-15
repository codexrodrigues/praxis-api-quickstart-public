package com.example.praxis.apiquickstart.rulelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class PolicyStudioOperationalEvidenceAdapterTest {
    private static final String EMPTY = "0".repeat(64);
    private static final String CREATED = "1".repeat(64);
    private static final String LEDGER = "2".repeat(64);

    private final PolicyStudioOperationalEvidenceAdapter adapter =
            new PolicyStudioOperationalEvidenceAdapter();

    @Test
    void recordsCreateMutationAndVerifiesCleanupWithoutExposingPayloads() {
        var states = new ArrayDeque<PolicyStudioOperationalEvidenceAdapter.OperationalState>();
        states.add(state(EMPTY, EMPTY));
        states.add(state(CREATED, LEDGER));
        states.add(state(EMPTY, EMPTY));

        var evidence = adapter.observe(
                PolicyStudioOperationalEvidenceAdapter.OperationMode.CREATE,
                true,
                () -> 1,
                state(EMPTY, EMPTY),
                states::remove,
                () -> {},
                () -> {});

        assertThat(evidence.operationMode()).isEqualTo("CREATE");
        assertThat(evidence.beforeStateDigest()).isEqualTo(EMPTY);
        assertThat(evidence.afterStateDigest()).isEqualTo(CREATED);
        assertThat(evidence.effectLedgerDigest()).isEqualTo(LEDGER);
        assertThat(evidence.mutationObserved()).isTrue();
        assertThat(evidence.noMutationVerified()).isFalse();
        assertThat(evidence.cleanupVerified()).isTrue();
        assertThat(evidence.baselineCallCount()).isOne();
    }

    @Test
    void recordsDeniedUpdateAsVerifiedNoMutation() {
        var states = new ArrayDeque<PolicyStudioOperationalEvidenceAdapter.OperationalState>();
        states.add(state(CREATED, EMPTY));
        states.add(state(CREATED, EMPTY));
        states.add(state(EMPTY, EMPTY));

        var evidence = adapter.observe(
                PolicyStudioOperationalEvidenceAdapter.OperationMode.UPDATE,
                false,
                () -> 0,
                state(EMPTY, EMPTY),
                states::remove,
                () -> {},
                () -> {});

        assertThat(evidence.mutationObserved()).isFalse();
        assertThat(evidence.noMutationVerified()).isTrue();
        assertThat(evidence.cleanupVerified()).isTrue();
    }

    @Test
    void alwaysCleansUpWhenTheOperationalCommandFails() {
        AtomicBoolean cleaned = new AtomicBoolean();

        assertThatThrownBy(() -> adapter.observe(
                        PolicyStudioOperationalEvidenceAdapter.OperationMode.UPDATE,
                        true,
                        () -> 0,
                        state(EMPTY, EMPTY),
                        () -> state(EMPTY, EMPTY),
                        () -> { throw new IllegalStateException("command failed"); },
                        () -> cleaned.set(true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("command failed");

        assertThat(cleaned).isTrue();
    }

    @Test
    void failsClosedWhenObservedMutationDiffersFromTheGovernedExpectation() {
        var states = new ArrayDeque<PolicyStudioOperationalEvidenceAdapter.OperationalState>();
        states.add(state(EMPTY, EMPTY));
        states.add(state(CREATED, LEDGER));
        states.add(state(EMPTY, EMPTY));

        assertThatThrownBy(() -> adapter.observe(
                        PolicyStudioOperationalEvidenceAdapter.OperationMode.CREATE,
                        false,
                        () -> 0,
                        state(EMPTY, EMPTY),
                        states::remove,
                        () -> {},
                        () -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not match");
    }

    @Test
    void rejectsUnsanitizedEvidenceAndInvalidBaselineCounts() {
        assertThatThrownBy(() -> state("raw-payload", EMPTY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stateDigest");
        assertThatThrownBy(() -> adapter.observe(
                        PolicyStudioOperationalEvidenceAdapter.OperationMode.CREATE,
                        false,
                        () -> -1,
                        state(EMPTY, EMPTY),
                        () -> state(EMPTY, EMPTY),
                        () -> {},
                        () -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("baseline call count");
    }

    @Test
    void failsClosedWhenCleanupDoesNotRestoreTheGovernedState() {
        var states = new ArrayDeque<PolicyStudioOperationalEvidenceAdapter.OperationalState>();
        states.add(state(EMPTY, EMPTY));
        states.add(state(CREATED, LEDGER));
        states.add(state(CREATED, LEDGER));

        assertThatThrownBy(() -> adapter.observe(
                        PolicyStudioOperationalEvidenceAdapter.OperationMode.CREATE,
                        true,
                        () -> 0,
                        state(EMPTY, EMPTY),
                        states::remove,
                        () -> {},
                        () -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cleanup did not restore");
    }

    private PolicyStudioOperationalEvidenceAdapter.OperationalState state(String state, String ledger) {
        return new PolicyStudioOperationalEvidenceAdapter.OperationalState(state, ledger);
    }
}
