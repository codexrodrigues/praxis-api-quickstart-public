package com.example.praxis.apiquickstart.rulelab;

import java.util.Objects;
import org.praxisplatform.rules.snapshot.CompiledRuleSnapshot;

/**
 * Immutable host-side handle to the exact compiled snapshot selected for one operation.
 *
 * <p>The handle is intentionally package-private: it pins evaluation consistency inside the host
 * without widening the public engine or HTTP contracts.</p>
 */
record ExtraordinaryGrantRuleSnapshotSession(
        CompiledRuleSnapshot compiled,
        long activationRevision) {

    ExtraordinaryGrantRuleSnapshotSession {
        Objects.requireNonNull(compiled, "compiled is required");
        if (activationRevision < 1) {
            throw new IllegalArgumentException("activationRevision must be positive");
        }
    }

    String snapshotKey() {
        return compiled.snapshot().snapshotKey();
    }

    String snapshotContentHash() {
        return compiled.snapshotContentHash();
    }
}
