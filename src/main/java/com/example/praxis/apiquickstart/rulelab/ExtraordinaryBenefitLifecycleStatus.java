package com.example.praxis.apiquickstart.rulelab;

import io.swagger.v3.oas.annotations.media.Schema;

/** Estados confirmados da solicitacao persistida no laboratório QL-05. */
@Schema(description = "Estado confirmado do pedido: avaliado, submetido, aprovado ou aplicado.")
public enum ExtraordinaryBenefitLifecycleStatus {
    EVALUATED,
    SUBMITTED,
    APPROVED,
    APPLIED
}
