package com.example.praxis.apiquickstart.riskintelligence.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id"})
@Schema(name = "CreateAmeacaDTO", description = "Corpo de criacao no risk intelligence; campos do POST. OpenAPI 3.1 (demo).")
public class CreateAmeacaDTO extends AmeacaDTO {
}

