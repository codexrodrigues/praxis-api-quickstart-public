package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.dto.filter.support.QuickstartRelativePeriodUiOptions;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import com.example.praxis.apiquickstart.hr.enums.Sentimento;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "MencoesMidiaFilterDTO",
        description = "Criterios de busca em mencoes de midia associadas a colaboradores. "
                + "Apoia monitoramento reputacional por pessoa, veiculo, sentimento, janela de publicacao e origem da URL.")
public class MencoesMidiaFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Herói Mencionado", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, helpText = "Filtrar menções ligadas a um colaborador.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador mencionado pela publicacao ou peca de midia monitorada.")
    private Integer funcionarioId;

    @UISchema(label = "Veículo de Mídia", controlType = FieldControlType.INPUT, maxLength = 120, order = 20, helpText = "Buscar pelo nome do veículo de mídia.", icon = "directions_car")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome do veiculo, canal, programa ou publicador responsavel pela mencao.")
    private String veiculo;

    @UISchema(label = "Sentimento", controlType = FieldControlType.INLINE_SENTIMENT, order = 30, helpText = "Filtrar pelo sentimento geral da menção.", icon = "mood")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Classificacao de sentimento atribuida a mencao, usada para separar impacto positivo, neutro ou negativo.")
    private Sentimento sentimento;

    @UISchema(label = "Período de Publicação", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 40, helpText = "Buscar menções publicadas em um intervalo de tempo.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "publicadoEm")
    @Schema(
            description = "Janela de publicacao da mencao, preservando o instante com offset para analise temporal.")
    private List<OffsetDateTime> publicadoEmBetween;

    @UISchema(label = "Mostrar sentimentos", controlType = FieldControlType.INLINE_MULTISELECT, order = 50, helpText = "Inclui menções com qualquer um dos sentimentos selecionados.", icon = "mood")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "sentimento")
    @Schema(
            description = "Conjunto de sentimentos aceitos para compor o resultado de monitoramento.")
    private List<Sentimento> sentimentoIn;

    @UISchema(label = "Ocultar sentimentos", controlType = FieldControlType.INLINE_MULTISELECT, order = 60, helpText = "Remove do resultado menções com os sentimentos selecionados.", icon = "mood")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN, relation = "sentimento")
    @Schema(
            description = "Conjunto de sentimentos que devem ser excluidos do resultado de monitoramento.")
    private List<Sentimento> sentimentoNotIn;

    @UISchema(label = "Publicado em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 70, helpText = "Busca menções publicadas em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "publicadoEm")
    @Schema(
            description = "Dia civil usado para localizar mencoes publicadas em uma data especifica.")
    private LocalDate publicadoEmOn;

    @UISchema(
            label = "Período rápido de publicação",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 80,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, helpText = "Aplica atalhos de tempo, como hoje, esta semana ou últimos períodos configurados.", icon = "event")
    @Schema(
            description = "Atalho de periodo relativo aplicado a data de publicacao da mencao.")
    private String publicadoEmPreset;

    @UISchema(label = "Publicado nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 85, helpText = "Informe quantos dias recentes devem ser considerados na busca.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "publicadoEm")
    @Schema(
            description = "Janela relativa para localizar mencoes publicadas nos ultimos N dias.")
    private Integer publicadoEmLastDays;

    @UISchema(label = "URL ou Domínio", controlType = FieldControlType.INPUT, maxLength = 500, order = 90, helpText = "Buscar por fragmentos de URL ou domínio.", icon = "link")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho de dominio, caminho ou URL de origem usado para rastrear a fonte externa da mencao.")
    private String url;

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getVeiculo() { return veiculo; }
    public void setVeiculo(String veiculo) { this.veiculo = veiculo; }
    public Sentimento getSentimento() { return sentimento; }
    public void setSentimento(Sentimento sentimento) { this.sentimento = sentimento; }
    public List<OffsetDateTime> getPublicadoEmBetween() { return publicadoEmBetween; }
    public void setPublicadoEmBetween(List<OffsetDateTime> publicadoEmBetween) { this.publicadoEmBetween = publicadoEmBetween; }
    public List<Sentimento> getSentimentoIn() { return sentimentoIn; }
    public void setSentimentoIn(List<Sentimento> sentimentoIn) { this.sentimentoIn = sentimentoIn; }
    public List<Sentimento> getSentimentoNotIn() { return sentimentoNotIn; }
    public void setSentimentoNotIn(List<Sentimento> sentimentoNotIn) { this.sentimentoNotIn = sentimentoNotIn; }
    public LocalDate getPublicadoEmOn() { return publicadoEmOn; }
    public void setPublicadoEmOn(LocalDate publicadoEmOn) { this.publicadoEmOn = publicadoEmOn; }
    public String getPublicadoEmPreset() { return publicadoEmPreset; }
    public void setPublicadoEmPreset(String publicadoEmPreset) { this.publicadoEmPreset = publicadoEmPreset; }
    public Integer getPublicadoEmLastDays() { return publicadoEmLastDays; }
    public void setPublicadoEmLastDays(Integer publicadoEmLastDays) { this.publicadoEmLastDays = publicadoEmLastDays; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}

