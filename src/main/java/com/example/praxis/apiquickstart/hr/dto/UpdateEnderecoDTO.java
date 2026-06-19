package com.example.praxis.apiquickstart.hr.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateEnderecoDTO", description = "Comando para substituir os dados editaveis de um endereco de colaborador, preservando o identificador do recurso e atualizando a localizacao usada por RH e operacoes.")
public class UpdateEnderecoDTO extends CreateEnderecoDTO {
}
