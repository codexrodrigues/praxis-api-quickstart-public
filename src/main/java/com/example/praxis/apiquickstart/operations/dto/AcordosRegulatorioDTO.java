package com.example.praxis.apiquickstart.operations.dto;

import com.example.praxis.apiquickstart.operations.enums.AcordoStatus;
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

@Schema(
    name = "AcordosRegulatorioDTO",
    description = "Registro de acordo ou conformidade com orgaos reguladores: jurisdicao, status e narrativa. Usado em auditorias e roadmaps de compliance."
)
public class AcordosRegulatorioDTO {
    @Schema(description = "Identificador do acordo no cadastro de operacoes.", example = "1")
    private Integer id;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Designacao publica do acordo ou programa de atendimento a norma.", example = "Convenio ANPD — dados pessoais")
    @UISchema(label = "Nome", controlType = FieldControlType.INPUT, required = true, maxLength = 200, icon = "badge")
    private String nome;

    @NotBlank
    @Size(max = 200)
    @DomainGovernance(
            kind = DomainGovernanceKind.COMPLIANCE,
            classification = DomainClassification.INTERNAL,
            dataCategory = DomainDataCategory.LEGAL,
            complianceTags = {"INTERNAL_POLICY", "REGULATORY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.ALLOW, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.ALLOW),
            reason = "Jurisdicao regulatoria aplicavel ao acordo."
    )
    @Schema(
        description = "Pais, estado ou ente regulador competente; impacta criterio de exigencia e de reporting."
    )
    @UISchema(label = "Jurisdição", controlType = FieldControlType.INPUT, required = true, maxLength = 200, icon = "label")
    private String jurisdicao;

    @NotNull
    @DomainGovernance(
            kind = DomainGovernanceKind.COMPLIANCE,
            classification = DomainClassification.INTERNAL,
            dataCategory = DomainDataCategory.LEGAL,
            complianceTags = {"INTERNAL_POLICY", "REGULATORY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.ALLOW, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.ALLOW),
            reason = "Status regulatorio do acordo."
    )
    @Schema(description = "Ciclo de vida do acordo: rascunho, em vigor, vencido ou arquivado (semantica do enum do dominio).")
    @UISchema(label = "Status", controlType = FieldControlType.SELECT, required = true, icon = "toggle_on")
    private AcordoStatus status;

    @Size(max = 4000)
    @Schema(
        description = "Texto livre de escopo, obrigacoes, evidencias e calendario de provas; base para revisor humano e IA (com sumarizacao quando sensiveis)."
    )
    @UISchema(label = "Descrição", controlType = FieldControlType.TEXTAREA, maxLength = 4000, icon = "description")
    private String descricao;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getJurisdicao() { return jurisdicao; }
    public void setJurisdicao(String jurisdicao) { this.jurisdicao = jurisdicao; }
    public AcordoStatus getStatus() { return status; }
    public void setStatus(AcordoStatus status) { this.status = status; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
