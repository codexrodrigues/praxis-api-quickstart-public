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
@Schema(
    name = "CreateFolhasPagamentoDTO",
    description = "Comando para registrar uma folha de pagamento de um colaborador em uma competencia, consolidando salario bruto, descontos, liquido, data de pagamento e vinculos operacionais. Os valores financeiros alimentam analytics e seguem governanca de privacidade e politica interna."
)
public class CreateFolhasPagamentoDTO extends FolhasPagamentoDTO {
}
