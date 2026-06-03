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
        description = "Criterios de busca em ocorrencias/ incidentes de campo (nao e o relatorio final a assinar so por filtrar). "
                + "Cruza missao, severidade, impacto; distinto de VwIndicadoresIncidente* (agregado). GenericFilter / POST /filter (demo).")
public class IncidenteFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.INPUT, maxLength = 2000, order = 10, icon = "description")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Texto livre de narrativa ou relatorio preliminar; LIKE (sensivel a LGPD) (demo).")
    private String descricao;

    @UISchema(controlType = FieldControlType.SELECT, order = 20, icon = "emergency")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Grau de gravidade unico; EQUAL Severidade (enum) (demo).")
    private Severidade severidade;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 30, icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Cenario ou coordenada textual; LIKE (demo).")
    private String local;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 40,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.MISSOES + "/options/filter", icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "missao.id")
    @Schema(
            description = "Ocorrencias ligadas a uma missao; EQUAL missaoId (FK) (demo).")
    private Integer missaoId;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 50, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "ocorridoEm")
    @Schema(
            description = "Janela do marco do fato; BETWEEN em ocorridoEm (demo).")
    private List<OffsetDateTime> ocorridoEmBetween;

    @UISchema(label = "Severidade (Incluir)", controlType = FieldControlType.INLINE_MULTISELECT, order = 60, icon = "emergency")
    @Filterable(operation = Filterable.FilterOperation.IN)
    @Schema(
            description = "Uniao de niveis; operacao IN em Severidade (multi) (demo).")
    private List<Severidade> severidadesIn;

    @UISchema(label = "Severidade (Excluir)", controlType = FieldControlType.INLINE_MULTISELECT, order = 70, icon = "emergency")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN)
    @Schema(
            description = "Excluir gravidades; NOT_IN (demo).")
    private List<Severidade> severidadesNotIn;

    @UISchema(label = "Ocorrido em (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 80, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "ocorridoEm")
    @Schema(
            description = "Dia civil do fato; ON_DATE (zona/ trunc do backend) (demo).")
    private LocalDate ocorridoEmOn;

    @UISchema(
            label = "Ocorrido em (Periodo Relativo)",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 90,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, icon = "event")
    @Schema(
            description = "Preset (ultima hora, hoje) conforme opcoes de UI; string serializada (demo).")
    private String ocorridoEmPreset;

    @UISchema(label = "Ocorrido em (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 95, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "ocorridoEm")
    @Schema(
            description = "Corte movel: ultimos N dias; IN_LAST_DAYS (demo).")
    private Integer ocorridoEmLastDays;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 100,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", icon = "calendar_today")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "danosCivis")
    @Schema(
            description = "Faixa de estimativa economica de dano; BETWEEN (moeda) (demo).")
    private List<BigDecimal> danosCivisBetween;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 110, icon = "health_and_safety")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "feridos")
    @Schema(
            description = "Contagem de feridos; BETWEEN (inteiro) (demo).")
    private List<Integer> feridosBetween;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 120, icon = "health_and_safety")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "mortos")
    @Schema(
            description = "Contagem de obitos; BETWEEN — dados sensiveis (demo).")
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
