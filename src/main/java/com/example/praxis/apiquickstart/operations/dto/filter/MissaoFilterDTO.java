package com.example.praxis.apiquickstart.operations.dto.filter;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.dto.filter.support.QuickstartRelativePeriodUiOptions;
import com.example.praxis.apiquickstart.operations.enums.MissaoPrioridade;
import com.example.praxis.apiquickstart.operations.enums.MissaoStatus;
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
        name = "MissaoFilterDTO",
        description = "Criterios de busca no catalogo de missoes (ameaca, janelas previstas e reais, status e prioridade); nao e a entidade a editar so com filtrar. "
                + "GenericFilter / POST /filter (demo).")
public class MissaoFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 10, icon = "title")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Designacao de campanha/operacao; LIKE titulo (demo).")
    private String titulo;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 4000, order = 20, icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Finalidade tatica/ narrativa; LIKE objetivo (demo).")
    private String objetivo;

    @UISchema(controlType = FieldControlType.SELECT, order = 30, icon = "priority_high")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Um unico grau; EQUAL MissaoPrioridade (demo).")
    private MissaoPrioridade prioridade;

    @UISchema(controlType = FieldControlType.SELECT, order = 40, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Uma fase; EQUAL MissaoStatus (demo).")
    private MissaoStatus status;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 50, icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Teatro ou base; LIKE local (demo).")
    private String local;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 60,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.RiskIntelligence.AMEACAS + "/options/filter", icon = "warning")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "ameaca.id")
    @Schema(
            description = "Ameaca a neutralizar; EQUAL ameacaId (demo).")
    private Integer ameacaId;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 70, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "inicioPrev")
    @Schema(
            description = "Janela de partida prevista; BETWEEN inicioPrev (demo).")
    private List<OffsetDateTime> inicioPrevBetween;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 80, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "fimPrev")
    @Schema(
            description = "Janela de conclusao prevista; BETWEEN fimPrev (demo).")
    private List<OffsetDateTime> fimPrevBetween;

    @UISchema(label = "Status (Incluir)", controlType = FieldControlType.INLINE_MULTISELECT, order = 90, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.IN)
    @Schema(
            description = "Uniao de fases; IN MissaoStatus (demo).")
    private List<MissaoStatus> statusIn;

    @UISchema(label = "Status (Excluir)", controlType = FieldControlType.INLINE_MULTISELECT, order = 100, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN)
    @Schema(
            description = "Excluir fases; NOT_IN MissaoStatus (demo).")
    private List<MissaoStatus> statusNotIn;

    @UISchema(label = "Prioridade (Incluir)", controlType = FieldControlType.INLINE_MULTISELECT, order = 110, icon = "priority_high")
    @Filterable(operation = Filterable.FilterOperation.IN)
    @Schema(
            description = "Uniao de prioridades; IN (demo).")
    private List<MissaoPrioridade> prioridadeIn;

    @UISchema(label = "Prioridade (Excluir)", controlType = FieldControlType.INLINE_MULTISELECT, order = 120, icon = "priority_high")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN)
    @Schema(
            description = "Excluir prioridades; NOT_IN (demo).")
    private List<MissaoPrioridade> prioridadeNotIn;

    @UISchema(label = "Início Previsto (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 130, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "inicioPrev")
    @Schema(
            description = "Partida prevista neste dia civil; ON_DATE inicioPrev (demo).")
    private LocalDate inicioPrevOn;

    @UISchema(
            label = "Início Previsto (Periodo Relativo)",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 140,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, icon = "event")
    @Schema(
            description = "Intervalo relativo (UI) sobre inicioPrev; string de preset, complementa BETWEEN/ON_DATE (demo).")
    private String inicioPrevPreset;

    @UISchema(label = "Início Previsto (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 145, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "inicioPrev")
    @Schema(
            description = "Partida prevista nos ultimos N dias; IN_LAST_DAYS inicioPrev (demo).")
    private Integer inicioPrevLastDays;

    @UISchema(label = "Fim Previsto (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 150, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "fimPrev")
    @Schema(
            description = "Conclusao prevista neste dia; ON_DATE fimPrev (demo).")
    private LocalDate fimPrevOn;

    @UISchema(
            label = "Fim Previsto (Periodo Relativo)",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 160,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, icon = "event")
    @Schema(
            description = "Intervalo relativo (UI) sobre fimPrev; string de preset (demo).")
    private String fimPrevPreset;

    @UISchema(label = "Fim Previsto (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 165, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "fimPrev")
    @Schema(
            description = "Fim previsto nos ultimos N dias; IN_LAST_DAYS fimPrev (demo).")
    private Integer fimPrevLastDays;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 170, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "inicioReal")
    @Schema(
            description = "Janela de inicio executado; BETWEEN inicioReal (demo).")
    private List<OffsetDateTime> inicioRealBetween;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 180, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "fimReal")
    @Schema(
            description = "Janela de fim executado; BETWEEN fimReal (demo).")
    private List<OffsetDateTime> fimRealBetween;

    @UISchema(label = "Início Real (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 190, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "inicioReal")
    @Schema(
            description = "Inicio efetivo neste dia; ON_DATE inicioReal (demo).")
    private LocalDate inicioRealOn;

    @UISchema(
            label = "Início Real (Periodo Relativo)",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 200,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, icon = "event")
    @Schema(
            description = "Intervalo relativo (UI) sobre inicioReal; string de preset (demo).")
    private String inicioRealPreset;

    @UISchema(label = "Início Real (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 205, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "inicioReal")
    @Schema(
            description = "Inicio real nos ultimos N dias; IN_LAST_DAYS inicioReal (demo).")
    private Integer inicioRealLastDays;

    @UISchema(label = "Fim Real (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 210, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "fimReal")
    @Schema(
            description = "Fim executado neste dia; ON_DATE fimReal (demo).")
    private LocalDate fimRealOn;

    @UISchema(
            label = "Fim Real (Periodo Relativo)",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 220,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, icon = "event")
    @Schema(
            description = "Intervalo relativo (UI) sobre fimReal; string de preset (demo).")
    private String fimRealPreset;

    @UISchema(label = "Fim Real (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 225, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "fimReal")
    @Schema(
            description = "Fim real nos ultimos N dias; IN_LAST_DAYS fimReal (demo).")
    private Integer fimRealLastDays;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public MissaoPrioridade getPrioridade() { return prioridade; }
    public void setPrioridade(MissaoPrioridade prioridade) { this.prioridade = prioridade; }
    public MissaoStatus getStatus() { return status; }
    public void setStatus(MissaoStatus status) { this.status = status; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public Integer getAmeacaId() { return ameacaId; }
    public void setAmeacaId(Integer ameacaId) { this.ameacaId = ameacaId; }
    public List<OffsetDateTime> getInicioPrevBetween() { return inicioPrevBetween; }
    public void setInicioPrevBetween(List<OffsetDateTime> inicioPrevBetween) { this.inicioPrevBetween = inicioPrevBetween; }
    public List<OffsetDateTime> getFimPrevBetween() { return fimPrevBetween; }
    public void setFimPrevBetween(List<OffsetDateTime> fimPrevBetween) { this.fimPrevBetween = fimPrevBetween; }

    public List<MissaoStatus> getStatusIn() { return statusIn; }
    public void setStatusIn(List<MissaoStatus> statusIn) { this.statusIn = statusIn; }
    public List<MissaoStatus> getStatusNotIn() { return statusNotIn; }
    public void setStatusNotIn(List<MissaoStatus> statusNotIn) { this.statusNotIn = statusNotIn; }
    public List<MissaoPrioridade> getPrioridadeIn() { return prioridadeIn; }
    public void setPrioridadeIn(List<MissaoPrioridade> prioridadeIn) { this.prioridadeIn = prioridadeIn; }
    public List<MissaoPrioridade> getPrioridadeNotIn() { return prioridadeNotIn; }
    public void setPrioridadeNotIn(List<MissaoPrioridade> prioridadeNotIn) { this.prioridadeNotIn = prioridadeNotIn; }
    public LocalDate getInicioPrevOn() { return inicioPrevOn; }
    public void setInicioPrevOn(LocalDate inicioPrevOn) { this.inicioPrevOn = inicioPrevOn; }
    public String getInicioPrevPreset() { return inicioPrevPreset; }
    public void setInicioPrevPreset(String inicioPrevPreset) { this.inicioPrevPreset = inicioPrevPreset; }
    public Integer getInicioPrevLastDays() { return inicioPrevLastDays; }
    public void setInicioPrevLastDays(Integer inicioPrevLastDays) { this.inicioPrevLastDays = inicioPrevLastDays; }
    public LocalDate getFimPrevOn() { return fimPrevOn; }
    public void setFimPrevOn(LocalDate fimPrevOn) { this.fimPrevOn = fimPrevOn; }
    public String getFimPrevPreset() { return fimPrevPreset; }
    public void setFimPrevPreset(String fimPrevPreset) { this.fimPrevPreset = fimPrevPreset; }
    public Integer getFimPrevLastDays() { return fimPrevLastDays; }
    public void setFimPrevLastDays(Integer fimPrevLastDays) { this.fimPrevLastDays = fimPrevLastDays; }
    public List<OffsetDateTime> getInicioRealBetween() { return inicioRealBetween; }
    public void setInicioRealBetween(List<OffsetDateTime> inicioRealBetween) { this.inicioRealBetween = inicioRealBetween; }
    public List<OffsetDateTime> getFimRealBetween() { return fimRealBetween; }
    public void setFimRealBetween(List<OffsetDateTime> fimRealBetween) { this.fimRealBetween = fimRealBetween; }
    public LocalDate getInicioRealOn() { return inicioRealOn; }
    public void setInicioRealOn(LocalDate inicioRealOn) { this.inicioRealOn = inicioRealOn; }
    public String getInicioRealPreset() { return inicioRealPreset; }
    public void setInicioRealPreset(String inicioRealPreset) { this.inicioRealPreset = inicioRealPreset; }
    public Integer getInicioRealLastDays() { return inicioRealLastDays; }
    public void setInicioRealLastDays(Integer inicioRealLastDays) { this.inicioRealLastDays = inicioRealLastDays; }
    public LocalDate getFimRealOn() { return fimRealOn; }
    public void setFimRealOn(LocalDate fimRealOn) { this.fimRealOn = fimRealOn; }
    public String getFimRealPreset() { return fimRealPreset; }
    public void setFimRealPreset(String fimRealPreset) { this.fimRealPreset = fimRealPreset; }
    public Integer getFimRealLastDays() { return fimRealLastDays; }
    public void setFimRealLastDays(Integer fimRealLastDays) { this.fimRealLastDays = fimRealLastDays; }
}



