package com.example.praxis.apiquickstart.hr.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "funcionarioNome"})
@Schema(name = "CreateEnderecoDTO", description = "Comando para registrar endereco residencial ou de correspondencia de um colaborador, excluindo campos de leitura e tratando localizacao como dado pessoal sensivel para uso em folha, emergencia e logistica.")
public class CreateEnderecoDTO extends EnderecoDTO {
}
