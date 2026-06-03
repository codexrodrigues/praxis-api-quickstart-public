package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id"})
@Schema(name = "CreateCargoDTO", description = "Corpo de criacao no modulo Recursos humanos; campos do POST. OpenAPI 3.1 (demo).")
public class CreateCargoDTO extends CargoDTO {
}
