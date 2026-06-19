package com.example.praxis.apiquickstart.operations.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateIncidenteDTO", description = "Comando para revisar um incidente existente, preservando sua identidade e atualizando estado, severidade, narrativa ou vinculos operacionais de acompanhamento.")
public class UpdateIncidenteDTO extends CreateIncidenteDTO {
}

