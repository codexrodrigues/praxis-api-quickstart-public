package com.example.praxis.apiquickstart.procurement.dto;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.annotation.AiUsageMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
    name = "ProcurementSupplierDTO",
    description = "Fornecedor homologavel para compras: vinculo a empresa, documento, risco e bloqueio. Governanca de dados pessoais e compliance em entidades reguladas."
)
@Getter
@Setter
public class ProcurementSupplierDTO {
    @Schema(description = "Identificador do fornecedor no modulo de procurement.", example = "1")
    private Integer id;

    @Schema(description = "Tenant ou empresa a que o cadastro de fornecedor se subordina em compras multibase.")
    @UISchema(label = "Empresa", controlType = FieldControlType.ENTITY_LOOKUP, order = 10, icon = "business",
            endpoint = ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS, valueField = "id", displayField = "label")
    private Integer companyId;

    @Schema(description = "Codigo unico alfanumerico usado em pedidos, XML e conciliacoes fiscais.", example = "FORN-00042")
    @UISchema(label = "Codigo", controlType = FieldControlType.INPUT, order = 20, icon = "tag")
    private String code;

    @Schema(
        description = "Razao social ou denominacao completa para contratos, cartas de homologacao e notas fiscais."
    )
    @UISchema(label = "Razao social", controlType = FieldControlType.INPUT, required = true, order = 30, icon = "business")
    private String legalName;

    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.PERSONAL,
            complianceTags = {"LGPD", "GDPR"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.MASK, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.REVIEW_REQUIRED),
            reason = "Documento de identificacao cadastral do fornecedor."
    )
    @Schema(
        description = "CNPJ, CPF ou outro registro legal conforme a politica de cadastro; dado sensiveis para KYC e fisco."
    )
    @UISchema(label = "Documento", controlType = FieldControlType.INPUT, order = 40, icon = "fingerprint")
    private String documentNumber;

    @DomainGovernance(
            kind = DomainGovernanceKind.COMPLIANCE,
            classification = DomainClassification.INTERNAL,
            dataCategory = DomainDataCategory.LEGAL,
            complianceTags = {"INTERNAL_POLICY", "REGULATORY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.ALLOW, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.ALLOW),
            reason = "Homologacao necessaria para compras governadas."
    )
    @Schema(
        description = "Estagio de aprovacao no processo de sourcing (PENDING, APPROVED, etc., conforme enum/tabela de negocio)."
    )
    @UISchema(label = "Homologacao", controlType = FieldControlType.SELECT, order = 50, icon = "toggle_on")
    private String homologationStatus;

    @DomainGovernance(
            kind = DomainGovernanceKind.SECURITY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.OPERATIONAL,
            complianceTags = {"INTERNAL_POLICY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.SUMMARIZE_ONLY, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.REVIEW_REQUIRED),
            reason = "Nivel de risco operacional do fornecedor."
    )
    @Schema(description = "Categoria de risco de terceiro; impacta aprovacoes, limites de pedido e monitoramento.")
    @UISchema(label = "Risco", controlType = FieldControlType.SELECT, order = 60, icon = "warning")
    private String riskLevel;

    @DomainGovernance(
            kind = DomainGovernanceKind.COMPLIANCE,
            classification = DomainClassification.INTERNAL,
            dataCategory = DomainDataCategory.LEGAL,
            complianceTags = {"INTERNAL_POLICY", "REGULATORY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.ALLOW, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.ALLOW),
            reason = "Status que governa elegibilidade do fornecedor para compras."
    )
    @Schema(description = "Elegibilidade geral (ACTIVE, INACTIVE, BLOCKED); bloqueia novos pedidos quando aplicavel.")
    @UISchema(label = "Status", controlType = FieldControlType.SELECT, order = 70, icon = "toggle_on")
    private String status;

    @DomainGovernance(
            kind = DomainGovernanceKind.COMPLIANCE,
            classification = DomainClassification.INTERNAL,
            dataCategory = DomainDataCategory.LEGAL,
            complianceTags = {"INTERNAL_POLICY", "REGULATORY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.ALLOW, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.ALLOW),
            reason = "Motivo documental para bloqueio do fornecedor."
    )
    @Schema(
        description = "Justificativa de compliance ou risco associada a bloqueio; visivel a auditores, nao a compradores de rotina em alguns perfis."
    )
    @UISchema(label = "Motivo de bloqueio", controlType = FieldControlType.INPUT, order = 80, icon = "notes")
    private String disabledReason;
}
