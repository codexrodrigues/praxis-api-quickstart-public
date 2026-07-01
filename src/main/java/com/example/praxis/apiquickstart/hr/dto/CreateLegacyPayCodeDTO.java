package com.example.praxis.apiquickstart.hr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({"id"})
@Schema(
        name = "CreateLegacyPayCodeDTO",
        description = "Comando publico para criar codigo de folha cuja escrita sera delegada ao adaptador legado fake do quickstart.")
public class CreateLegacyPayCodeDTO extends LegacyPayCodeDTO {
}
