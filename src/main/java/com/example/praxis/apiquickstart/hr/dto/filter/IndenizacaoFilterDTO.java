package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.math.BigDecimal;
import java.util.List;

@Schema(
        name = "IndenizacaoFilterDTO",
        description = "Criterios de busca de cobertura indenizatoria/ sinistros (nao e o lancamento ajustavel so com filtro). "
                + "Cruza com Operacoes (Incidente); GenericFilter / POST /filter (demo).")
public class IndenizacaoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Incidente", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.INCIDENTES + "/options/filter", helpText = "Filtrar indenizações associadas a um incidente.", icon = "report_problem")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "incidente.id")
    @Schema(
            description = "Apenas parcelas de um dado incidente; EQUAL (FK) (demo).")
    private Integer incidenteId;

    @UISchema(label = "Status de Pagamento", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, order = 20, helpText = "Filtrar por status de pagamento (paga ou pendente).", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Somente pagas ou a pagar; EQUAL na flag pago (tesouraria) (demo).")
    private Boolean pago;

    @UISchema(label = "Faixa de Valor", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 30,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Buscar indenizações por faixa de valor.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "valor")
    @Schema(
            description = "Faixa de valor acordado; BETWEEN na moeda (demo).")
    private List<BigDecimal> valorBetween;

    @UISchema(label = "Seguradora", controlType = FieldControlType.INPUT, maxLength = 200, order = 40, helpText = "Buscar por nome da seguradora responsável.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Seguradora/ TPA; LIKE em nome (demo).")
    private String seguradora;

    @UISchema(label = "Nº do Processo", controlType = FieldControlType.INPUT, maxLength = 100, order = 50, helpText = "Filtrar pelo número do processo ou sinistro.", icon = "fingerprint")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Protocolo de processo/ sinistro; LIKE (demo).")
    private String processoNum;

    public Integer getIncidenteId() { return incidenteId; }
    public void setIncidenteId(Integer incidenteId) { this.incidenteId = incidenteId; }
    public Boolean getPago() { return pago; }
    public void setPago(Boolean pago) { this.pago = pago; }
    public List<BigDecimal> getValorBetween() { return valorBetween; }
    public void setValorBetween(List<BigDecimal> valorBetween) { this.valorBetween = valorBetween; }
    public String getSeguradora() { return seguradora; }
    public void setSeguradora(String seguradora) { this.seguradora = seguradora; }
    public String getProcessoNum() { return processoNum; }
    public void setProcessoNum(String processoNum) { this.processoNum = processoNum; }
}
