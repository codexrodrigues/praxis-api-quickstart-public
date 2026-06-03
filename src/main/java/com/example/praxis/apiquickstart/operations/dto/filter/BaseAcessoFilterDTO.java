package com.example.praxis.apiquickstart.operations.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "BaseAcessoFilterDTO",
        description = "Criterios de busca em credenciais de acesso a bases (quem entra ate onde); nao e a credencial a emitir so com filtrar. "
                + "GenericFilter / POST /filter (demo).")
public class BaseAcessoFilterDTO implements GenericFilterDTO {
    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.BASES + "/options/filter", icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "base.id")
    @Schema(
            description = "Apenas acessos a esta base; EQUAL baseId (demo).")
    private Integer baseId;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Acessos de um colaborador; EQUAL (demo).")
    private Integer funcionarioId;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 255, order = 30, icon = "trending_up")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Classe de clearance (A, S, R); LIKE em nivelAcesso (demo).")
    private String nivelAcesso;

    @UISchema(type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, order = 40, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Somente vias ativas ou incluir revogadas; EQUAL (demo).")
    private Boolean ativo;

    public Integer getBaseId() { return baseId; }
    public void setBaseId(Integer baseId) { this.baseId = baseId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getNivelAcesso() { return nivelAcesso; }
    public void setNivelAcesso(String nivelAcesso) { this.nivelAcesso = nivelAcesso; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
