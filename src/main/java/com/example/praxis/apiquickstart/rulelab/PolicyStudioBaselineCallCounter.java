package com.example.praxis.apiquickstart.rulelab;

import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** Measures baseline-authority calls on the synchronous host execution thread. */
@Component
final class PolicyStudioBaselineCallCounter {
    private final ThreadLocal<Integer> calls = ThreadLocal.withInitial(() -> 0);

    int current() {
        return calls.get();
    }

    int deltaSince(int before) {
        int current = calls.get();
        if (before < 0 || current < before) {
            throw new IllegalStateException("Baseline call counter moved backwards");
        }
        return current - before;
    }

    <T> T observeBaselineCall(Supplier<T> call) {
        Objects.requireNonNull(call, "baseline call is required");
        calls.set(Math.addExact(calls.get(), 1));
        return call.get();
    }

    void clear() {
        calls.remove();
    }
}
