package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "funcionarioNome"})
@Schema(name = "CreateDependenteDTO", description = "Comando para declarar um dependente de colaborador, registrando nome, parentesco, nascimento e titular para beneficios, elegibilidade e obrigacoes cadastrais sob governanca de dados pessoais.")
public class CreateDependenteDTO extends DependenteDTO {
}
