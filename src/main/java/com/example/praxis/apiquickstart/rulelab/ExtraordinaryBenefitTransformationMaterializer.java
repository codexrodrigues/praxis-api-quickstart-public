package com.example.praxis.apiquickstart.rulelab;

import java.math.BigDecimal;
import java.util.List;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.contract.TransformationOperation;
import org.praxisplatform.rules.contract.TransformationValueType;
import org.praxisplatform.rules.contract.TypedTransformationProposal;

/**
 * Host-owned allowlisted adapter from a pure engine proposal to the pilot write model.
 *
 * <p>The engine validates determinism and snapshot consistency. This adapter separately
 * authorizes the one field that the pilot permits, binds it to the governed schema reference,
 * and validates the domain numeric boundary before the existing transaction persists it.</p>
 */
final class ExtraordinaryBenefitTransformationMaterializer {
    static final String PROPOSAL_KEY = "grant.recommended-amount";
    static final String BINDING_KEY = "grant.amount-transformation";
    static final String TARGET_PATH = "request.recommendedAmount";
    static final String SCHEMA_REF =
            "urn:praxis:rule-lab:extraordinary-benefit-request:v1#/recommendedAmount";
    static final String REASON_CODE = "RECOMMENDED_AMOUNT_CALCULATED";

    /** Returns the authorized amount only for a successful consolidated decision. */
    BigDecimal materializeRecommendedAmount(RuleEvaluationResult result) {
        ExtraordinaryBenefitMaterializedTransformation materialized = materialize(result);
        return materialized == null ? null : materialized.amount();
    }

    /** Returns both the authorized value and exact proposal for host-only audit projection. */
    ExtraordinaryBenefitMaterializedTransformation materialize(RuleEvaluationResult result) {
        if (result.decision() != RuleDecision.ALLOW) {
            return null;
        }
        List<TypedTransformationProposal> candidates = result.transformationProposals().stream()
                .filter(proposal -> PROPOSAL_KEY.equals(proposal.proposalKey()))
                .toList();
        if (candidates.size() != 1 || result.transformationProposals().size() != 1) {
            throw new IllegalStateException("Expected exactly one allowlisted transformation proposal");
        }
        TypedTransformationProposal proposal = candidates.getFirst();
        return new ExtraordinaryBenefitMaterializedTransformation(validateProposal(proposal), proposal);
    }

    /** Validates the host authorization, schema binding, optimistic before state, and value domain. */
    BigDecimal validateProposal(TypedTransformationProposal proposal) {
        if (!PROPOSAL_KEY.equals(proposal.proposalKey())
                || !BINDING_KEY.equals(proposal.bindingKey())
                || !BINDING_KEY.equals(proposal.slotKey())
                || !TARGET_PATH.equals(proposal.targetPath())
                || !SCHEMA_REF.equals(proposal.schemaRef())
                || proposal.operation() != TransformationOperation.SET
                || proposal.before().present()
                || !proposal.after().present()
                || proposal.after().type() != TransformationValueType.NUMBER
                || !REASON_CODE.equals(proposal.reasonCode())) {
            throw new IllegalStateException("Transformation proposal is not authorized by the host contract");
        }
        BigDecimal amount = proposal.after().value().decimalValue();
        if (amount.signum() <= 0 || amount.scale() > 2 || amount.precision() - amount.scale() > 13) {
            throw new IllegalStateException("Recommended amount violates the governed monetary schema");
        }
        return amount;
    }
}
