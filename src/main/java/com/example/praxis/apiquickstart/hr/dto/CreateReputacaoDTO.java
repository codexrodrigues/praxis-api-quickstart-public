package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "funcionarioNome"})
@Schema(name = "CreateReputacaoDTO", description = "Comando para registrar snapshot reputacional de colaborador, informando scores publico e governamental usados por rankings, perfil e analises de risco operacional.")
public class CreateReputacaoDTO extends ReputacaoDTO {
}
