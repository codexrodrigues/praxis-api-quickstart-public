package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload de atualizacao completa de funcionario.
 *
 * <p>Herda as mesmas restricoes do create porque o quickstart trata o PUT
 * como substituicao integral do estado editavel do recurso.
 */
@Schema(name = "UpdateFuncionarioDTO", description = "Corpo de atualizacao no modulo Recursos humanos; campos mutaveis. OpenAPI 3.1 (demo).")
public class UpdateFuncionarioDTO extends CreateFuncionarioDTO {
}
