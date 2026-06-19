package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.annotation.AiUsageMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO principal da folha de pagamento.
 *
 * <p>Representa a competencia de um funcionario com seus valores consolidados,
 * servindo de base para CRUD, listagens operacionais e workflows de pagamento.
 */
@Schema(
    name = "FolhasPagamentoDTO",
    description = "Competencia de folha de um colaborador: valores de referencia, descontos, liquido e data de pagamento. Dados financeiros com governanca LGPD/politica interna."
)
public class FolhasPagamentoDTO {
    @Schema(description = "Identificador interno do registo de folha no servico de RH.", example = "1")
    private Integer id;

    @NotNull
    @Min(1900)
    @Max(2100)
    @Schema(description = "Ano da competencia de folha (calendario civil).", example = "2025")
    @UISchema(label = "Ano", required = true, group = "Competência", order = 10, helpText = "Ano base da competência (ex: 2025).", icon = "calendar_today")
    private Integer ano;

    @NotNull
    @Min(1)
    @Max(12)
    @Schema(description = "Mes da competencia (1-12) para a qual a folha se aplica.", example = "3")
    @UISchema(label = "Mês", required = true, group = "Competência", order = 20, helpText = "Mês base da competência (1 a 12).", icon = "calendar_month")
    private Integer mes;

    @NotNull
    @DecimalMin("0.00")
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.FINANCIAL,
            complianceTags = {"LGPD", "INTERNAL_POLICY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.MASK, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.ALLOW),
            reason = "Valor bruto da folha."
    )
    @Schema(
        description = "Total de vencimentos antes de descontos na competencia; dado financeiro sujeito a controle e mascaramento em contexto de IA."
    )
    @UISchema(label = "Salário Bruto", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, group = "Valores", order = 10, helpText = "Valor total sem os descontos.", icon = "payments")
    private BigDecimal salarioBruto;

    @NotNull
    @DecimalMin("0.00")
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.FINANCIAL,
            complianceTags = {"LGPD", "INTERNAL_POLICY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.MASK, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.ALLOW),
            reason = "Total de descontos da folha."
    )
    @Schema(
        description = "Soma de descontos legais e contratuais aplicados na competencia (INSS, IRRF, beneficios, etc., conforme configuracao do tenant)."
    )
    @UISchema(label = "Total Descontos", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, group = "Valores", order = 20, helpText = "Soma de todos os descontos.", icon = "money_off")
    private BigDecimal totalDescontos;

    @NotNull
    @DecimalMin("0.00")
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.FINANCIAL,
            complianceTags = {"LGPD", "INTERNAL_POLICY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.MASK, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.ALLOW),
            reason = "Valor liquido pago ao colaborador."
    )
    @Schema(
        description = "Valor liquido creditado ou a creditar ao colaborador apos descontos; dado pessoal e financeiro."
    )
    @UISchema(label = "Salário Líquido", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, group = "Valores", order = 30, helpText = "Valor final creditado ao colaborador.", icon = "account_balance_wallet")
    private BigDecimal salarioLiquido;

    @NotNull
    @Schema(description = "Data efetiva ou programada de pagamento da competencia, usada em conciliacao e auditoria.")
    @UISchema(label = "Data de Pagamento", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, group = "Competência", order = 30, helpText = "Data programada para o depósito.", icon = "event_available")
    private LocalDate dataPagamento;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP, group = "Relacionamentos", order = 10, icon = "badge",
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            helpText = "Colaborador titular desta folha.")
    @Schema(
        description = "Chave do colaborador a quem a folha se refere; liga a cadastro de funcionario do mesmo tenant."
    )
    private Integer funcionarioId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }
    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
    public BigDecimal getSalarioBruto() { return salarioBruto; }
    public void setSalarioBruto(BigDecimal salarioBruto) { this.salarioBruto = salarioBruto; }
    public BigDecimal getTotalDescontos() { return totalDescontos; }
    public void setTotalDescontos(BigDecimal totalDescontos) { this.totalDescontos = totalDescontos; }
    public BigDecimal getSalarioLiquido() { return salarioLiquido; }
    public void setSalarioLiquido(BigDecimal salarioLiquido) { this.salarioLiquido = salarioLiquido; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
}
