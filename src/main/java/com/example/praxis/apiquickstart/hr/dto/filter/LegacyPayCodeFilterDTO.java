package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "LegacyPayCodeFilterDTO",
        description = "Criterios de busca para codigos de folha legados publicados como recurso Praxis canônico.")
public class LegacyPayCodeFilterDTO implements GenericFilterDTO {

    @UISchema(label = "Codigo", controlType = FieldControlType.INPUT, maxLength = 40, order = 10, helpText = "Filtrar por parte do codigo legado.")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(description = "Trecho do codigo operacional reconhecido pelo backend legado de folha.")
    private String code;

    @UISchema(label = "Descricao", controlType = FieldControlType.INPUT, maxLength = 240, order = 20, helpText = "Filtrar por descricao de negocio.")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(description = "Trecho da descricao de negocio do codigo de folha.")
    private String description;

    @UISchema(label = "Categoria", controlType = FieldControlType.INPUT, maxLength = 80, order = 30, helpText = "Filtrar por categoria de folha.")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(description = "Categoria operacional usada em calculo, conferencia e conciliacao de folha.")
    private String payrollCategory;

    @UISchema(label = "Status", controlType = FieldControlType.INPUT, maxLength = 40, order = 40, helpText = "Filtrar por estado operacional.")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(description = "Estado operacional publico do codigo, como ACTIVE, DRAFT ou RETIRED.")
    private String status;

    @UISchema(label = "Ativo", controlType = FieldControlType.CHECKBOX, order = 50, helpText = "Filtrar codigos ativos ou inativos.")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(description = "Sinal de uso operacional em novos eventos de folha.")
    private Boolean active;

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
