package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "funcionarioNome", "cargoNome"})
@Schema(name = "CreateHistoricosCargoDTO", description = "Comando para registrar uma movimentacao de cargo de colaborador, vinculando funcionario, cargo, vigencia e observacoes para trilha de carreira.")
public class CreateHistoricosCargoDTO extends HistoricosCargoDTO {
}
