package com.example.praxis.apiquickstart.riskintelligence.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id"})
@Schema(
        name = "CreateAmeacaDTO",
        description = "Comando para cadastrar uma nova ameaca no catalogo de inteligencia de risco, incluindo classificacao, teatro de atuacao, severidade inicial, status e recompensa.")
public class CreateAmeacaDTO extends AmeacaDTO {
}

