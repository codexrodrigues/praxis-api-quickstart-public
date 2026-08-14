package com.example.praxis.apiquickstart.hr.dto.determination;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Data determinada pelo calendario corporativo e versao da decisao executada.")
public record PayrollPaymentDateDeterminationResponse(
        LocalDate dataPagamento,
        String decisionVersion
) {}
