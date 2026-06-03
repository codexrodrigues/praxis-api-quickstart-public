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
        description = "Criterios de busca em mencoes de midia (nao e a peca a editar; pode ser lista para monitorizacao/ crisis). "
                + "Inclui sentimento, veiculo, janela temporal; GenericFilter / POST /filter. Sentimento: NEG, NEU, POS (enum) (demo).")
public class MencoesMidiaFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Herói Mencionado", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Filtrar menções ligadas a um colaborador.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Citar apenas um heroi; EQUAL (FK) (demo).")
    private Integer funcionarioId;

    @UISchema(label = "Veículo de Mídia", controlType = FieldControlType.INPUT, maxLength = 120, order = 20, helpText = "Buscar pelo nome do veículo de mídia.", icon = "directions_car")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Outlets (jornal, site, programa); LIKE (demo).")
    private String veiculo;

    @UISchema(label = "Sentimento", controlType = FieldControlType.INLINE_SENTIMENT, order = 30, helpText = "Filtrar pelo sentimento geral da menção.", icon = "mood")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Filtro simples; EQUAL a um unico Sentimento (demo).")
    private Sentimento sentimento;

    @UISchema(label = "Período de Publicação", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 40, helpText = "Buscar menções publicadas em um intervalo de tempo.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "publicadoEm")
    @Schema(
            description = "Janela de publicacao (instante com offset); BETWEEN em publicadoEm (demo).")
    private List<OffsetDateTime> publicadoEmBetween;

    @UISchema(label = "Sentimento (Incluir)", controlType = FieldControlType.INLINE_MULTISELECT, order = 50, helpText = "Filtrar menções que contenham qualquer um destes sentimentos.", icon = "mood")
    @Filterable(operation = Filterable.FilterOperation.IN)
    @Schema(
            description = "Uniao: quaisquer mencoes cujo sentimento pertenca a esta lista; operacao IN (multi-select) (demo).")
    private List<Sentimento> sentimentoIn;

    @UISchema(label = "Sentimento (Excluir)", controlType = FieldControlType.INLINE_MULTISELECT, order = 60, helpText = "Excluir menções que contenham estes sentimentos.", icon = "mood")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN)
    @Schema(
            description = "Rejeitar resultados cujo sentimento caia na lista; operacao NOT_IN (demo).")
    private List<Sentimento> sentimentoNotIn;

    @UISchema(label = "Publicado em (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 70, helpText = "Buscar menções publicadas em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "publicadoEm")
    @Schema(
            description = "Apenas pecas cujo instante cai no dia local; ON_DATE (zona/ trunc depende de backend) (demo).")
    private LocalDate publicadoEmOn;

    @UISchema(
            label = "Publicado em (Periodo Relativo)",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 80,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, helpText = "Filtrar usando períodos relativos (ex: última semana).", icon = "event")
    @Schema(
            description = "Preset (ex.: ultima semana) conforme opcoes de UI; string serializada, interpretacao do motor de filtro (demo).")
    private String publicadoEmPreset;

    @UISchema(label = "Publicado em (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 85, helpText = "Buscar menções publicadas nos últimos N dias.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "publicadoEm")
    @Schema(
            description = "Recencia: publicado nos ultimos N dias; IN_LAST_DAYS (demo).")
    private Integer publicadoEmLastDays;

    @UISchema(label = "URL ou Domínio", controlType = FieldControlType.INPUT, maxLength = 500, order = 90, helpText = "Buscar por fragmentos de URL ou domínio.", icon = "link")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Fragmento de URL (dominio, path); LIKE; atencao a dados externos (demo).")
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

