package com.example.praxis.apiquickstart.riskintelligence.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(
        name = "VwIndicadoresIncidenteFilterDTO",
        description = "Criterios de busca sobre a vista VwIndicadoresIncidente (nao e o incidente nem a indenizacao a editar). "
                + "Filtra agregados de dano, cobertura e tempo; distinto de IncidenteFilterDTO. GenericFilter / POST /filter (demo Risk).")
public class VwIndicadoresIncidenteFilterDTO implements GenericFilterDTO {
    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.INCIDENTES + "/options/filter", icon = "report_problem")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Foco num unico incidente; EQUAL (FK) (demo).")
    private Integer incidenteId;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 20, icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Pesquisa em missao associada (texto na vista); LIKE (demo).")
    private String missao;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 2000, order = 30, icon = "description")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Fragmento de descricao do painel; LIKE (demo).")
    private String descricao;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 40, icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Local do fato; LIKE (demo).")
    private String local;

    @UISchema(controlType = FieldControlType.ASYNC_SELECT, maxLength = 50, order = 50,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.RiskIntelligence.VW_INDICADORES_INCIDENTES + "/option-sources/severidade/options/filter", icon = "emergency")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Severidade catalogada; EQUAL via option-source (demo).")
    private String severidade;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 60,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", icon = "calendar_today")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "danosCivis")
    @Schema(
            description = "Faixa de estimativa de danos a terceiros; BETWEEN (demo).")
    private List<BigDecimal> danosCivisBetween;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 70,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", icon = "stacked_bar_chart")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "totalIndenizacoes")
    @Schema(
            description = "Faixa de soma de indenizacoes; BETWEEN (demo).")
    private List<BigDecimal> totalIndenizacoesBetween;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 80,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "totalPago")
    @Schema(
            description = "Faixa de valor ja pago; BETWEEN (tesouraria) (demo).")
    private List<BigDecimal> totalPagoBetween;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 90,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "totalPendente")
    @Schema(
            description = "Faixa de saldo pendente; BETWEEN (demo).")
    private List<BigDecimal> totalPendenteBetween;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 100, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "ocorridoEm")
    @Schema(
            description = "Janela temporal do ocorrido; BETWEEN em OffsetDateTime (demo).")
    private List<OffsetDateTime> ocorridoEmBetween;

    @UISchema(label = "Ocorrido em (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 110, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "ocorridoEm")
    @Schema(
            description = "Incidentes cujo instante cai neste dia civil; ON_DATE (demo).")
    private LocalDate ocorridoEmOn;

    @UISchema(label = "Ocorrido em (Ultimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 120, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "ocorridoEm")
    @Schema(
            description = "Recencia: ocorridos nos ultimos N dias; IN_LAST_DAYS (demo).")
    private Integer ocorridoEmLastDays;

    public Integer getIncidenteId() { return incidenteId; }
    public void setIncidenteId(Integer incidenteId) { this.incidenteId = incidenteId; }
    public String getMissao() { return missao; }
    public void setMissao(String missao) { this.missao = missao; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public String getSeveridade() { return severidade; }
    public void setSeveridade(String severidade) { this.severidade = severidade; }
    public List<BigDecimal> getDanosCivisBetween() { return danosCivisBetween; }
    public void setDanosCivisBetween(List<BigDecimal> danosCivisBetween) { this.danosCivisBetween = danosCivisBetween; }
    public List<BigDecimal> getTotalIndenizacoesBetween() { return totalIndenizacoesBetween; }
    public void setTotalIndenizacoesBetween(List<BigDecimal> totalIndenizacoesBetween) { this.totalIndenizacoesBetween = totalIndenizacoesBetween; }
    public List<BigDecimal> getTotalPagoBetween() { return totalPagoBetween; }
    public void setTotalPagoBetween(List<BigDecimal> totalPagoBetween) { this.totalPagoBetween = totalPagoBetween; }
    public List<BigDecimal> getTotalPendenteBetween() { return totalPendenteBetween; }
    public void setTotalPendenteBetween(List<BigDecimal> totalPendenteBetween) { this.totalPendenteBetween = totalPendenteBetween; }
    public List<OffsetDateTime> getOcorridoEmBetween() { return ocorridoEmBetween; }
    public void setOcorridoEmBetween(List<OffsetDateTime> ocorridoEmBetween) { this.ocorridoEmBetween = ocorridoEmBetween; }
    public LocalDate getOcorridoEmOn() { return ocorridoEmOn; }
    public void setOcorridoEmOn(LocalDate ocorridoEmOn) { this.ocorridoEmOn = ocorridoEmOn; }
    public Integer getOcorridoEmLastDays() { return ocorridoEmLastDays; }
    public void setOcorridoEmLastDays(Integer ocorridoEmLastDays) { this.ocorridoEmLastDays = ocorridoEmLastDays; }
}
