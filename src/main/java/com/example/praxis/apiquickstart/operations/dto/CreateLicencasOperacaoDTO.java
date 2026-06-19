package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "acordoNome", "funcionarioNome", "equipeNome"})
@Schema(name = "CreateLicencasOperacaoDTO", description = "Comando para conceder uma licenca operacional baseada em acordo regulatorio, vinculando colaborador ou equipe, classe de autorizacao e janela de vigencia.")
public class CreateLicencasOperacaoDTO extends LicencasOperacaoDTO {
}

