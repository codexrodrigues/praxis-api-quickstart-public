package com.example.praxis.apiquickstart.operations.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateBaseAcessoDTO", description = "Comando para revisar uma credencial de acesso existente, ajustando base, colaborador, nivel, vigencia ou estado sem alterar sua identidade tecnica.")
public class UpdateBaseAcessoDTO extends CreateBaseAcessoDTO {
}

