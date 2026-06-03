package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "BaseAcessoDTO",
        description = "Liberacao de pessoa a base (nivel de credencial, ativo, vigencia operacional). "
                + "Payload de borda; nao e criterio de filtro. OpenAPI 3.1 e x-ui (demo).")
public class BaseAcessoDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @NotNull
    @UISchema(label = "Base", controlType = FieldControlType.SELECT, group = "Relacionamentos", order = 10,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Operations.BASES + "/options/filter",
            tableHidden = true, icon = "location_on")
    @Schema(
            description = "FK; instalacao a que o acesso se refere (baseId).")
    private Integer baseId;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.SELECT, group = "Relacionamentos", order = 20,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter",
            tableHidden = true, icon = "badge")
    @Schema(
            description = "FK; colaborador beneficiario (funcionarioId).")
    private Integer funcionarioId;

    @UISchema(label = "Base", readOnly = true, formHidden = true, group = "Relacionamentos", order = 11, icon = "location_on")
    @Schema(
            description = "Nome da base denormalizado (read model).")
    private String baseNome;
    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, group = "Relacionamentos", order = 21, icon = "badge")
    @Schema(
            description = "Nome do colaborador denormalizado (read model).")
    private String funcionarioNome;

    @NotBlank
    @Size(max = 255)
    @UISchema(label = "Nível de Acesso", type = FieldDataType.TEXT, required = true, maxLength = 255, group = "Principal", order = 10, icon = "trending_up")
    @Schema(
            description = "String de classificacao de corredor (ex. TSI-3); politica e interpretada no backend.")
    private String nivelAcesso;

    @NotNull
    @UISchema(label = "Ativo", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, group = "Principal", order = 20, icon = "toggle_on")
    @Schema(
            description = "Se falso, credencial suspensa; bloqueia novos acessos fisicos.")
    private Boolean ativo;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getBaseId() { return baseId; }
    public void setBaseId(Integer baseId) { this.baseId = baseId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getBaseNome() { return baseNome; }
    public void setBaseNome(String baseNome) { this.baseNome = baseNome; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public void setFuncionarioNome(String funcionarioNome) { this.funcionarioNome = funcionarioNome; }
    public String getNivelAcesso() { return nivelAcesso; }
    public void setNivelAcesso(String nivelAcesso) { this.nivelAcesso = nivelAcesso; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}

