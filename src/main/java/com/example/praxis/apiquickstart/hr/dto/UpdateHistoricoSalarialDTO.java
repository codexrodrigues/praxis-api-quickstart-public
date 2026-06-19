package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateHistoricoSalarialDTO", description = "Comando para revisar valor, vigencia ou motivo de uma fatia salarial existente, preservando a identidade do registro e a rastreabilidade financeira.")
public class UpdateHistoricoSalarialDTO extends CreateHistoricoSalarialDTO {
}
