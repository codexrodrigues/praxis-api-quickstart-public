package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.annotation.AiUsageMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO da trilha salarial do funcionario.
 *
 * <p>Modela a evolucao de remuneracao ao longo do tempo, com vigencia
 * inicial/final e justificativa administrativa para cada mudanca.
 */
@Schema(
        name = "HistoricoSalarialDTO",
        description = "Registo de faixa ou valor de remuneracao do colaborador num intervalo de vigencia. Alimenta analises de carreira e preparacao de folha; "
                + "dados de salario sao sensiveis (LGPD) e nao sao a mesma coisa que o pagamento realizado na folha (veja eventos de folha).")
public class HistoricoSalarialDTO {
    @Schema(
            description = "Chave de uma fatia de historico. Varias fatias no tempo cobrem a trajectoria salarial; encadeamento e fechamento de intervalos pertencem a regra de negocio.",
            example = "1")
    private Integer id;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP, required = true, icon = "badge",
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            helpText = "Colaborador titular do histórico.")
    @Schema(
            description = "Colaborador cujo salario (neste recorte) esta a ser registado; referencia o recurso de funcionario.",
            example = "4")
    private Integer funcionarioId;

    @NotNull
    @DecimalMin("0.00")
    @DomainGovernance(
        kind = DomainGovernanceKind.COMPLIANCE,
        classification = DomainClassification.CONFIDENTIAL,
        dataCategory = DomainDataCategory.FINANCIAL,
        complianceTags = {"INTERNAL_POLICY", "FINANCIAL_PRIVACY"},
        reason = "O valor nominal do salário é estritamente confidencial e não deve ser exposto de forma bruta por IAs.",
        aiUsage = @AiUsagePolicy(
            visibility = AiUsageMode.SUMMARIZE_ONLY,
            trainingUse = AiUsageMode.DENY,
            ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
            reasoningUse = AiUsageMode.ALLOW
        )
    )
    @UISchema(label = "Salário", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, required = true, helpText = "Valor da remuneração no período.", icon = "payments")
    @Schema(
            description = "Base remuneratoria acordada para o periodo (moeda do backend, tipicamente BRL). Valor de RH administrativo, confidencial; nao e liquido de transferencia bancaria.",
            example = "15000.00")
    private BigDecimal salario;

    @NotNull
    @UISchema(label = "Data Início", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, required = true, helpText = "Primeiro dia de vigência do salário.", icon = "event")
    @Schema(
            description = "Inicio de vigencia deste valor (inclusive); primeiro dia em que a faixa e considerada ativa para efeito de regra de negocio.",
            example = "2024-01-01")
    private LocalDate dataInicio;

    @UISchema(label = "Data Fim", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, helpText = "Último dia de vigência (se aplicável).", icon = "event_available")
    @Schema(
            description = "Fim de vigencia (inclusive) ou nulo se ainda ativa. Nova fatia consecutiva deve comecar apos o encerramento logico (evitar buracos e sobreposicoes).",
            example = "2024-12-31")
    private LocalDate dataFim;

    @Size(max = 2000)
    @UISchema(label = "Motivo", controlType = FieldControlType.TEXTAREA, maxLength = 2000, helpText = "Justificativa da alteração (ex: promoção, reajuste).", icon = "notes")
    @Schema(
            description = "Justificativa administrativa (reajuste, promocao, correcao, acordo) para rastreabilidade; insumo de auditoria interna, nao substitui o documento legal.",
            example = "Reajuste anual 2024")
    private String motivo;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public BigDecimal getSalario() { return salario; }
    public void setSalario(BigDecimal salario) { this.salario = salario; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
