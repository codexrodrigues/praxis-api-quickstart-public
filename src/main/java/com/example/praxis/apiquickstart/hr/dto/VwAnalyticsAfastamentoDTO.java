package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.praxisplatform.uischema.annotation.AiControlledUseMode;
import org.praxisplatform.uischema.annotation.AiTrainingUseMode;
import org.praxisplatform.uischema.annotation.AiVisibilityMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.LocalDate;

@UISchema(label = "Analytics de Afastamentos", readOnly = true, icon = "analytics")
@Schema(
        name = "VwAnalyticsAfastamentoDTO",
        description = "Projecao somente leitura de dias unicos de afastamento por colaborador, lotacao historica efetiva e competencia mensal. "
                + "Publica metricas agregaveis e criticidade versionada sem expor motivo, observacoes, nome ou departamento atual do colaborador.")
public class VwAnalyticsAfastamentoDTO {
    @Schema(description = "Identidade estavel da linha analitica, formada por colaborador, departamento efetivo e competencia mensal.", example = "7:3:202607")
    @UISchema(label = "Cód. Analítico", formHidden = true, icon = "tag")
    private String analyticsId;

    @Schema(description = "Colaborador associado à linha analítica. Permite drill-down governado para employee-360 sem publicar nome ou dados pessoais nesta view.", example = "7")
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.INTERNAL,
            dataCategory = DomainDataCategory.PERSONAL,
            complianceTags = {"LGPD", "GDPR"},
            reason = "Identificador pseudonimizado de colaborador ainda permite acesso nominal quando combinado com employee-360; deve ser separado da leitura agregada.",
            aiUsage = @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY,
                    ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
                    reasoningUse = AiControlledUseMode.REVIEW_REQUIRED
            )
    )
    @UISchema(label = "Cód. Colaborador", formHidden = true, icon = "badge")
    private Integer funcionarioId;

    @Schema(description = "Departamento efetivo no periodo analitico, resolvido pela tabela historica de lotacao; nao usa o departamento atual do cadastro.", example = "3")
    @UISchema(label = "Cód. Departamento", type = FieldDataType.NUMBER, formHidden = true, icon = "apartment")
    private Integer departamentoId;

    @Schema(description = "Codigo corporativo do departamento efetivo no periodo analisado.", example = "RH-OPS")
    @UISchema(label = "Código Departamento", icon = "apartment")
    private String departamentoCodigo;

    @Schema(description = "Nome do departamento efetivo usado apenas como label de bucket em comparacoes e tabelas analiticas.", example = "Operações de RH")
    @UISchema(label = "Departamento", icon = "apartment")
    private String departamento;

    @Schema(description = "Competencia mensal da linha analitica, sempre no primeiro dia do mes.", example = "2026-07-01")
    @UISchema(label = "Competência", type = FieldDataType.DATE, icon = "calendar_month")
    private LocalDate competencia;

    @Schema(description = "Ano civil da competencia.", example = "2026")
    @UISchema(label = "Ano", type = FieldDataType.NUMBER, icon = "calendar_today")
    private Integer ano;

    @Schema(description = "Mes civil da competencia, de 1 a 12.", example = "7")
    @UISchema(label = "Mês", type = FieldDataType.NUMBER, icon = "calendar_month")
    private Integer mes;

    @Schema(description = "Primeiro dia coberto pela uniao de afastamentos atribuidos à lotacao e competencia desta linha; nao implica continuidade ate a data final.", example = "2026-07-01")
    @UISchema(label = "Início Analítico", type = FieldDataType.DATE, icon = "event")
    private LocalDate periodoInicio;

    @Schema(description = "Ultimo dia coberto pela uniao de afastamentos atribuidos à lotacao e competencia desta linha; nao implica continuidade desde a data inicial.", example = "2026-07-15")
    @UISchema(label = "Fim Analítico", type = FieldDataType.DATE, icon = "event_available")
    private LocalDate periodoFim;

    @Schema(description = "Quantidade de dias corridos unicos de afastamento no grão colaborador, departamento efetivo e competencia; sobreposicoes nao duplicam a metrica.", example = "15")
    @UISchema(label = "Dias Afastado", type = FieldDataType.NUMBER, icon = "timer")
    private Long diasAfastado;

    @Schema(description = "Nivel de criticidade calculado por politica versionada: STANDARD, ATTENTION ou CRITICAL.", example = "CRITICAL")
    @UISchema(label = "Criticidade", icon = "priority_high")
    private String criticalityLevel;

    @Schema(description = "Identificador da politica deterministica usada para classificar criticidade nesta projection.", example = "hr-absence-criticality-v1")
    @UISchema(label = "Política", formHidden = true, icon = "policy")
    private String criticalityPolicyId;

    @Schema(description = "Versao da politica de criticidade aplicada à linha analitica.", example = "2026-07-15")
    @UISchema(label = "Versão Política", formHidden = true, icon = "policy")
    private String criticalityPolicyVersion;

    public String getAnalyticsId() { return analyticsId; }
    public void setAnalyticsId(String analyticsId) { this.analyticsId = analyticsId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public Integer getDepartamentoId() { return departamentoId; }
    public void setDepartamentoId(Integer departamentoId) { this.departamentoId = departamentoId; }
    public String getDepartamentoCodigo() { return departamentoCodigo; }
    public void setDepartamentoCodigo(String departamentoCodigo) { this.departamentoCodigo = departamentoCodigo; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public LocalDate getCompetencia() { return competencia; }
    public void setCompetencia(LocalDate competencia) { this.competencia = competencia; }
    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }
    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
    public LocalDate getPeriodoInicio() { return periodoInicio; }
    public void setPeriodoInicio(LocalDate periodoInicio) { this.periodoInicio = periodoInicio; }
    public LocalDate getPeriodoFim() { return periodoFim; }
    public void setPeriodoFim(LocalDate periodoFim) { this.periodoFim = periodoFim; }
    public Long getDiasAfastado() { return diasAfastado; }
    public void setDiasAfastado(Long diasAfastado) { this.diasAfastado = diasAfastado; }
    public String getCriticalityLevel() { return criticalityLevel; }
    public void setCriticalityLevel(String criticalityLevel) { this.criticalityLevel = criticalityLevel; }
    public String getCriticalityPolicyId() { return criticalityPolicyId; }
    public void setCriticalityPolicyId(String criticalityPolicyId) { this.criticalityPolicyId = criticalityPolicyId; }
    public String getCriticalityPolicyVersion() { return criticalityPolicyVersion; }
    public void setCriticalityPolicyVersion(String criticalityPolicyVersion) { this.criticalityPolicyVersion = criticalityPolicyVersion; }
}
