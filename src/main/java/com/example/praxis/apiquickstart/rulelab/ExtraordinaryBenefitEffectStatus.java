package com.example.praxis.apiquickstart.rulelab;

import io.swagger.v3.oas.annotations.media.Schema;

/** Estado host-side do efeito; o engine continua produzindo apenas a intencao pura. */
@Schema(description = "Estado host-side do laboratorio: planejado ou registrado no ledger local; nao confirma execucao em sistema externo.")
public enum ExtraordinaryBenefitEffectStatus {
    PLANNED,
    LOCAL_RECORDED
}
