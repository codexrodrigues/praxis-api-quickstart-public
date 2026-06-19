package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "basePrincipalNome"})
@Schema(name = "CreateEquipeDTO", description = "Comando para criar uma equipe operacional, informando identidade, especialidade, disponibilidade e base principal para composicao de missoes e planejamento tatico.")
public class CreateEquipeDTO extends EquipeDTO {
}

