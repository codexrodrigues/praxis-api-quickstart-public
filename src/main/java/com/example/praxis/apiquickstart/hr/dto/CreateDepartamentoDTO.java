package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "responsavelNome"})
@Schema(name = "CreateDepartamentoDTO", description = "Comando para criar uma unidade organizacional de RH, informando nome, codigo interno e responsavel operacional sem aceitar campos desnormalizados resolvidos pelo backend.")
public class CreateDepartamentoDTO extends DepartamentoDTO {
}
