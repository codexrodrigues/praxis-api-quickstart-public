package com.example.praxis.apiquickstart.rulelab.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Motivo corporativo declarado para solicitar o beneficio extraordinario. */
@Schema(description = "Categoria de necessidade que fundamenta a solicitacao e orienta auditoria e analise posterior.")
public enum ExtraordinaryBenefitReason {
    EMERGENCY_MEDICAL,
    FAMILY_HARDSHIP,
    DISASTER_RECOVERY,
    OTHER
}
