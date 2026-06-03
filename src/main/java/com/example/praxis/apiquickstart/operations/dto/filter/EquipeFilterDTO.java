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
        description = "Criterios de busca em equipas taticas (nao e a Equipe a editar so por filtrar). "
                + "Ancora em base principal e estados; GenericFilter / POST /filter (demo).")
public class EquipeFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 10, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Nome de unidade (ex.: Esquadrao Alpha); LIKE (demo).")
    private String nome;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 50, order = 15, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Sigla curta; LIKE (demo).")
    private String sigla;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 20,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.BASES + "/options/filter", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "basePrincipal.id")
    @Schema(
            description = "Filtrar equipas ancoradas nesta base; EQUAL basePrincipalId (FK) (demo).")
    private Integer basePrincipalId;

    @UISchema(controlType = FieldControlType.SELECT, order = 30, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Estado unico; EQUAL EquipeStatus (demo).")
    private EquipeStatus status;

    @UISchema(label = "Status (Incluir)", controlType = FieldControlType.SELECT, order = 40, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.IN)
    @Schema(
            description = "Uniao de estados aceites; operacao IN (demo).")
    private java.util.List<EquipeStatus> statusIn;

    @UISchema(label = "Status (Excluir)", controlType = FieldControlType.SELECT, order = 50, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN)
    @Schema(
            description = "Excluir estados; operacao NOT_IN (demo).")
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


