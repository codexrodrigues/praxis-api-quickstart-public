package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Read-only deterministic decision paired with sanitized DB fact provenance. */
@Schema(description = "Resultado do preflight com fatos autoritativos. Não persiste solicitação de benefício nem autoriza efeito externo.")
public record ExtraordinaryBenefitAuthoritativeEvaluationResponse(
    @Schema(description = "Decisão determinística do RuleSet avaliada sobre os fatos congelados pelo servidor.") ExtraordinaryBenefitEvaluationResponse evaluation,
    @Schema(description = "Evidência sanitizada que identifica o snapshot DB-backed dos fatos.") ExtraordinaryBenefitFactEvidence factEvidence) {}
