package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.LocalDate;

@Schema(
        name = "ScheduleFolhaPagamentoDTO",
        description = "Payload de agendamento de pagamento: define a data-alvo (LocalDate) em que a folha ou o lote de liquidacao deve ser executado. "
                + "Usado em actions que movem a folha para estado agendado ou reprogramam credito; nao representa a competencia contabil, mas a data operacional de liquidacao.")
public class ScheduleFolhaPagamentoDTO {

    @NotNull
    @UISchema(
            label = "Payment date",
            type = FieldDataType.DATE,
            controlType = FieldControlType.DATE_PICKER,
            required = true,
            order = 10,
            helpText = "Data programada para execuÃ§Ã£o da folha.",
            icon = "event"
    )
    @Schema(
            description = "Data prevista de pagamento/liquidacao acordada com tesouraria; interpretada no fuso do servidor ou politica de RH do quickstart (sem horario intradia neste DTO).",
            example = "2025-04-30")
    private LocalDate dataPagamento;

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }
}
