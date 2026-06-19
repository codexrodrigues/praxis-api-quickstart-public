package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id"})
@Schema(name = "CreateHabilidadeDTO", description = "Comando para criar uma competencia no catalogo de habilidades, classificando nome, categoria, descricao e nivel de poder para filtros, perfis e requisitos de missao.")
public class CreateHabilidadeDTO extends HabilidadeDTO {
}
