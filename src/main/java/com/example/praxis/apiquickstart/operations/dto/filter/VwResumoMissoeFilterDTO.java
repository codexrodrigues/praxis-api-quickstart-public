package com.example.praxis.apiquickstart.operations.dto.filter;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.dto.filter.support.QuickstartRelativePeriodUiOptions;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "VwResumoMissoeFilterDTO",
        description = "Criterios de busca na vista VwResumoMissoe (resumo agregado; nao e a missao em edicao). "
                + "Distinto de MissaoFilterDTO (entidade). Suporta multiplas missoes, texto e janelas temporais de acoes. GenericFilter / POST /filter (demo).")
public class VwResumoMissoeFilterDTO implements GenericFilterDTO {
    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.MISSOES + "/options/filter", icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Uma unica missao no resumo; EQUAL (FK) (demo).")
    private Integer missaoId;

    @UISchema(label = "Missões (Incluir)", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_MULTISELECT, order = 15,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.MISSOES + "/options/filter", icon = "checklist")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "missaoId")
    @Schema(
            description = "Subconjunto de missoes; operacao IN (multi-select) (demo).")
    private List<Integer> missaoIdsIn;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 20, icon = "title")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Titulo exibido na vista; LIKE (demo).")
    private String titulo;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 50, order = 30, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Estado de workflow (string da projecao); LIKE (demo).")
    private String status;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 50, order = 40, icon = "priority_high")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Prioridade exibida; LIKE (demo).")
    private String prioridade;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 50, icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Cenario operacional; LIKE (demo).")
    private String local;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 60, icon = "warning")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Ameaca principal (texto denormalizado); LIKE (demo).")
    private String ameaca;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 70, icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "qtdHerois")
    @Schema(
            description = "Faixa de headcount de herois; BETWEEN (demo).")
    private List<Long> qtdHeroisBetween;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 80, icon = "event_note")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "qtdEventos")
    @Schema(
            description = "Faixa de volume de eventos; BETWEEN (carga operacional) (demo).")
    private List<Long> qtdEventosBetween;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 90, icon = "stacked_bar_chart")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "primeiraAcao")
    @Schema(
            description = "Janela da primeira acao na timeline; BETWEEN (demo).")
    private List<OffsetDateTime> primeiraAcaoBetween;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 100, icon = "stacked_bar_chart")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "ultimaAcao")
    @Schema(
            description = "Janela da ultima acao; BETWEEN (staleness) (demo).")
    private List<OffsetDateTime> ultimaAcaoBetween;

    @UISchema(label = "Primeira Ação (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 110, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "primeiraAcao")
    @Schema(
            description = "Primeira acao neste dia civil; ON_DATE (demo).")
    private LocalDate primeiraAcaoOn;

    @UISchema(
            label = "Primeira Ação (Periodo Relativo)",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 120,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, icon = "event")
    @Schema(
            description = "Preset de periodo para primeira acao; string+ UI options (demo).")
    private String primeiraAcaoPreset;

    @UISchema(label = "Primeira Ação (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 125, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "primeiraAcao")
    @Schema(
            description = "Missao com primeira acao recente; IN_LAST_DAYS (demo).")
    private Integer primeiraAcaoLastDays;

    @UISchema(label = "Última Ação (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 130, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "ultimaAcao")
    @Schema(
            description = "Ultima acao neste dia; ON_DATE (demo).")
    private LocalDate ultimaAcaoOn;

    @UISchema(
            label = "Última Ação (Periodo Relativo)",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 140,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, icon = "event")
    @Schema(
            description = "Preset relativo para ultima acao; string serializada (demo).")
    private String ultimaAcaoPreset;

    @UISchema(label = "Última Ação (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 145, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "ultimaAcao")
    @Schema(
            description = "Atividade recente; ultima acao em N dias; IN_LAST_DAYS (demo).")
    private Integer ultimaAcaoLastDays;

    public Integer getMissaoId() { return missaoId; }
    public void setMissaoId(Integer missaoId) { this.missaoId = missaoId; }
    public List<Integer> getMissaoIdsIn() { return missaoIdsIn; }
    public void setMissaoIdsIn(List<Integer> missaoIdsIn) { this.missaoIdsIn = missaoIdsIn; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public String getAmeaca() { return ameaca; }
    public void setAmeaca(String ameaca) { this.ameaca = ameaca; }
    public List<Long> getQtdHeroisBetween() { return qtdHeroisBetween; }
    public void setQtdHeroisBetween(List<Long> qtdHeroisBetween) { this.qtdHeroisBetween = qtdHeroisBetween; }
    public List<Long> getQtdEventosBetween() { return qtdEventosBetween; }
    public void setQtdEventosBetween(List<Long> qtdEventosBetween) { this.qtdEventosBetween = qtdEventosBetween; }
    public List<OffsetDateTime> getPrimeiraAcaoBetween() { return primeiraAcaoBetween; }
    public void setPrimeiraAcaoBetween(List<OffsetDateTime> primeiraAcaoBetween) { this.primeiraAcaoBetween = primeiraAcaoBetween; }
    public List<OffsetDateTime> getUltimaAcaoBetween() { return ultimaAcaoBetween; }
    public void setUltimaAcaoBetween(List<OffsetDateTime> ultimaAcaoBetween) { this.ultimaAcaoBetween = ultimaAcaoBetween; }
    public LocalDate getPrimeiraAcaoOn() { return primeiraAcaoOn; }
    public void setPrimeiraAcaoOn(LocalDate primeiraAcaoOn) { this.primeiraAcaoOn = primeiraAcaoOn; }
    public String getPrimeiraAcaoPreset() { return primeiraAcaoPreset; }
    public void setPrimeiraAcaoPreset(String primeiraAcaoPreset) { this.primeiraAcaoPreset = primeiraAcaoPreset; }
    public Integer getPrimeiraAcaoLastDays() { return primeiraAcaoLastDays; }
    public void setPrimeiraAcaoLastDays(Integer primeiraAcaoLastDays) { this.primeiraAcaoLastDays = primeiraAcaoLastDays; }
    public LocalDate getUltimaAcaoOn() { return ultimaAcaoOn; }
    public void setUltimaAcaoOn(LocalDate ultimaAcaoOn) { this.ultimaAcaoOn = ultimaAcaoOn; }
    public String getUltimaAcaoPreset() { return ultimaAcaoPreset; }
    public void setUltimaAcaoPreset(String ultimaAcaoPreset) { this.ultimaAcaoPreset = ultimaAcaoPreset; }
    public Integer getUltimaAcaoLastDays() { return ultimaAcaoLastDays; }
    public void setUltimaAcaoLastDays(Integer ultimaAcaoLastDays) { this.ultimaAcaoLastDays = ultimaAcaoLastDays; }
}
