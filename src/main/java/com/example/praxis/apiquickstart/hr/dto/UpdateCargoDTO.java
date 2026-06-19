package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateCargoDTO", description = "Comando para revisar os atributos editaveis de um cargo existente, preservando sua identidade tecnica e atualizando a semantica usada por lotacao, progressao e enquadramento remuneratorio.")
public class UpdateCargoDTO extends CreateCargoDTO {
}
