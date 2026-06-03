package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload de criacao da folha.
 *
 * <p>Oculta apenas o identificador tecnico, mantendo explicitos os campos
 * de competencia, valores consolidados e vinculo com o funcionario.
 */
@JsonIgnoreProperties({"id"})
@Schema(name = "CreateFolhasPagamentoDTO", description = "Corpo de criacao no modulo Recursos humanos; campos do POST. OpenAPI 3.1 (demo).")
public class CreateFolhasPagamentoDTO extends FolhasPagamentoDTO {
}
