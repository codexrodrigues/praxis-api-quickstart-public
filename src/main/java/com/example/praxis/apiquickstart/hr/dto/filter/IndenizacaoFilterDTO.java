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
        description = "Criterios de busca de indenizacoes e coberturas vinculadas a incidentes operacionais. "
                + "Apoia acompanhamento financeiro por incidente, status de pagamento, valor, seguradora e protocolo de sinistro.")
public class IndenizacaoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Incidente", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.INCIDENTES_INCIDENT_LOOKUP_OPTIONS, helpText = "Filtrar indenizações associadas a um incidente.", icon = "report_problem")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "incidente.id")
    @Schema(
            description = "Incidente operacional que originou a indenizacao ou processo de cobertura.")
    private Integer incidenteId;

    @UISchema(label = "Status de Pagamento", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, order = 20, helpText = "Filtrar por status de pagamento (paga ou pendente).", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Situacao de liquidacao financeira que separa indenizacoes pagas de valores ainda pendentes.")
    private Boolean pago;

    @UISchema(label = "Faixa de Valor", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 30,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Buscar indenizações por faixa de valor.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "valor")
    @Schema(
            description = "Faixa de valor acordado ou provisionado para a indenizacao.")
    private List<BigDecimal> valorBetween;

    @UISchema(label = "Seguradora", controlType = FieldControlType.INPUT, maxLength = 200, order = 40, helpText = "Buscar por nome da seguradora responsável.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome da seguradora, administradora ou terceiro responsavel pela cobertura.")
    private String seguradora;

    @UISchema(label = "Nº do Processo", controlType = FieldControlType.INPUT, maxLength = 100, order = 50, helpText = "Filtrar pelo número do processo ou sinistro.", icon = "fingerprint")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do numero de processo, protocolo ou sinistro usado para rastrear a cobertura.")
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
