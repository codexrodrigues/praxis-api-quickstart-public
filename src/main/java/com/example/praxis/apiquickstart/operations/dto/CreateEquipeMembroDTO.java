package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "equipeNome", "funcionarioNome"})
@Schema(name = "CreateEquipeMembroDTO", description = "Comando para associar colaborador a equipe operacional, registrando papel, lideranca ou vigencia usada por escala, disponibilidade e planejamento de missao.")
public class CreateEquipeMembroDTO extends EquipeMembroDTO {
}

