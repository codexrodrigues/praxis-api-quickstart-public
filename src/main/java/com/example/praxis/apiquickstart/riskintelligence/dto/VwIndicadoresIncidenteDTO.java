package com.example.praxis.apiquickstart.riskintelligence.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.AiUsageMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@UISchema(label = "Indicadores de Incidentes", readOnly = true, icon = "report_problem")
@Schema(
        name = "VwIndicadoresIncidenteDTO",
        description = "Linha de vista so-leitura: indicadores de sinistro/ incidente agregando impacto, severidade e trilha financeira (indenizacoes, pago, pendente). "
                + "Cruza Operacoes, local e indicadores financeiros para painéis de risco e respostas assistidas, sem substituir o incidente transacional editavel.")
public class VwIndicadoresIncidenteDTO {
    @Schema(
            description = "Chave do incidente transacional que originou a linha materializada; liga o indicador ao cadastro e ao workflow de Operacoes.",
            example = "7")
    private Integer incidenteId;

    @UISchema(label = "Missão", icon = "flag")
    @Schema(
            description = "Titulo ou referencia da missao associada ao incidente, desnormalizado para leitura, agrupamento e explicacao no painel de risco.")
    private String missao;

    @UISchema(label = "Descrição", icon = "description")
    @DomainGovernance(
            kind = DomainGovernanceKind.SECURITY,
            classification = DomainClassification.INTERNAL,
            dataCategory = DomainDataCategory.OPERATIONAL,
            complianceTags = {"INTERNAL_POLICY"},
            aiUsage = @AiUsagePolicy(
                    visibility = AiUsageMode.SUMMARIZE_ONLY,
                    trainingUse = AiUsageMode.DENY,
                    ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
                    reasoningUse = AiUsageMode.REVIEW_REQUIRED),
            reason = "Narrativa de incidente pode conter contexto operacional sensivel."
    )
    @Schema(
            description = "Resumo ou narrativa curta exibida no painel; nao e o relatorio completo e pode exigir resumo para uso por IA.")
    private String descricao;

    @UISchema(label = "Local", icon = "location_on")
    @DomainGovernance(
            kind = DomainGovernanceKind.SECURITY,
            classification = DomainClassification.INTERNAL,
            dataCategory = DomainDataCategory.OPERATIONAL,
            complianceTags = {"INTERNAL_POLICY"},
            aiUsage = @AiUsagePolicy(
                    visibility = AiUsageMode.ALLOW,
                    trainingUse = AiUsageMode.DENY,
                    ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
                    reasoningUse = AiUsageMode.ALLOW),
            reason = "Local do incidente direciona triagem e dashboards, mas ainda pertence ao contexto operacional."
    )
    @Schema(
            description = "Geografia ou cenario do incidente; dimensao operacional para filtros, mapas e dashboards de risco.")
    private String local;

    @UISchema(label = "Severidade", icon = "emergency")
    @Schema(
            description = "Classe de gravidade atribuida ao incidente; orienta triagem, priorizacao e agrupamentos nos indicadores de risco.")
    private String severidade;

    @UISchema(label = "Danos Civis", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, icon = "calendar_today")
    @DomainGovernance(
            kind = DomainGovernanceKind.COMPLIANCE,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.FINANCIAL,
            complianceTags = {"FINANCIAL_CONTROL", "RISK_AUDIT"},
            aiUsage = @AiUsagePolicy(
                    visibility = AiUsageMode.SUMMARIZE_ONLY,
                    trainingUse = AiUsageMode.DENY,
                    ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
                    reasoningUse = AiUsageMode.ALLOW),
            reason = "Valor financeiro agregado deve ser usado preferencialmente em analises agregadas e decisoes auditaveis."
    )
    @Schema(
            description = "Estimativa financeira agregada de dano a terceiros ou infraestrutura; metrica de risco para histogramas, rankings e auditoria.")
    private BigDecimal danosCivis;

    @UISchema(label = "Total Indenizações", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, icon = "warning")
    @DomainGovernance(
            kind = DomainGovernanceKind.COMPLIANCE,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.FINANCIAL,
            complianceTags = {"FINANCIAL_CONTROL", "RISK_AUDIT"},
            aiUsage = @AiUsagePolicy(
                    visibility = AiUsageMode.SUMMARIZE_ONLY,
                    trainingUse = AiUsageMode.DENY,
                    ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
                    reasoningUse = AiUsageMode.ALLOW),
            reason = "Total de indenizacoes compoe analise financeira de risco e exige rastreabilidade."
    )
    @Schema(
            description = "Soma agregada de indenizacoes associadas ao incidente; metrica financeira para comparacao, dashboard e controle.")
    private BigDecimal totalIndenizacoes;

    @UISchema(label = "Total Pago", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, icon = "payments")
    @DomainGovernance(
            kind = DomainGovernanceKind.COMPLIANCE,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.FINANCIAL,
            complianceTags = {"FINANCIAL_CONTROL", "RISK_AUDIT"},
            aiUsage = @AiUsagePolicy(
                    visibility = AiUsageMode.SUMMARIZE_ONLY,
                    trainingUse = AiUsageMode.DENY,
                    ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
                    reasoningUse = AiUsageMode.ALLOW),
            reason = "Valor liquidado deve ser explicado como metrica agregada, nao como dado transacional de pagamento individual."
    )
    @Schema(
            description = "Parcela agregada ja liquidada da cobertura; usada para reconciliacao, progresso financeiro e dashboards executivos.")
    private BigDecimal totalPago;

    @UISchema(label = "Total Pendente", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, icon = "payments")
    @DomainGovernance(
            kind = DomainGovernanceKind.COMPLIANCE,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.FINANCIAL,
            complianceTags = {"FINANCIAL_CONTROL", "RISK_AUDIT"},
            aiUsage = @AiUsagePolicy(
                    visibility = AiUsageMode.SUMMARIZE_ONLY,
                    trainingUse = AiUsageMode.DENY,
                    ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
                    reasoningUse = AiUsageMode.ALLOW),
            reason = "Saldo pendente direciona priorizacao e deve manter governanca financeira."
    )
    @Schema(
            description = "Saldo agregado ainda nao pago; metrica para priorizacao, controle financeiro e comparacao com total de indenizacoes.")
    private BigDecimal totalPendente;

    @UISchema(label = "Ocorrido em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "event")
    @Schema(
            description = "Marco temporal do incidente com offset, usado para linha do tempo operacional, filtros de recencia e correlacao com missoes.",
            example = "2025-04-20T10:00:00Z")
    private OffsetDateTime ocorridoEm;

    public Integer getIncidenteId() { return incidenteId; }
    public void setIncidenteId(Integer incidenteId) { this.incidenteId = incidenteId; }
    public String getMissao() { return missao; }
    public void setMissao(String missao) { this.missao = missao; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public String getSeveridade() { return severidade; }
    public void setSeveridade(String severidade) { this.severidade = severidade; }
    public BigDecimal getDanosCivis() { return danosCivis; }
    public void setDanosCivis(BigDecimal danosCivis) { this.danosCivis = danosCivis; }
    public BigDecimal getTotalIndenizacoes() { return totalIndenizacoes; }
    public void setTotalIndenizacoes(BigDecimal totalIndenizacoes) { this.totalIndenizacoes = totalIndenizacoes; }
    public BigDecimal getTotalPago() { return totalPago; }
    public void setTotalPago(BigDecimal totalPago) { this.totalPago = totalPago; }
    public BigDecimal getTotalPendente() { return totalPendente; }
    public void setTotalPendente(BigDecimal totalPendente) { this.totalPendente = totalPendente; }
    public OffsetDateTime getOcorridoEm() { return ocorridoEm; }
    public void setOcorridoEm(OffsetDateTime ocorridoEm) { this.ocorridoEm = ocorridoEm; }
}
