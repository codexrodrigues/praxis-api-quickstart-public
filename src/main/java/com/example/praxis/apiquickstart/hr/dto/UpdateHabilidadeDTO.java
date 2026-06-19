package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateHabilidadeDTO", description = "Comando para revisar uma competencia do catalogo de habilidades, atualizando sua classificacao sem alterar diretamente os vinculos ja atribuídos a colaboradores.")
public class UpdateHabilidadeDTO extends CreateHabilidadeDTO {
}
