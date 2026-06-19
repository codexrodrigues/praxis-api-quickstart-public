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
                + "Usado por consoles operacionais e assistentes para localizar missoes por ciclo de vida, agenda, prioridade, teatro e ameaca relacionada.")
public class MissaoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Título da missão", controlType = FieldControlType.INPUT, maxLength = 200, order = 10,
            helpText = "Digite parte do nome da missão para localizar campanhas específicas.", icon = "title")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome usado para localizar uma campanha ou operação no catálogo de missões.")
    private String titulo;

    @UISchema(label = "Objetivo da missão", controlType = FieldControlType.INPUT, maxLength = 4000, order = 20,
            helpText = "Busque por palavras presentes na finalidade ou narrativa operacional.", icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho da finalidade operacional ou narrativa esperada para a missão.")
    private String objetivo;

    @UISchema(label = "Prioridade", controlType = FieldControlType.SELECT, order = 30,
            helpText = "Filtra por uma única prioridade operacional.", icon = "priority_high")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Grau de urgência atribuído à missão para orientar triagem e ordenação operacional.")
    private MissaoPrioridade prioridade;

    @UISchema(label = "Status", controlType = FieldControlType.SELECT, order = 40,
            helpText = "Filtra por uma única fase do ciclo de vida da missão.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Fase atual do ciclo de vida da missão, como planejada, em andamento, pausada, concluída ou falha.")
    private MissaoStatus status;

    @UISchema(label = "Local", controlType = FieldControlType.INPUT, maxLength = 200, order = 50,
            helpText = "Digite parte do teatro operacional, base ou região da missão.", icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do teatro operacional, base ou região associada à missão.")
    private String local;

    @UISchema(label = "Ameaça", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 60,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.RiskIntelligence.AMEACAS_THREAT_LOOKUP_OPTIONS,
            helpText = "Selecione a ameaça vinculada às missões que deseja analisar.", icon = "warning")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "ameaca.id")
    @Schema(
            description = "Ameaça principal que a missão deve conter, investigar ou neutralizar.")
    private Integer ameacaId;

    @UISchema(label = "Período previsto de início", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 70,
            helpText = "Informe uma janela para encontrar missões previstas para começar nesse período.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "inicioPrev")
    @Schema(
            description = "Intervalo de datas em que a missão estava prevista para começar.")
    private List<OffsetDateTime> inicioPrevBetween;

    @UISchema(label = "Período previsto de término", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 80,
            helpText = "Informe uma janela para encontrar missões previstas para terminar nesse período.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "fimPrev")
    @Schema(
            description = "Intervalo de datas em que a missão estava prevista para terminar.")
    private List<OffsetDateTime> fimPrevBetween;

    @UISchema(label = "Mostrar status", controlType = FieldControlType.INLINE_MULTISELECT, order = 90,
            helpText = "Mostra apenas missões nos status selecionados.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "status")
    @Schema(
            description = "Conjunto de fases que devem aparecer no resultado da busca.")
    private List<MissaoStatus> statusIn;

    @UISchema(label = "Ocultar status", controlType = FieldControlType.INLINE_MULTISELECT, order = 100,
            helpText = "Remove do resultado as missões nos status selecionados.", icon = "toggle_off")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN, relation = "status")
    @Schema(
            description = "Conjunto de fases que devem ser removidas do resultado da busca.")
    private List<MissaoStatus> statusNotIn;

    @UISchema(label = "Mostrar prioridades", controlType = FieldControlType.INLINE_MULTISELECT, order = 110,
            helpText = "Mostra apenas missões com as prioridades selecionadas.", icon = "priority_high")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "prioridade")
    @Schema(
            description = "Conjunto de prioridades que devem aparecer no resultado da busca.")
    private List<MissaoPrioridade> prioridadeIn;

    @UISchema(label = "Ocultar prioridades", controlType = FieldControlType.INLINE_MULTISELECT, order = 120,
            helpText = "Remove do resultado as missões com as prioridades selecionadas.", icon = "low_priority")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN, relation = "prioridade")
    @Schema(
            description = "Conjunto de prioridades que devem ser removidas do resultado da busca.")
    private List<MissaoPrioridade> prioridadeNotIn;

    @UISchema(label = "Início previsto em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 130,
            helpText = "Escolha um dia exato de início previsto.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "inicioPrev")
    @Schema(
            description = "Dia civil exato em que a missão estava prevista para começar.")
    private LocalDate inicioPrevOn;

    @UISchema(
            label = "Início previsto rápido",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 140,
            helpText = "Use atalhos como hoje, este mês ou últimos dias para o início previsto.",
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, icon = "event")
    @Schema(
            description = "Atalho de período relativo aplicado à data prevista de início, como mês atual ou últimos dias.")
    private String inicioPrevPreset;

    @UISchema(label = "Início previsto nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 145,
            helpText = "Informe quantos dias recentes devem ser considerados para o início previsto.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "inicioPrev")
    @Schema(
            description = "Quantidade de dias recentes usada para localizar missões com início previsto nesse recorte.")
    private Integer inicioPrevLastDays;

    @UISchema(label = "Término previsto em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 150,
            helpText = "Escolha um dia exato de término previsto.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "fimPrev")
    @Schema(
            description = "Dia civil exato em que a missão estava prevista para terminar.")
    private LocalDate fimPrevOn;

    @UISchema(
            label = "Término previsto rápido",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 160,
            helpText = "Use atalhos de período para o término previsto da missão.",
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, icon = "event")
    @Schema(
            description = "Atalho de período relativo aplicado à data prevista de término.")
    private String fimPrevPreset;

    @UISchema(label = "Término previsto nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 165,
            helpText = "Informe quantos dias recentes devem ser considerados para o término previsto.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "fimPrev")
    @Schema(
            description = "Quantidade de dias recentes usada para localizar missões com término previsto nesse recorte.")
    private Integer fimPrevLastDays;

    @UISchema(label = "Período real de início", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 170,
            helpText = "Informe uma janela para encontrar missões que começaram nesse período.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "inicioReal")
    @Schema(
            description = "Intervalo de datas em que a execução da missão realmente começou.")
    private List<OffsetDateTime> inicioRealBetween;

    @UISchema(label = "Período real de término", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 180,
            helpText = "Informe uma janela para encontrar missões concluídas nesse período.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "fimReal")
    @Schema(
            description = "Intervalo de datas em que a execução da missão realmente terminou.")
    private List<OffsetDateTime> fimRealBetween;

    @UISchema(label = "Início real em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 190,
            helpText = "Escolha o dia exato em que a execução começou.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "inicioReal")
    @Schema(
            description = "Dia civil exato em que a execução da missão começou.")
    private LocalDate inicioRealOn;

    @UISchema(
            label = "Início real rápido",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 200,
            helpText = "Use atalhos de período para o início real da execução.",
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, icon = "event")
    @Schema(
            description = "Atalho de período relativo aplicado à data real de início da missão.")
    private String inicioRealPreset;

    @UISchema(label = "Início real nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 205,
            helpText = "Informe quantos dias recentes devem ser considerados para o início real.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "inicioReal")
    @Schema(
            description = "Quantidade de dias recentes usada para localizar missões iniciadas nesse recorte.")
    private Integer inicioRealLastDays;

    @UISchema(label = "Término real em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 210,
            helpText = "Escolha o dia exato em que a execução terminou.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "fimReal")
    @Schema(
            description = "Dia civil exato em que a execução da missão terminou.")
    private LocalDate fimRealOn;

    @UISchema(
            label = "Término real rápido",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 220,
            helpText = "Use atalhos de período para o término real da execução.",
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, icon = "event")
    @Schema(
            description = "Atalho de período relativo aplicado à data real de término da missão.")
    private String fimRealPreset;

    @UISchema(label = "Término real nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 225,
            helpText = "Informe quantos dias recentes devem ser considerados para o término real.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "fimReal")
    @Schema(
            description = "Quantidade de dias recentes usada para localizar missões finalizadas nesse recorte.")
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



