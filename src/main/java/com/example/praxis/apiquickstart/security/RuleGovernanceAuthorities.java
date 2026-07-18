package com.example.praxis.apiquickstart.security;

/** IAM authorities consumed by the Config Starter rule snapshot control plane. */
public final class RuleGovernanceAuthorities {
    public static final String DEFINITION_AUTHOR = "ROLE_RULE_DEFINITION_AUTHOR";
    public static final String DEFINITION_APPROVER = "ROLE_RULE_DEFINITION_APPROVER";
    public static final String COMPOSITION_APPROVER = "ROLE_RULE_COMPOSITION_APPROVER";
    public static final String SNAPSHOT_PUBLISHER = "ROLE_RULE_SNAPSHOT_PUBLISHER";
    public static final String SNAPSHOT_OPERATOR = "ROLE_RULE_SNAPSHOT_OPERATOR";

    private RuleGovernanceAuthorities() {
    }
}
