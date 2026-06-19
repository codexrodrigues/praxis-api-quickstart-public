package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id"})
@Schema(name = "CreateIncidenteDTO", description = "Comando para registrar um incidente operacional, capturando titulo, gravidade, status, localizacao e relacoes necessarias para resposta, auditoria e cobertura.")
public class CreateIncidenteDTO extends IncidenteDTO {
}

