package com.example.praxis.apiquickstart.rulelab;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * Bounded registry that makes active operation scopes observable without ThreadLocal state.
 *
 * <p>The immutable context is still passed explicitly. Registration exists only to reject leaks,
 * duplicate operation identity and unbounded concurrent scopes in the reference host.</p>
 */
@Component
final class RuleLabOperationScopeRegistry {
    private static final int MAX_ACTIVE_OPERATIONS = 64;

    private final ConcurrentHashMap<UUID, Scope> active = new ConcurrentHashMap<>();
    private final Semaphore capacity = new Semaphore(MAX_ACTIVE_OPERATIONS);

    Scope open(RuleLabOperationContext context) {
        Objects.requireNonNull(context, "context is required");
        if (!capacity.tryAcquire()) {
            throw new IllegalStateException("Rule Lab operation capacity exceeded");
        }
        Scope scope = new Scope(this, context);
        if (active.putIfAbsent(context.operationId(), scope) != null) {
            capacity.release();
            throw new IllegalStateException("Rule Lab operation is already active: " + context.operationId());
        }
        return scope;
    }

    int activeCount() {
        return active.size();
    }

    private void close(Scope scope) {
        if (active.remove(scope.context().operationId(), scope)) {
            capacity.release();
        }
    }

    static final class Scope implements AutoCloseable {
        private final RuleLabOperationScopeRegistry owner;
        private final RuleLabOperationContext context;
        private final AtomicBoolean closed = new AtomicBoolean();
        private RuleLabOperationStage stage = RuleLabOperationStage.OPENED;

        private Scope(RuleLabOperationScopeRegistry owner, RuleLabOperationContext context) {
            this.owner = owner;
            this.context = context;
        }

        RuleLabOperationContext context() {
            return context;
        }

        synchronized void advance(RuleLabOperationStage expected, RuleLabOperationStage next) {
            if (closed.get()) {
                throw new IllegalStateException("Rule Lab operation scope is closed");
            }
            if (stage != expected || next.ordinal() != expected.ordinal() + 1) {
                throw new IllegalStateException("Invalid Rule Lab operation stage transition: " + stage + " -> " + next);
            }
            stage = next;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                owner.close(this);
            }
        }
    }
}
