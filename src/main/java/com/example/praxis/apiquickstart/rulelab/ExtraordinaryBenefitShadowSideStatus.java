package com.example.praxis.apiquickstart.rulelab;

import io.swagger.v3.oas.annotations.media.Schema;

/** Estado de execucao de um lado da comparacao, sem expor excecao ou payload. */
@Schema(description = "Estado sanitizado da execucao isolada do baseline ou do candidato.")
public enum ExtraordinaryBenefitShadowSideStatus {
    SUCCESS,
    TIMEOUT,
    ERROR
}
