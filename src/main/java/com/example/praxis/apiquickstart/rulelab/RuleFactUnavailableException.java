package com.example.praxis.apiquickstart.rulelab;

/** Falha fechada e sanitizada da aquisicao de facts. */
final class RuleFactUnavailableException extends IllegalStateException {
    static final String NOT_FOUND = "RULE_FACT_SNAPSHOT_NOT_FOUND";
    static final String AMBIGUOUS = "RULE_FACT_SNAPSHOT_AMBIGUOUS";

    private final String code;

    RuleFactUnavailableException(String code) {
        super(code);
        this.code = code;
    }

    String code() {
        return code;
    }
}
