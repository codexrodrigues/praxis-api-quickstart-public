package com.example.praxis.apiquickstart.hr.dto.filter;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.security.HrDepartmentScopedFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.praxisplatform.uischema.annotation.AiControlledUseMode;
import org.praxisplatform.uischema.annotation.AiTrainingUseMode;
import org.praxisplatform.uischema.annotation.AiVisibilityMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "VwAnalyticsAfastamentoFilterDTO",
        description = "Criterios de busca sobre a projection mensal de afastamentos por lotacao historica. "
                + "Filtra recortes analiticos seguros para comparison, listas criticas e drill-down governado.")
public class VwAnalyticsAfastamentoFilterDTO implements GenericFilterDTO, HrDepartmentScopedFilter {
    @UISchema(label = "Competência", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 10, helpText = "Janela mensal considerada na análise.", icon = "calendar_month")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "competencia")
    @Schema(description = "Janela de competencias mensais que delimitam a analise comparativa.")
    private List<LocalDate> competenciaBetween;

    @UISchema(label = "Período analítico", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 20, helpText = "Filtra pelo primeiro dia coberto no grão mensal e na lotação efetiva.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "periodoInicio")
    @Schema(description = "Intervalo aplicado sobre o primeiro dia coberto pela uniao de afastamentos no grão analitico.")
    private List<LocalDate> periodoInicioBetween;

    @UISchema(label = "Departamento", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 30,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_OPTIONS, helpText = "Filtra pela lotação efetiva do período.", icon = "apartment")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "departamentoId")
    @Schema(description = "Departamento efetivo resolvido historicamente, sem fallback para o departamento atual do colaborador.")
    private Integer departamentoId;

    @UISchema(label = "Departamentos", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 35,
            multiple = true, valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_OPTIONS, formHidden = true, icon = "apartment")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "departamentoId")
    @Schema(description = "Conjunto de departamentos efetivos. O backend intersecta este filtro com o escopo do principal quando houver restrição departamental.")
    private List<Integer> departamentoIdsIn;

    @UISchema(label = "Departamento", controlType = FieldControlType.INPUT, maxLength = 120, order = 40, formHidden = true, helpText = "Busca textual auxiliar pelo label do departamento.", icon = "apartment")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(description = "Trecho do nome do departamento efetivo usado apenas como filtro auxiliar.")
    private String departamento;

    @UISchema(label = "Criticidade", controlType = FieldControlType.ASYNC_SELECT, order = 50,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_ANALYTICS_AFASTAMENTOS + "/option-sources/criticalityLevel/options/filter", helpText = "Filtra pelo nível calculado da política de criticidade.", icon = "priority_high")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(description = "Nivel de criticidade versionado: STANDARD, ATTENTION ou CRITICAL.")
    private String criticalityLevel;

    @UISchema(label = "Mostrar colaboradores", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 60,
            multiple = true,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, helpText = "Inclui na análise apenas colaboradores autorizados selecionados.", icon = "checklist")
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.INTERNAL,
            dataCategory = DomainDataCategory.PERSONAL,
            complianceTags = {"LGPD", "GDPR"},
            reason = "Filtro nominal de colaborador deve ser usado apenas por fluxos autorizados; comparacoes agregadas o rejeitam sem autoridade nominal e dashboards devem preferir departamento e competencia.",
            aiUsage = @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY,
                    ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
                    reasoningUse = AiControlledUseMode.REVIEW_REQUIRED
            )
    )
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "funcionarioId")
    @Schema(description = "Conjunto de colaboradores usado para drill-down nominal governado; comparacoes agregadas exigem autoridade nominal quando este filtro esta presente e a projection não publica nomes.")
    private List<Integer> funcionarioIdsIn;

    @UISchema(label = "Dias afastado", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 70, helpText = "Filtra pela quantidade de dias únicos no grão analítico.", icon = "timer")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "diasAfastado")
    @Schema(description = "Intervalo de dias corridos unicos de afastamento no grão colaborador, departamento efetivo e competencia.")
    private List<Long> diasAfastadoBetween;

    public List<LocalDate> getCompetenciaBetween() { return competenciaBetween; }
    public void setCompetenciaBetween(List<LocalDate> competenciaBetween) { this.competenciaBetween = competenciaBetween; }
    public List<LocalDate> getPeriodoInicioBetween() { return periodoInicioBetween; }
    public void setPeriodoInicioBetween(List<LocalDate> periodoInicioBetween) { this.periodoInicioBetween = periodoInicioBetween; }
    public Integer getDepartamentoId() { return departamentoId; }
    public void setDepartamentoId(Integer departamentoId) { this.departamentoId = departamentoId; }
    public List<Integer> getDepartamentoIdsIn() { return departamentoIdsIn; }
    public void setDepartamentoIdsIn(List<Integer> departamentoIdsIn) { this.departamentoIdsIn = departamentoIdsIn; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public String getCriticalityLevel() { return criticalityLevel; }
    public void setCriticalityLevel(String criticalityLevel) { this.criticalityLevel = criticalityLevel; }
    public List<Integer> getFuncionarioIdsIn() { return funcionarioIdsIn; }
    public void setFuncionarioIdsIn(List<Integer> funcionarioIdsIn) { this.funcionarioIdsIn = funcionarioIdsIn; }
    public List<Long> getDiasAfastadoBetween() { return diasAfastadoBetween; }
    public void setDiasAfastadoBetween(List<Long> diasAfastadoBetween) { this.diasAfastadoBetween = diasAfastadoBetween; }
}
