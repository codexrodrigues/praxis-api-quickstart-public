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
@Schema(
    name = "CreateFuncionarioDTO",
    description = "Comando para cadastrar um colaborador com identificacao civil, contato, vinculo organizacional e remuneracao inicial. Campos resolvidos pelo servidor, como id, avatar e nomes de cargo/departamento, nao fazem parte da autoria do cadastro."
)
public class CreateFuncionarioDTO extends FuncionarioDTO {
}
