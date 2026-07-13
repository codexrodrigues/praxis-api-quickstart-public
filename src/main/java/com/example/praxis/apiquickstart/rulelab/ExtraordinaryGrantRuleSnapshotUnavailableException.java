package com.example.praxis.apiquickstart.rulelab;

/** Raised when no valid last-known-good RuleSet snapshot is available for evaluation. */
public final class ExtraordinaryGrantRuleSnapshotUnavailableException extends IllegalStateException {
    public ExtraordinaryGrantRuleSnapshotUnavailableException(String message) {
        super(message);
    }
}
