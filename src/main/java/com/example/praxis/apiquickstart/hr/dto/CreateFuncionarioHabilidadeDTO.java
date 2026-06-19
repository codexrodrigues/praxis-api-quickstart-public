package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "funcionarioNome", "habilidadeNome"})
@Schema(name = "CreateFuncionarioHabilidadeDTO", description = "Comando para associar um colaborador a uma habilidade do catalogo, registrando proficiencia e origem para matriz de competencias e alocacao a missoes.")
public class CreateFuncionarioHabilidadeDTO extends FuncionarioHabilidadeDTO {
}
