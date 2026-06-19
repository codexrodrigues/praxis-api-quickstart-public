package com.example.praxis.apiquickstart.operations.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.AcordoStatus;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "AcordosRegulatorioFilterDTO",
        description = "Criterios de busca em acordos regulatorios firmados com autoridades ou jurisdicoes. "
                + "Apoia descoberta por nome, territorio, status de vigencia e conteudo resumido do instrumento.")
public class AcordosRegulatorioFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Nome do acordo", controlType = FieldControlType.INPUT, maxLength = 200, order = 10,
            helpText = "Busca pelo nome curto ou título do acordo regulatório.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome curto ou titulo publico do acordo regulatorio.")
    private String nome;

    @UISchema(label = "Jurisdição", controlType = FieldControlType.INPUT, maxLength = 120, order = 20,
            helpText = "Filtra pelo órgão, território ou jurisdição responsável.", icon = "gavel")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do orgao, territorio ou jurisdicao responsavel pelo acordo.")
    private String jurisdicao;

    @UISchema(label = "Status do acordo", controlType = FieldControlType.SELECT, order = 30,
            helpText = "Mostra acordos conforme vigência, homologação ou suspensão.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Estado de vigencia, homologacao ou suspensao do acordo regulatorio.")
    private AcordoStatus status;

    @UISchema(label = "Descrição do acordo", controlType = FieldControlType.INPUT, maxLength = 4000, order = 40,
            helpText = "Busca por palavras-chave no resumo ou nas cláusulas do acordo.", icon = "description")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do resumo, objetivo ou clausulas relevantes do acordo.")
    private String descricao;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getJurisdicao() { return jurisdicao; }
    public void setJurisdicao(String jurisdicao) { this.jurisdicao = jurisdicao; }
    public AcordoStatus getStatus() { return status; }
    public void setStatus(AcordoStatus status) { this.status = status; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
