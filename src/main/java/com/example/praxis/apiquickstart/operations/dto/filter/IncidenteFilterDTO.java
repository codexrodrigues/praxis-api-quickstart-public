package com.example.praxis.apiquickstart.operations.dto.filter;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.dto.filter.support.QuickstartRelativePeriodUiOptions;
import com.example.praxis.apiquickstart.operations.enums.Severidade;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "IncidenteFilterDTO",
        description = "Criterios de busca em incidentes operacionais registrados em campo. "
                + "Apoia analise por missao, severidade, local, janela temporal, danos materiais e impacto humano.")
public class IncidenteFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Descrição", controlType = FieldControlType.INPUT, maxLength = 2000, order = 10,
            helpText = "Busca por palavras-chave na narrativa ou no relatório preliminar.", icon = "description")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho da narrativa ou relatorio preliminar do incidente, tratado como conteudo sensivel de operacao.")
    private String descricao;

    @UISchema(label = "Severidade", controlType = FieldControlType.SELECT, order = 20,
            helpText = "Seleciona uma gravidade específica do incidente.", icon = "emergency")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Grau de gravidade atribuido ao incidente para triagem, priorizacao e escalonamento.")
    private Severidade severidade;

    @UISchema(label = "Local", controlType = FieldControlType.INPUT, maxLength = 200, order = 30,
            helpText = "Busca pelo local, cenário ou coordenada textual do incidente.", icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do local, cenario ou coordenada textual onde o incidente ocorreu.")
    private String local;

    @UISchema(label = "Missão", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 40,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.MISSOES_MISSION_LOOKUP_OPTIONS,
            helpText = "Filtra incidentes ligados a uma missão específica.", icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "missao.id")
    @Schema(
            description = "Missao operacional a qual o incidente esta associado.")
    private Integer missaoId;

    @UISchema(label = "Período do incidente", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 50,
            helpText = "Filtra pela janela de data e hora em que o incidente ocorreu.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "ocorridoEm")
    @Schema(
            description = "Janela de data e hora em que o incidente ocorreu.")
    private List<OffsetDateTime> ocorridoEmBetween;

    @UISchema(label = "Mostrar severidades", controlType = FieldControlType.INLINE_MULTISELECT, order = 60,
            helpText = "Inclui incidentes com qualquer uma das severidades selecionadas.", icon = "emergency")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "severidade")
    @Schema(
            description = "Conjunto de severidades aceitas para compor o resultado da busca.")
    private List<Severidade> severidadesIn;

    @UISchema(label = "Ocultar severidades", controlType = FieldControlType.INLINE_MULTISELECT, order = 70,
            helpText = "Remove do resultado incidentes com as severidades selecionadas.", icon = "emergency")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN, relation = "severidade")
    @Schema(
            description = "Conjunto de severidades que devem ser excluidas do resultado da busca.")
    private List<Severidade> severidadesNotIn;

    @UISchema(label = "Ocorrido em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 80,
            helpText = "Mostra incidentes ocorridos em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "ocorridoEm")
    @Schema(
            description = "Dia civil usado para localizar incidentes ocorridos em uma data especifica.")
    private LocalDate ocorridoEmOn;

    @UISchema(
            label = "Período rápido do incidente",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 90,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, helpText = "Aplica atalhos de tempo, como hoje, esta semana ou últimos períodos configurados.", icon = "event")
    @Schema(
            description = "Atalho de periodo relativo aplicado a data de ocorrencia do incidente.")
    private String ocorridoEmPreset;

    @UISchema(label = "Ocorrido nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 95,
            helpText = "Informe quantos dias recentes devem ser considerados na busca.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "ocorridoEm")
    @Schema(
            description = "Janela relativa para localizar incidentes ocorridos nos ultimos N dias.")
    private Integer ocorridoEmLastDays;

    @UISchema(label = "Faixa de danos civis", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 100,
            helpText = "Filtra por estimativa monetária de danos a terceiros.",
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", icon = "calendar_today")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "danosCivis")
    @Schema(
            description = "Faixa de estimativa monetaria de danos civis ou materiais atribuida ao incidente.")
    private List<BigDecimal> danosCivisBetween;

    @UISchema(label = "Faixa de feridos", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 110,
            helpText = "Filtra incidentes pela quantidade de pessoas feridas.", icon = "health_and_safety")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "feridos")
    @Schema(
            description = "Faixa de quantidade de pessoas feridas registradas no incidente.")
    private List<Integer> feridosBetween;

    @UISchema(label = "Faixa de óbitos", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 120,
            helpText = "Filtra incidentes pela quantidade registrada de óbitos.", icon = "health_and_safety")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "mortos")
    @Schema(
            description = "Faixa de quantidade de obitos registrados no incidente, tratada como dado operacional sensivel.")
    private List<Integer> mortosBetween;

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Severidade getSeveridade() { return severidade; }
    public void setSeveridade(Severidade severidade) { this.severidade = severidade; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public Integer getMissaoId() { return missaoId; }
    public void setMissaoId(Integer missaoId) { this.missaoId = missaoId; }
    public List<OffsetDateTime> getOcorridoEmBetween() { return ocorridoEmBetween; }
    public void setOcorridoEmBetween(List<OffsetDateTime> ocorridoEmBetween) { this.ocorridoEmBetween = ocorridoEmBetween; }

    public List<Severidade> getSeveridadesIn() { return severidadesIn; }
    public void setSeveridadesIn(List<Severidade> severidadesIn) { this.severidadesIn = severidadesIn; }
    public List<Severidade> getSeveridadesNotIn() { return severidadesNotIn; }
    public void setSeveridadesNotIn(List<Severidade> severidadesNotIn) { this.severidadesNotIn = severidadesNotIn; }
    public LocalDate getOcorridoEmOn() { return ocorridoEmOn; }
    public void setOcorridoEmOn(LocalDate ocorridoEmOn) { this.ocorridoEmOn = ocorridoEmOn; }
    public String getOcorridoEmPreset() { return ocorridoEmPreset; }
    public void setOcorridoEmPreset(String ocorridoEmPreset) { this.ocorridoEmPreset = ocorridoEmPreset; }
    public Integer getOcorridoEmLastDays() { return ocorridoEmLastDays; }
    public void setOcorridoEmLastDays(Integer ocorridoEmLastDays) { this.ocorridoEmLastDays = ocorridoEmLastDays; }
    public List<BigDecimal> getDanosCivisBetween() { return danosCivisBetween; }
    public void setDanosCivisBetween(List<BigDecimal> danosCivisBetween) { this.danosCivisBetween = danosCivisBetween; }
    public List<Integer> getFeridosBetween() { return feridosBetween; }
    public void setFeridosBetween(List<Integer> feridosBetween) { this.feridosBetween = feridosBetween; }
    public List<Integer> getMortosBetween() { return mortosBetween; }
    public void setMortosBetween(List<Integer> mortosBetween) { this.mortosBetween = mortosBetween; }
}
