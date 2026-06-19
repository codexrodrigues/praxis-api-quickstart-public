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
                + "Suporta recortes por missoes, estado operacional, ameaca, participantes, volume de eventos e recencia da linha do tempo.")
public class VwResumoMissoeFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Missão", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.MISSOES_MISSION_LOOKUP_OPTIONS,
            helpText = "Foca o resumo agregado de uma missão específica.", icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Missao especifica cujo resumo agregado deve ser localizado.")
    private Integer missaoId;

    @UISchema(label = "Mostrar missões", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_MULTISELECT, order = 15,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.MISSOES_MISSION_LOOKUP_OPTIONS,
            helpText = "Inclui no resumo apenas as missões selecionadas.", icon = "checklist")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "missaoId")
    @Schema(
            description = "Conjunto de missoes que deve compor o resumo agregado retornado.")
    private List<Integer> missaoIdsIn;

    @UISchema(label = "Título da missão", controlType = FieldControlType.INPUT, maxLength = 200, order = 20,
            helpText = "Busca por palavras-chave no título exibido no resumo.", icon = "title")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do titulo da missao exibido na vista de resumo.")
    private String titulo;

    @UISchema(label = "Status da missão", controlType = FieldControlType.INPUT, maxLength = 50, order = 30,
            helpText = "Busca pelo status textual projetado na visão resumida.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do estado de workflow projetado na linha de resumo.")
    private String status;

    @UISchema(label = "Prioridade", controlType = FieldControlType.INPUT, maxLength = 50, order = 40,
            helpText = "Busca pela prioridade textual exibida no resumo.", icon = "priority_high")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho da prioridade operacional exibida no resumo.")
    private String prioridade;

    @UISchema(label = "Local", controlType = FieldControlType.INPUT, maxLength = 200, order = 50,
            helpText = "Filtra pelo local ou cenário operacional da missão.", icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do local ou cenario operacional da missao.")
    private String local;

    @UISchema(label = "Ameaça principal", controlType = FieldControlType.INPUT, maxLength = 200, order = 60,
            helpText = "Busca pelo texto da ameaça principal associada à missão.", icon = "warning")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho da ameaca principal desnormalizada associada a missao.")
    private String ameaca;

    @UISchema(label = "Quantidade de heróis", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 70,
            helpText = "Filtra missões pela faixa de participantes no resumo.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "qtdHerois")
    @Schema(
            description = "Faixa de quantidade de participantes vinculados a missao no resumo.")
    private List<Long> qtdHeroisBetween;

    @UISchema(label = "Quantidade de eventos", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 80,
            helpText = "Filtra missões pela quantidade de eventos registrados na linha do tempo.", icon = "event_note")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "qtdEventos")
    @Schema(
            description = "Faixa de quantidade de eventos registrados na linha do tempo da missao.")
    private List<Long> qtdEventosBetween;

    @UISchema(label = "Período da primeira ação", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 90,
            helpText = "Filtra pela janela em que a primeira ação da missão ocorreu.", icon = "stacked_bar_chart")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "primeiraAcao")
    @Schema(
            description = "Janela temporal da primeira acao registrada na linha do tempo da missao.")
    private List<OffsetDateTime> primeiraAcaoBetween;

    @UISchema(label = "Período da última ação", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 100,
            helpText = "Filtra pela janela em que a atividade mais recente da missão ocorreu.", icon = "stacked_bar_chart")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "ultimaAcao")
    @Schema(
            description = "Janela temporal da atividade mais recente registrada para a missao.")
    private List<OffsetDateTime> ultimaAcaoBetween;

    @UISchema(label = "Primeira ação em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 110,
            helpText = "Mostra missões cuja primeira ação ocorreu em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "primeiraAcao")
    @Schema(
            description = "Dia civil usado para localizar missoes cuja primeira acao ocorreu nessa data.")
    private LocalDate primeiraAcaoOn;

    @UISchema(
            label = "Período rápido da primeira ação",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 120,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, helpText = "Aplica atalhos de tempo para a primeira ação, como hoje ou últimos períodos.", icon = "event")
    @Schema(
            description = "Atalho de periodo relativo aplicado a data da primeira acao, conforme opcoes publicadas para a UI.")
    private String primeiraAcaoPreset;

    @UISchema(label = "Primeira ação nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 125,
            helpText = "Informe quantos dias recentes devem ser considerados para a primeira ação.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "primeiraAcao")
    @Schema(
            description = "Janela relativa para localizar missoes cuja primeira acao ocorreu nos ultimos N dias.")
    private Integer primeiraAcaoLastDays;

    @UISchema(label = "Última ação em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 130,
            helpText = "Mostra missões cuja atividade mais recente ocorreu em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "ultimaAcao")
    @Schema(
            description = "Dia civil usado para localizar missoes cuja ultima atividade ocorreu nessa data.")
    private LocalDate ultimaAcaoOn;

    @UISchema(
            label = "Período rápido da última ação",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 140,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, helpText = "Aplica atalhos de tempo para a última ação, como hoje ou últimos períodos.", icon = "event")
    @Schema(
            description = "Atalho de periodo relativo aplicado a data da ultima acao da missao.")
    private String ultimaAcaoPreset;

    @UISchema(label = "Última ação nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 145,
            helpText = "Informe quantos dias recentes devem ser considerados para a última ação.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "ultimaAcao")
    @Schema(
            description = "Janela relativa para localizar missoes com atividade registrada nos ultimos N dias.")
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
