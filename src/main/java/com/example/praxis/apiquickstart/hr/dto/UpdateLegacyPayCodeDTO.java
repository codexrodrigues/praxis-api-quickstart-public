package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "UpdateLegacyPayCodeDTO",
        description = "Comando publico para revisar codigo de folha mantendo a identidade do recurso e delegando a mutacao ao legado.")
public class UpdateLegacyPayCodeDTO extends CreateLegacyPayCodeDTO {
}
