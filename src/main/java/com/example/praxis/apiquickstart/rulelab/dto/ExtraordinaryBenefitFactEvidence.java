package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Sanitized provenance for one server-resolved fact snapshot. */
@Schema(description = "Proveniência sanitizada que comprova a resolução dos fatos em fontes controladas pelo host sem expor dados pessoais ou valores orçamentários.")
public record ExtraordinaryBenefitFactEvidence(
    @Schema(description = "Identidade SHA-256 determinística dos fatos congelados que influenciaram a decisão.") String factSnapshotId,
    @Schema(description = "Instante UTC único usado na resolução dos fatos e na avaliação das regras.") Instant resolvedAtUtc,
    @Schema(description = "Nomes lógicos allowlisted das fontes; tabelas físicas, SQL e registros nunca são expostos.") List<String> sources,
    @Schema(description = "Versão da política efetiva do programa de benefício usada na avaliação.") long programPolicyVersion) {
  public ExtraordinaryBenefitFactEvidence {
    sources = sources == null ? List.of() : List.copyOf(sources);
  }
}
