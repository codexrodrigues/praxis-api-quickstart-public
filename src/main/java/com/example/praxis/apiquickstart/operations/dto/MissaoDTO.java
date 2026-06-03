package com.example.praxis.apiquickstart.operations.dto;

import com.example.praxis.apiquickstart.operations.enums.MissaoPrioridade;
import com.example.praxis.apiquickstart.operations.enums.MissaoStatus;
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

import java.time.OffsetDateTime;

@Schema(
    name = "MissaoDTO",
    description = "Operacao tatica vinculada a uma ameaca: prazos, local, status e trilha de compliance. Dados de objetivo e prioridade sao operacionalmente sensiveis."
)
public class MissaoDTO {
    @Schema(description = "Identificador interno da missao.", example = "1")
    private Integer id;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Titulo curto exibido em listas e aprovacoes; identifica a missao de forma unica perante a equipa operacional.", example = "Reconhecimento setor 7")
    @UISchema(label = "Título", controlType = FieldControlType.INPUT, required = true, maxLength = 200, icon = "title")
    private String titulo;

    @Size(max = 4000)
    @DomainGovernance(
            kind = DomainGovernanceKind.SECURITY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.OPERATIONAL,
            complianceTags = {"INTERNAL_POLICY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.SUMMARIZE_ONLY, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.REVIEW_REQUIRED),
            reason = "Objetivo operacional da missao."
    )
    @Schema(
        description = "Narrativa do objetivo operacional; pode conter taticas, restricoes e coordenadas -- resumo e governanca restringem exposicao a IA."
    )
    @UISchema(label = "Objetivo", controlType = FieldControlType.TEXTAREA, maxLength = 4000, icon = "flag")
    private String objetivo;

    @NotNull
    @DomainGovernance(
            kind = DomainGovernanceKind.SECURITY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.OPERATIONAL,
            complianceTags = {"INTERNAL_POLICY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.SUMMARIZE_ONLY, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.REVIEW_REQUIRED),
            reason = "Prioridade operacional da missao."
    )
    @Schema(description = "Nivel de urgencia operacional; afeta alocacao de recursos e SLAs de equipa.")
    @UISchema(label = "Prioridade", controlType = FieldControlType.SELECT, required = true, icon = "priority_high")
    private MissaoPrioridade prioridade;

    @NotNull
    @DomainGovernance(
            kind = DomainGovernanceKind.COMPLIANCE,
            classification = DomainClassification.INTERNAL,
            dataCategory = DomainDataCategory.LEGAL,
            complianceTags = {"INTERNAL_POLICY", "REGULATORY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.ALLOW, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.ALLOW),
            reason = "Status governado do ciclo operacional da missao."
    )
    @Schema(description = "Ponto do fluxo de vida da missao; governa acoes, licencas e publicacao em dashboards regulados.")
    @UISchema(label = "Status", controlType = FieldControlType.SELECT, required = true, icon = "toggle_on")
    private MissaoStatus status;

    @Size(max = 200)
    @DomainGovernance(
            kind = DomainGovernanceKind.SECURITY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.OPERATIONAL,
            complianceTags = {"INTERNAL_POLICY"},
            aiUsage = @AiUsagePolicy(visibility = AiUsageMode.SUMMARIZE_ONLY, trainingUse = AiUsageMode.DENY, ruleAuthoring = AiUsageMode.REVIEW_REQUIRED, reasoningUse = AiUsageMode.REVIEW_REQUIRED),
            reason = "Local operacional da missao."
    )
    @Schema(
        description = "Area ou endereco operacional; pode conter classificacoes nao publicas, devendo ser resumido em canais nao confiaveis."
    )
    @UISchema(label = "Local", controlType = FieldControlType.INPUT, maxLength = 200, icon = "location_on")
    private String local;

    @UISchema(label = "Ameaça", controlType = FieldControlType.SELECT,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.RiskIntelligence.AMEACAS + "/options/filter",
            tableHidden = true, icon = "warning")
    @Schema(
        description = "Referencia a ameaca de risco associada; enriquecimento e SLAs vêm do servico de risk intelligence."
    )
    private Integer ameacaId;

    @Schema(description = "Denominacao exibida da ameaca (somente leitura, join operacional).")
    @UISchema(label = "Ameaça", readOnly = true, formHidden = true, icon = "warning")
    private String ameacaNome;

    @Schema(description = "Janela planejada de inicio (timezone da aplicacao/tenant).")
    @UISchema(label = "Início Previsto", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "event")
    private OffsetDateTime inicioPrev;

    @Schema(description = "Janela planejada de termino, usada em planeamento de frota e folgas.")
    @UISchema(label = "Fim Previsto", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "event")
    private OffsetDateTime fimPrev;

    @Schema(description = "Timestamp efetivo de inicio, alimenta auditoria e relatorios de atraso.")
    @UISchema(label = "Início Real", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "event")
    private OffsetDateTime inicioReal;

    @Schema(description = "Encerramento operacional; necessario para gatilhos de faturamento e desmobilizacao de recursos.")
    @UISchema(label = "Fim Real", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "event")
    private OffsetDateTime fimReal;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public MissaoPrioridade getPrioridade() { return prioridade; }
    public void setPrioridade(MissaoPrioridade prioridade) { this.prioridade = prioridade; }
    public MissaoStatus getStatus() { return status; }
    public void setStatus(MissaoStatus status) { this.status = status; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public Integer getAmeacaId() { return ameacaId; }
    public void setAmeacaId(Integer ameacaId) { this.ameacaId = ameacaId; }
    public String getAmeacaNome() { return ameacaNome; }
    public void setAmeacaNome(String ameacaNome) { this.ameacaNome = ameacaNome; }
    public OffsetDateTime getInicioPrev() { return inicioPrev; }
    public void setInicioPrev(OffsetDateTime inicioPrev) { this.inicioPrev = inicioPrev; }
    public OffsetDateTime getFimPrev() { return fimPrev; }
    public void setFimPrev(OffsetDateTime fimPrev) { this.fimPrev = fimPrev; }
    public OffsetDateTime getInicioReal() { return inicioReal; }
    public void setInicioReal(OffsetDateTime inicioReal) { this.inicioReal = inicioReal; }
    public OffsetDateTime getFimReal() { return fimReal; }
    public void setFimReal(OffsetDateTime fimReal) { this.fimReal = fimReal; }
}
