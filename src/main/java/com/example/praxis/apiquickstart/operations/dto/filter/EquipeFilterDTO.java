package com.example.praxis.apiquickstart.operations.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.enums.EquipeStatus;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "EquipeFilterDTO",
        description = "Criterios de busca em equipes taticas de Operacoes. "
                + "Apoia descoberta por nome, sigla, base principal e estado operacional da equipe.")
public class EquipeFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Nome da equipe", controlType = FieldControlType.INPUT, maxLength = 200, order = 10,
            helpText = "Digite parte do nome da equipe tática.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome da unidade tatica usado para localizar equipes operacionais.")
    private String nome;

    @UISchema(label = "Sigla da equipe", controlType = FieldControlType.INPUT, maxLength = 50, order = 15,
            helpText = "Digite a sigla ou parte da sigla da equipe.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho da sigla usada como identificador curto da equipe.")
    private String sigla;

    @UISchema(label = "Base principal", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.BASES_BASE_LOOKUP_OPTIONS,
            helpText = "Selecione a base operacional principal da equipe.", icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "basePrincipal.id")
    @Schema(
            description = "Base operacional principal onde a equipe esta ancorada.")
    private Integer basePrincipalId;

    @UISchema(label = "Status da equipe", controlType = FieldControlType.SELECT, order = 30,
            helpText = "Filtra por um único estado da equipe.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Estado operacional atual da equipe, usado para separar equipes ativas, indisponiveis ou em reorganizacao.")
    private EquipeStatus status;

    @UISchema(label = "Mostrar status", controlType = FieldControlType.SELECT, order = 40,
            helpText = "Mostra apenas equipes nos status selecionados.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "status")
    @Schema(
            description = "Conjunto de estados de equipe que devem aparecer no resultado da busca.")
    private java.util.List<EquipeStatus> statusIn;

    @UISchema(label = "Ocultar status", controlType = FieldControlType.SELECT, order = 50,
            helpText = "Remove do resultado as equipes nos status selecionados.", icon = "toggle_off")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN, relation = "status")
    @Schema(
            description = "Conjunto de estados de equipe que devem ser removidos do resultado da busca.")
    private java.util.List<EquipeStatus> statusNotIn;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }
    public Integer getBasePrincipalId() { return basePrincipalId; }
    public void setBasePrincipalId(Integer basePrincipalId) { this.basePrincipalId = basePrincipalId; }
    public EquipeStatus getStatus() { return status; }
    public void setStatus(EquipeStatus status) { this.status = status; }
    public java.util.List<EquipeStatus> getStatusIn() { return statusIn; }
    public void setStatusIn(java.util.List<EquipeStatus> statusIn) { this.statusIn = statusIn; }
    public java.util.List<EquipeStatus> getStatusNotIn() { return statusNotIn; }
    public void setStatusNotIn(java.util.List<EquipeStatus> statusNotIn) { this.statusNotIn = statusNotIn; }
}


