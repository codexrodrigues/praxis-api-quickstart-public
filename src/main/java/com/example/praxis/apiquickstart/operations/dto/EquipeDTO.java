package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.EquipeStatus;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "EquipeDTO",
        description = "Unidade tactica (nome, sigla, base de apoio, status de escalonamento). "
                + "Payload de borda; nao e criterio de filtro. OpenAPI 3.1 e x-ui (demo).")
public class EquipeDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Nome", controlType = FieldControlType.INPUT, required = true, maxLength = 200, icon = "badge")
    @Schema(
            description = "Designacao oficial do grupo de atuacao.")
    private String nome;

    @Size(max = 12)
    @UISchema(label = "Sigla", maxLength = 12, icon = "label")
    @Schema(
            description = "Abreviatura de comunicacoes (ex. ALFA-1).")
    private String sigla;

    @UISchema(label = "Base Principal", controlType = FieldControlType.SELECT,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Operations.BASES + "/options/filter",
            tableHidden = true, icon = "toggle_on")
    @Schema(
            description = "FK; instalacao de origem/ logistica (basePrincipalId).")
    private Integer basePrincipalId;

    @UISchema(label = "Base Principal", readOnly = true, formHidden = true, icon = "toggle_on")
    @Schema(
            description = "Nome da base denormalizado para tabela (read model).")
    private String basePrincipalNome;

    @NotNull
    @UISchema(label = "Status", controlType = FieldControlType.SELECT, required = true, icon = "toggle_on")
    @Schema(
            description = "Elegibilidade de escalonamento; EquipeStatus.")
    private EquipeStatus status;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }
    public Integer getBasePrincipalId() { return basePrincipalId; }
    public void setBasePrincipalId(Integer basePrincipalId) { this.basePrincipalId = basePrincipalId; }
    public String getBasePrincipalNome() { return basePrincipalNome; }
    public void setBasePrincipalNome(String basePrincipalNome) { this.basePrincipalNome = basePrincipalNome; }
    public EquipeStatus getStatus() { return status; }
    public void setStatus(EquipeStatus status) { this.status = status; }
}


