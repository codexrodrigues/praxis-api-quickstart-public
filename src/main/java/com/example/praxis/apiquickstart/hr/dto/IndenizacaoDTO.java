package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.annotation.AiControlledUseMode;
import org.praxisplatform.uischema.annotation.AiTrainingUseMode;
import org.praxisplatform.uischema.annotation.AiVisibilityMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;

import java.math.BigDecimal;

@Schema(
        name = "IndenizacaoDTO",
        description = "Registo de cobertura indenizatoria (seguro, sinistro) associado a um incidente de Operacoes. "
                + "Aproxima a ponte entre RH, financeiro e o modulo de incidentes: valor acordado, pagamento, seguradora e processo; nao substitui apolice nem contrato.")
public class IndenizacaoDTO {
    @Schema(
            description = "Chave da indenizacao. Um incidente pode ter varias parcelas ou revisoes; cada registo materializa um valor e um estado de liquidacao.",
            example = "1")
    private Integer id;

    @NotNull
    @DecimalMin("0.00")
    @DomainGovernance(
        kind = DomainGovernanceKind.COMPLIANCE,
        classification = DomainClassification.CONFIDENTIAL,
        dataCategory = DomainDataCategory.FINANCIAL,
        complianceTags = {"INTERNAL_POLICY"},
        reason = "Valores indenizatórios são acordos financeiros confidenciais entre a empresa, seguradora e as partes envolvidas.",
        aiUsage = @AiUsagePolicy(
            visibility = AiVisibilityMode.SUMMARIZE_ONLY,
            trainingUse = AiTrainingUseMode.DENY,
            ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
            reasoningUse = AiControlledUseMode.ALLOW
        )
    )
    @UISchema(label = "Valor", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, required = true, helpText = "Montante aprovado para indenização.", icon = "payments")
    @Schema(
            description = "Montante em moeda do backend; valor alvo de cobertura acordada ou aprovada, nao necessariamente ja transferido (ver pago).",
            example = "25000.00")
    private BigDecimal valor;

    @NotNull
    @UISchema(label = "Pago", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, required = true, helpText = "Indica se o valor já foi transferido.", icon = "payments")
    @Schema(
            description = "Indica se o valor foi liquidado (transferencia, ordem bancaria) no sentido de reconciliacao de RH; ainda pode exigir anexo de comprovante fora do DTO.",
            example = "false")
    private Boolean pago;

    @Size(max = 200)
    @UISchema(label = "Seguradora", controlType = FieldControlType.INPUT, maxLength = 200, helpText = "Empresa responsável pela cobertura.", icon = "label")
    @Schema(
            description = "Entidade underwriter ou TPA que processa a reclamacao; vazio se a cobertura for assumida internamente.",
            example = "Cobertura Metropolis Mutual")
    private String seguradora;

    @Size(max = 100)
    @UISchema(label = "Número do Processo", controlType = FieldControlType.INPUT, maxLength = 100, helpText = "Número do processo ou protocolo do sinistro.", icon = "fingerprint")
    @Schema(
            description = "Protocolo de sinistro ou processo administrativo; rastreio com a seguradora ou departamento de risco (nao e numero de processo judicial unificado automaticamente).",
            example = "SIN-2025-00412")
    private String processoNum;

    @NotNull
    @UISchema(label = "Incidente", controlType = FieldControlType.ENTITY_LOOKUP,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Operations.INCIDENTES_INCIDENT_LOOKUP_OPTIONS, required = true,
            tableHidden = true, helpText = "Incidente que originou a indenização.", icon = "report_problem")
    @Schema(
            description = "Incidente de Operacoes que originou a cobertura: FK a Incidente; ancora danos, missao e responsavel operacional.",
            example = "7")
    private Integer incidenteId;

    @UISchema(label = "Incidente", readOnly = true, formHidden = true, helpText = "Título do incidente (preenchido automaticamente).", icon = "report_problem")
    @Schema(
            description = "Titulo do incidente (desnormalizado, leitura) para tabelas de acompanhamento de sinistros e reconciliacao com a equipa de missao.")
    private String incidenteTitulo;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public Boolean getPago() { return pago; }
    public void setPago(Boolean pago) { this.pago = pago; }
    public String getSeguradora() { return seguradora; }
    public void setSeguradora(String seguradora) { this.seguradora = seguradora; }
    public String getProcessoNum() { return processoNum; }
    public void setProcessoNum(String processoNum) { this.processoNum = processoNum; }
    public Integer getIncidenteId() { return incidenteId; }
    public void setIncidenteId(Integer incidenteId) { this.incidenteId = incidenteId; }
    public String getIncidenteTitulo() { return incidenteTitulo; }
    public void setIncidenteTitulo(String incidenteTitulo) { this.incidenteTitulo = incidenteTitulo; }
}
