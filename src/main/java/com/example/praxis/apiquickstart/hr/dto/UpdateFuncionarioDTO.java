package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload de atualizacao completa de funcionario.
 *
 * <p>Reutiliza o vocabulário explícito do comando de criação porque o quickstart trata o PUT
 * como substituição integral do estado editável. Essa herança ocorre entre contratos de escrita;
 * o DTO de leitura não participa da hierarquia.
 */
@Schema(
    name = "UpdateFuncionarioDTO",
    description = "Comando de substituicao dos dados editaveis do colaborador, preservando identidade tecnica e campos resolvidos pelo backend. Alteracoes em documentos, contato, remuneracao e vinculos continuam sujeitas a governanca de privacidade e revisao quando usadas por IA."
)
public class UpdateFuncionarioDTO extends CreateFuncionarioDTO {
}
