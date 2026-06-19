package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "incidenteTitulo"})
@Schema(name = "CreateIndenizacaoDTO", description = "Comando para registrar cobertura indenizatoria associada a incidente operacional, informando valor, liquidacao, seguradora, processo e incidente de origem.")
public class CreateIndenizacaoDTO extends IndenizacaoDTO {
}
