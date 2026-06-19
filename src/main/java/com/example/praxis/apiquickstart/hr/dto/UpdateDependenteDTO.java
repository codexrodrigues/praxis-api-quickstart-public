package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateDependenteDTO", description = "Comando para revisar os dados cadastrais editaveis de um dependente, preservando sua identidade tecnica e mantendo o vinculo com o colaborador titular.")
public class UpdateDependenteDTO extends CreateDependenteDTO {
}
