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
        description = "Criterios de busca em credenciais de acesso a bases operacionais. "
                + "Apoia auditoria de autorizacoes por base, colaborador, nivel de acesso e situacao ativa.")
public class BaseAcessoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Base", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.BASES_BASE_LOOKUP_OPTIONS,
            helpText = "Mostra credenciais de acesso emitidas para uma base específica.", icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "base.id")
    @Schema(
            description = "Base operacional para a qual a credencial de acesso foi emitida.")
    private Integer baseId;

    @UISchema(label = "Colaborador", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            helpText = "Lista os acessos concedidos a um colaborador ou herói.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador ou heroi que recebeu a credencial de acesso a base.")
    private Integer funcionarioId;

    @UISchema(label = "Nível de acesso", controlType = FieldControlType.INPUT, maxLength = 255, order = 30,
            helpText = "Busca por classe de credencial, clearance ou nível operacional.", icon = "trending_up")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nivel, classe de clearance ou regra operacional associada a credencial.")
    private String nivelAcesso;

    @UISchema(label = "Acesso ativo", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, order = 40,
            helpText = "Diferencia acessos ativos de credenciais revogadas ou expiradas.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Situacao da credencial que separa acessos ativos de registros revogados ou expirados.")
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
