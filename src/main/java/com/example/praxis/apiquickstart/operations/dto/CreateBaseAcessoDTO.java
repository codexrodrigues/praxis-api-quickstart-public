package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "baseNome", "funcionarioNome"})
@Schema(name = "CreateBaseAcessoDTO", description = "Comando para conceder credencial de acesso a uma base, vinculando colaborador, instalacao, nivel de autorizacao, vigencia e estado ativo para auditoria operacional.")
public class CreateBaseAcessoDTO extends BaseAcessoDTO {
}

