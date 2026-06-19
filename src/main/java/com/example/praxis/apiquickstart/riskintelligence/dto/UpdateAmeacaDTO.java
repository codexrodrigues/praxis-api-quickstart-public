package com.example.praxis.apiquickstart.riskintelligence.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(
        name = "UpdateAmeacaDTO",
        description = "Comando para revisar o cadastro de uma ameaca existente quando mudam classe, area de atuacao, nivel de risco, status operacional ou recompensa.")
public class UpdateAmeacaDTO extends CreateAmeacaDTO {
}

