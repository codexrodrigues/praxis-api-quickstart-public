package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id"})
@Schema(name = "CreateHistoricoSalarialDTO", description = "Comando para registrar uma fatia de historico salarial de colaborador, com valor, vigencia e motivo usados por auditoria de carreira e preparacao de folha.")
public class CreateHistoricoSalarialDTO extends HistoricoSalarialDTO {
}
