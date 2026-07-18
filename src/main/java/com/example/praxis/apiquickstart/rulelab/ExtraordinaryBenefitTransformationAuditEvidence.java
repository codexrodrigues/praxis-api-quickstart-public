package com.example.praxis.apiquickstart.rulelab;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.contract.TypedTransformationProposal;

/**
 * Redacted, deterministic evidence for an authorized transformation proposal.
 *
 * <p>The host persists the exact canonical before/after digests published by the engine. Values
 * never leave this factory, and the separate identity digest contains only technical coordinates.</p>
 */
record ExtraordinaryBenefitTransformationAuditEvidence(
        String proposalKey,
        String bindingKey,
        String slotKey,
        String targetPath,
        String schemaRef,
        String operation,
        String reasonCode,
        String proposalIdentityDigest,
        String beforeDigest,
        String afterDigest) {

    static ExtraordinaryBenefitTransformationAuditEvidence from(
            TypedTransformationProposal proposal,
            RuleEvaluationResult result) {
        Objects.requireNonNull(proposal, "proposal is required");
        Objects.requireNonNull(result, "result is required");
        if (!result.transformationProposals().contains(proposal)) {
            throw new IllegalArgumentException("proposal must belong to the evaluated result");
        }
        String identity = framed(
                "praxis-transformation-audit-identity-v1",
                proposal.proposalKey(), proposal.bindingKey(), proposal.slotKey(),
                proposal.targetPath(), proposal.schemaRef(), proposal.operation().name(),
                proposal.reasonCode());
        return new ExtraordinaryBenefitTransformationAuditEvidence(
                proposal.proposalKey(),
                proposal.bindingKey(),
                proposal.slotKey(),
                proposal.targetPath(),
                proposal.schemaRef(),
                proposal.operation().name(),
                proposal.reasonCode(),
                sha256(identity),
                proposal.beforeDigest(),
                proposal.afterDigest());
    }

    private static String framed(String... values) {
        StringBuilder framed = new StringBuilder();
        for (String value : values) {
            String required = Objects.requireNonNull(value, "digest component is required");
            framed.append(required.length()).append(':').append(required);
        }
        return framed.toString();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception unavailable) {
            throw new IllegalStateException("SHA-256 must be available", unavailable);
        }
    }
}
