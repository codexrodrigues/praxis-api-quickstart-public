package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload de atualizacao completa de funcionario.
 *
 * <p>Herda as mesmas restricoes do create porque o quickstart trata o PUT
 * como substituicao integral do estado editavel do recurso.
 */
@Schema(
    name = "UpdateFuncionarioDTO",
    description = "Comando de substituicao dos dados editaveis do colaborador, preservando identidade tecnica e campos resolvidos pelo backend. Alteracoes em documentos, contato, remuneracao e vinculos continuam sujeitas a governanca de privacidade e revisao quando usadas por IA."
)
public class UpdateFuncionarioDTO extends CreateFuncionarioDTO {
}
