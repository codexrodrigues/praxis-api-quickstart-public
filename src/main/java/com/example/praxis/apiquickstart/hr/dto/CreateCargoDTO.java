package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id"})
@Schema(name = "CreateCargoDTO", description = "Comando para criar uma funcao no catalogo de cargos de RH, definindo titulo, senioridade, faixa salarial de referencia e texto de apoio usado por alocacao, progressao e governanca remuneratoria.")
public class CreateCargoDTO extends CargoDTO {
}
