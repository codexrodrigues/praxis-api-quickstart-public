package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id"})
@Schema(name = "CreateSinaisSocorroDTO", description = "Comando para abrir um sinal de socorro operacional, registrando origem, severidade, localizacao e contexto para triagem e resposta.")
public class CreateSinaisSocorroDTO extends SinaisSocorroDTO {
}

