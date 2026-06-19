package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload de atualizacao completa da folha.
 *
 * <p>No quickstart ele reaproveita o contrato de criacao para mostrar que o
 * estado editavel da folha continua o mesmo ao longo do ciclo administrativo.
 */
@Schema(
    name = "UpdateFolhasPagamentoDTO",
    description = "Comando para revisar uma folha de pagamento existente, preservando sua identidade e recalculando a leitura operacional de competencia, valores consolidados, eventos e data de pagamento. Alteracoes podem afetar dashboards, auditoria financeira e actions governadas como marcacao de pagamento."
)
public class UpdateFolhasPagamentoDTO extends CreateFolhasPagamentoDTO {
}
