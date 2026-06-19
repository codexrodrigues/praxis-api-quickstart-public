package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id"})
@Schema(
    name = "CreateAcordosRegulatorioDTO",
    description = "Comando para cadastrar um acordo regulatorio que condiciona operacoes por nome, jurisdicao, descricao, vigencia e status. O registro serve como referencia de compliance para licencas, missoes, auditorias e workflows de revisao."
)
public class CreateAcordosRegulatorioDTO extends AcordosRegulatorioDTO {
}

