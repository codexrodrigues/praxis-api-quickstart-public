package com.example.praxis.apiquickstart.rulelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.praxisplatform.rules.contract.TransformationOperation;
import org.praxisplatform.rules.contract.TransformationValue;
import org.praxisplatform.rules.contract.TransformationValueType;
import org.praxisplatform.rules.contract.TypedTransformationProposal;

class ExtraordinaryBenefitTransformationMaterializerTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ExtraordinaryBenefitTransformationMaterializer materializer =
            new ExtraordinaryBenefitTransformationMaterializer();

    @Test
    void materializesOnlyTheAllowlistedSchemaBoundAmount() {
        BigDecimal amount = materializer.validateProposal(proposal(
                ExtraordinaryBenefitTransformationMaterializer.TARGET_PATH,
                ExtraordinaryBenefitTransformationMaterializer.SCHEMA_REF,
                "2500.00"));

        assertEquals(new BigDecimal("2500.00"), amount);
    }

    @Test
    void rejectsAFieldThatTheHostDidNotAuthorize() {
        var proposal = proposal(
                "request.availableBudgetAmount",
                ExtraordinaryBenefitTransformationMaterializer.SCHEMA_REF,
                "2500.00");

        assertThrows(IllegalStateException.class, () -> materializer.validateProposal(proposal));
    }

    @Test
    void rejectsAProposalOutsideTheGovernedMonetarySchema() {
        var wrongSchema = proposal(
                ExtraordinaryBenefitTransformationMaterializer.TARGET_PATH,
                "urn:praxis:rule-lab:other:v1#/recommendedAmount",
                "2500.00");
        var excessiveScale = proposal(
                ExtraordinaryBenefitTransformationMaterializer.TARGET_PATH,
                ExtraordinaryBenefitTransformationMaterializer.SCHEMA_REF,
                "2500.001");

        assertThrows(IllegalStateException.class, () -> materializer.validateProposal(wrongSchema));
        assertThrows(IllegalStateException.class, () -> materializer.validateProposal(excessiveScale));
    }

    private TypedTransformationProposal proposal(String targetPath, String schemaRef, String amount) {
        TransformationValue before = TransformationValue.absent();
        TransformationValue after = TransformationValue.of(
                TransformationValueType.NUMBER,
                JSON.getNodeFactory().numberNode(new BigDecimal(amount)));
        return new TypedTransformationProposal(
                ExtraordinaryBenefitTransformationMaterializer.PROPOSAL_KEY,
                ExtraordinaryBenefitTransformationMaterializer.BINDING_KEY,
                ExtraordinaryBenefitTransformationMaterializer.BINDING_KEY,
                targetPath,
                schemaRef,
                TransformationOperation.SET,
                before,
                after,
                before.digest(),
                after.digest(),
                ExtraordinaryBenefitTransformationMaterializer.REASON_CODE);
    }
}
