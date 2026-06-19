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
                + "Filtra agregados de dano, cobertura e tempo sem substituir o cadastro transacional de incidentes.")
public class VwIndicadoresIncidenteFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Incidente", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.INCIDENTES_INCIDENT_LOOKUP_OPTIONS,
            helpText = "Foca os indicadores agregados de um incidente específico.", icon = "report_problem")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Identificador do incidente transacional que ancora o indicador materializado.")
    private Integer incidenteId;

    @UISchema(label = "Missão relacionada", controlType = FieldControlType.INPUT, maxLength = 200, order = 20,
            helpText = "Busca pelo texto da missão associada ao incidente.", icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do titulo ou referencia da missao associada ao incidente, pesquisado no texto desnormalizado da vista.")
    private String missao;

    @UISchema(label = "Descrição do incidente", controlType = FieldControlType.INPUT, maxLength = 2000, order = 30,
            helpText = "Busca por palavras-chave na descrição resumida do incidente.", icon = "description")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho da narrativa curta do incidente exibida no painel; permite localizar indicadores por contexto operacional resumido.")
    private String descricao;

    @UISchema(label = "Local do incidente", controlType = FieldControlType.INPUT, maxLength = 200, order = 40,
            helpText = "Filtra pelo local ou região onde o incidente ocorreu.", icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do local ou cenario do fato usado para recortes geograficos e busca operacional.")
    private String local;

    @UISchema(label = "Severidade", controlType = FieldControlType.ASYNC_SELECT, maxLength = 50, order = 50,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.RiskIntelligence.VW_INDICADORES_INCIDENTES + "/option-sources/severidade/options/filter",
            helpText = "Seleciona a gravidade operacional registrada no indicador.", icon = "emergency")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Classe de severidade catalogada para triagem, priorizacao e agrupamento dos indicadores.")
    private String severidade;

    @UISchema(label = "Faixa de danos civis", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 60,
            helpText = "Filtra incidentes por estimativa monetária de danos a terceiros.",
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", icon = "calendar_today")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "danosCivis")
    @Schema(
            description = "Faixa de estimativa financeira de danos civis ou a infraestrutura atribuida ao incidente.")
    private List<BigDecimal> danosCivisBetween;

    @UISchema(label = "Total de indenizações", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 70,
            helpText = "Filtra pelo volume total de indenizações associadas ao incidente.",
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", icon = "stacked_bar_chart")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "totalIndenizacoes")
    @Schema(
            description = "Faixa do total agregado de indenizacoes vinculadas ao incidente.")
    private List<BigDecimal> totalIndenizacoesBetween;

    @UISchema(label = "Total pago", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 80,
            helpText = "Filtra pela faixa já liquidada em indenizações.",
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "totalPago")
    @Schema(
            description = "Faixa do valor agregado ja liquidado, usada para reconciliacao financeira e acompanhamento de cobertura.")
    private List<BigDecimal> totalPagoBetween;

    @UISchema(label = "Total pendente", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 90,
            helpText = "Filtra pela faixa ainda pendente de pagamento.",
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "totalPendente")
    @Schema(
            description = "Faixa do saldo agregado ainda pendente de pagamento ou cobertura.")
    private List<BigDecimal> totalPendenteBetween;

    @UISchema(label = "Período do incidente", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 100,
            helpText = "Filtra por intervalo de data e hora em que o incidente ocorreu.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "ocorridoEm")
    @Schema(
            description = "Janela temporal do incidente, com offset, para recortes de linha do tempo e correlacao com missoes.")
    private List<OffsetDateTime> ocorridoEmBetween;

    @UISchema(label = "Ocorrido em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 110,
            helpText = "Mostra incidentes ocorridos em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "ocorridoEm")
    @Schema(
            description = "Dia civil usado para localizar incidentes ocorridos nessa data, independente do horario exato.")
    private LocalDate ocorridoEmOn;

    @UISchema(label = "Ocorrido nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 120,
            helpText = "Informe quantos dias recentes devem ser considerados na busca.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "ocorridoEm")
    @Schema(
            description = "Janela relativa de recencia para localizar incidentes ocorridos nos ultimos N dias.")
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
