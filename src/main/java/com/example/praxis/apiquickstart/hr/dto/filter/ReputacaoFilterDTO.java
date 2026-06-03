package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.dto.filter.support.QuickstartRelativePeriodUiOptions;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "ReputacaoFilterDTO",
        description = "Criterios de busca na linha de snapshot de Reputacao (nao e o score persistido a editar so com filtro). "
                + "Afinidade por funcionario, faixas de score e frescor; GenericFilter / POST /filter (demo).")
public class ReputacaoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Herói", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Filtrar a reputação de um colaborador.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Apenas reputacao de um colaborador; EQUAL (FK) (demo).")
    private Integer funcionarioId;

    @UISchema(label = "Score de Mídia", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 20, helpText = "Buscar por faixa de aprovação pública.", icon = "campaign")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "scorePublico")
    @Schema(
            description = "Faixa de indice de midia/ visibilidade; BETWEEN (demo).")
    private List<Integer> scorePublicoBetween;

    @UISchema(label = "Score Governamental", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 30, helpText = "Buscar por faixa de alinhamento governamental.", icon = "gavel")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "scoreGovernamental")
    @Schema(
            description = "Faixa de alinhamento governamental; BETWEEN (demo).")
    private List<Integer> scoreGovernamentalBetween;

    @UISchema(label = "Período de Atualização", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 40, helpText = "Filtrar por intervalo de data de atualização.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "atualizadoEm")
    @Schema(
            description = "Frescor do snapshot (ultima recomputacao); BETWEEN em OffsetDateTime (demo).")
    private List<OffsetDateTime> atualizadoEmBetween;

    @UISchema(label = "Atualizado em (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 50, helpText = "Buscar por data de atualização específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "atualizadoEm")
    @Schema(
            description = "Mudancas cujo timestamp cai neste dia civil; ON_DATE (zona/ trunc depende de backend) (demo).")
    private LocalDate atualizadoEmOn;

    @UISchema(
            label = "Atualizado em (Periodo Relativo)",
            controlType = FieldControlType.INLINE_RELATIVE_PERIOD,
            order = 60,
            extraProperties = {
                    @ExtensionProperty(name = "relativePeriodOptions", value = QuickstartRelativePeriodUiOptions.DEFAULT_OPTIONS_JSON)
            }, helpText = "Filtrar usando períodos de atualização pré-definidos.", icon = "event")
    @Schema(
            description = "Preset de periodo (ex.: este mes) serializado; interpretacao conforme opcoes em extraProperties/ UI (demo).")
    private String atualizadoEmPreset;

    @UISchema(label = "Atualizado em (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 65, helpText = "Buscar pontuações atualizadas nos últimos N dias.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "atualizadoEm")
    @Schema(
            description = "Recencia: atualizado nos ultimos N dias; IN_LAST_DAYS (GenericFilter) (demo).")
    private Integer atualizadoEmLastDays;

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public List<Integer> getScorePublicoBetween() { return scorePublicoBetween; }
    public void setScorePublicoBetween(List<Integer> scorePublicoBetween) { this.scorePublicoBetween = scorePublicoBetween; }
    public List<Integer> getScoreGovernamentalBetween() { return scoreGovernamentalBetween; }
    public void setScoreGovernamentalBetween(List<Integer> scoreGovernamentalBetween) { this.scoreGovernamentalBetween = scoreGovernamentalBetween; }
    public List<OffsetDateTime> getAtualizadoEmBetween() { return atualizadoEmBetween; }
    public void setAtualizadoEmBetween(List<OffsetDateTime> atualizadoEmBetween) { this.atualizadoEmBetween = atualizadoEmBetween; }
    public LocalDate getAtualizadoEmOn() { return atualizadoEmOn; }
    public void setAtualizadoEmOn(LocalDate atualizadoEmOn) { this.atualizadoEmOn = atualizadoEmOn; }
    public String getAtualizadoEmPreset() { return atualizadoEmPreset; }
    public void setAtualizadoEmPreset(String atualizadoEmPreset) { this.atualizadoEmPreset = atualizadoEmPreset; }
    public Integer getAtualizadoEmLastDays() { return atualizadoEmLastDays; }
    public void setAtualizadoEmLastDays(Integer atualizadoEmLastDays) { this.atualizadoEmLastDays = atualizadoEmLastDays; }
}

