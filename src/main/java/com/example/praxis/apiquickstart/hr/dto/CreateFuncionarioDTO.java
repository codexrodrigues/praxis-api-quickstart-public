package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload de criacao de funcionario.
 *
 * <p>Reaproveita o contrato principal, mas ignora campos resolvidos pelo backend
 * ou derivados para leitura, como id, avatar e nomes de relacionamentos.
 */
@JsonIgnoreProperties({"id", "avatarUrl", "cargoNome", "departamentoNome"})
@Schema(name = "CreateFuncionarioDTO", description = "Corpo de criacao no modulo Recursos humanos; campos do POST. OpenAPI 3.1 (demo).")
public class CreateFuncionarioDTO extends FuncionarioDTO {
}
