package com.example.praxis.apiquickstart.rulelab;

import io.swagger.v3.oas.annotations.media.Schema;

/** Estado host-side do efeito; o engine continua produzindo apenas a intencao pura. */
@Schema(description = "Estado host-side do efeito: planejado pela decisao ou efetivamente executado pelo host.")
public enum ExtraordinaryBenefitEffectStatus {
    PLANNED,
    EXECUTED
}
