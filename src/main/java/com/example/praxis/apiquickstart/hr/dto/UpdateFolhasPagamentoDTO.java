package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload de atualizacao completa da folha.
 *
 * <p>No quickstart ele reaproveita o contrato de criacao para mostrar que o
 * estado editavel da folha continua o mesmo ao longo do ciclo administrativo.
 */
@Schema(name = "UpdateFolhasPagamentoDTO", description = "Corpo de atualizacao no modulo Recursos humanos; campos mutaveis. OpenAPI 3.1 (demo).")
public class UpdateFolhasPagamentoDTO extends CreateFolhasPagamentoDTO {
}
