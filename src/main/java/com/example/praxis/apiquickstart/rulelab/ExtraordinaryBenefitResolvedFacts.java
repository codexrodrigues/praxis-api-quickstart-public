package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitFactEvidence;

/** Internal immutable handoff from the DB fact provider to deterministic evaluation. */
record ExtraordinaryBenefitResolvedFacts(
    ExtraordinaryBenefitEvaluationRequest evaluationRequest,
    ExtraordinaryBenefitFactEvidence evidence) {}
