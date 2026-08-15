package com.example.praxis.apiquickstart.rulelab;

import java.util.Objects;
import java.util.function.IntSupplier;
import org.praxisplatform.config.contract.DomainRuleOperationalTestEvidence;
import org.springframework.stereotype.Component;

/**
 * Converte uma operacao real do host em evidencia operacional sanitizada do Test Run V58.
 *
 * <p>O adapter nunca recebe payloads de negocio, SQL ou credenciais. O probe deve projetar
 * somente digests deterministas do recurso e do ledger de efeitos. A limpeza e obrigatoria e
 * executada tambem quando o comando falha, para que uma prova descartavel nao deixe dados no
 * datasource compartilhado.</p>
 */
@Component
final class PolicyStudioOperationalEvidenceAdapter {

    DomainRuleOperationalTestEvidence observe(
            OperationMode operationMode,
            boolean mutationExpected,
            IntSupplier baselineCallCount,
            OperationalState cleanupExpectedState,
            OperationalStateProbe probe,
            Runnable command,
            Runnable cleanup) {
        Objects.requireNonNull(operationMode, "operationMode is required");
        cleanupExpectedState = requireState(cleanupExpectedState, "expected cleanup");
        Objects.requireNonNull(probe, "probe is required");
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(cleanup, "cleanup is required");
        Objects.requireNonNull(baselineCallCount, "baselineCallCount is required");

        OperationalState before = requireState(probe.capture(), "before");
        OperationalState after = null;
        boolean cleanupVerified = false;
        try {
            command.run();
            after = requireState(probe.capture(), "after");
        } finally {
            cleanup.run();
            OperationalState cleaned = requireState(probe.capture(), "cleanup");
            cleanupVerified = cleanupExpectedState.equals(cleaned);
        }

        int measuredBaselineCalls = baselineCallCount.getAsInt();
        return evidence(operationMode, mutationExpected, measuredBaselineCalls, before, after, cleanupVerified);
    }

    DomainRuleOperationalTestEvidence evidence(
            OperationMode operationMode,
            boolean mutationExpected,
            int measuredBaselineCalls,
            OperationalState before,
            OperationalState after,
            boolean cleanupVerified) {
        Objects.requireNonNull(operationMode, "operationMode is required");
        before = requireState(before, "before");
        after = requireState(after, "after");
        boolean mutationObserved = !before.stateDigest().equals(after.stateDigest());
        if (!cleanupVerified) {
            throw new IllegalStateException("Operational cleanup did not restore the governed fixture state");
        }
        if (mutationObserved != mutationExpected) {
            throw new IllegalStateException(
                    "Operational mutation did not match the governed scenario expectation");
        }
        if (measuredBaselineCalls < 0) {
            throw new IllegalStateException("Measured baseline call count cannot be negative");
        }
        boolean noMutationVerified = !mutationObserved;
        return new DomainRuleOperationalTestEvidence(
                operationMode.name(),
                before.stateDigest(),
                after.stateDigest(),
                mutationObserved,
                noMutationVerified,
                cleanupVerified,
                after.effectLedgerDigest(),
                measuredBaselineCalls);
    }

    private OperationalState requireState(OperationalState state, String phase) {
        if (state == null) {
            throw new IllegalStateException("Operational probe returned no " + phase + " state");
        }
        return state;
    }

    enum OperationMode {
        CREATE,
        UPDATE
    }

    @FunctionalInterface
    interface OperationalStateProbe {
        OperationalState capture();
    }

    record OperationalState(String stateDigest, String effectLedgerDigest) {
        OperationalState {
            stateDigest = requireDigest(stateDigest, "stateDigest");
            effectLedgerDigest = requireDigest(effectLedgerDigest, "effectLedgerDigest");
        }

        private static String requireDigest(String value, String field) {
            if (value == null || !value.matches("[A-F0-9]{64}")) {
                throw new IllegalArgumentException(field + " must be an uppercase SHA-256 digest");
            }
            return value;
        }
    }
}
