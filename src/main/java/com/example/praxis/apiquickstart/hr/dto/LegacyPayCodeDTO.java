package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "LegacyPayCodeDTO",
        description = "Codigo de folha mantido por um backend legado. O contrato publico e resource-oriented, "
                + "mas criacao, edicao, exclusao e duplicacao de rascunho sao executadas por um adaptador de comando do host.")
public class LegacyPayCodeDTO {

    @Schema(description = "Identificador publico do codigo de folha no recurso Praxis.", example = "101")
    private Integer id;

    @NotBlank
    @Size(max = 40)
    @UISchema(label = "Codigo", required = true, maxLength = 40, group = "Principal", order = 10, helpText = "Codigo operacional usado pelo sistema legado de folha.")
    @Schema(description = "Codigo operacional reconhecido pelo backend legado de folha para classificar eventos e rubricas.", example = "EVT-HAZARD")
    private String code;

    @NotBlank
    @Size(max = 240)
    @UISchema(label = "Descricao", required = true, maxLength = 240, group = "Principal", order = 20, helpText = "Descricao de negocio exibida em telas e auditorias.")
    @Schema(description = "Descricao de negocio do codigo, usada por operadores para entender quando a rubrica deve ser aplicada.")
    private String description;

    @NotBlank
    @Size(max = 80)
    @UISchema(label = "Categoria", required = true, maxLength = 80, group = "Governanca", order = 10, helpText = "Grupo de classificacao da folha.")
    @Schema(description = "Categoria de folha que orienta calculo, conferencia e conciliacao operacional.", example = "additional-pay")
    private String payrollCategory;

    @NotBlank
    @Size(max = 40)
    @UISchema(label = "Status", required = true, maxLength = 40, group = "Governanca", order = 20, helpText = "Estado operacional retornado pelo legado.")
    @Schema(description = "Estado operacional do codigo conforme guard do host legado; valores comuns sao ACTIVE, DRAFT e RETIRED.", example = "ACTIVE")
    private String status;

    @NotNull
    @UISchema(label = "Ativo", controlType = FieldControlType.CHECKBOX, required = true, group = "Governanca", order = 30, helpText = "Indica se o codigo pode ser usado em novos eventos.")
    @Schema(description = "Sinal publico de uso operacional. Nao substitui as permissoes dinamicas avaliadas pelo provider de availability.")
    private Boolean active = Boolean.TRUE;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPayrollCategory() { return payrollCategory; }
    public void setPayrollCategory(String payrollCategory) { this.payrollCategory = payrollCategory; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
