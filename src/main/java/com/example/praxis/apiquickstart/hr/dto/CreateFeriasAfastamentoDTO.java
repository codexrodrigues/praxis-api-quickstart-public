package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id"})
@Schema(name = "CreateFeriasAfastamentoDTO", description = "Comando para registrar um periodo de ferias, licenca ou afastamento de colaborador, reduzindo disponibilidade operacional e alimentando calendario de folha e cobertura.")
public class CreateFeriasAfastamentoDTO extends FeriasAfastamentoDTO {
}
