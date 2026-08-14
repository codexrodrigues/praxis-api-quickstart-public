package com.example.praxis.apiquickstart.rulelab;

/** Raised when an inspected composition digest no longer matches host-recomputed content. */
final class StalePolicyStudioRuleSetCandidateException extends RuntimeException {
    StalePolicyStudioRuleSetCandidateException(String message) {
        super(message);
    }
}
