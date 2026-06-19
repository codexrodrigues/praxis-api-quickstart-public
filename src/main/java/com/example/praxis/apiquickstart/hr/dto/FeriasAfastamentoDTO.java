package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.annotation.AiUsageMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;

import java.time.LocalDate;

@Schema(
        name = "FeriasAfastamentoDTO",
        description = "Periodo em que o colaborador esta em ferias, licenca ou afastamento: bloqueia ou reduz disponibilidade para missao e alimenta calendario de folha. "
                + "O tipo e texto livre neste contrato, mas representa a classificacao operacional que regras de RH podem normalizar em catalogo.")
public class FeriasAfastamentoDTO {
    @Schema(
            description = "Chave do periodo de ausencia. Cada registo e um intervalo; sobreposicoes de datas devem ser tratadas por regra de negocio fora do DTO.",
            example = "1")
    private Integer id;

    @NotBlank
    @Size(max = 100)
    @DomainGovernance(
        kind = DomainGovernanceKind.PRIVACY,
        classification = DomainClassification.RESTRICTED,
        dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
        complianceTags = {"LGPD", "GDPR"},
        reason = "O tipo de afastamento pode revelar informações médicas, como licenças de saúde, configurando dado sensível.",
        aiUsage = @AiUsagePolicy(
            visibility = AiUsageMode.MASK,
            trainingUse = AiUsageMode.DENY,
            ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
            reasoningUse = AiUsageMode.REVIEW_REQUIRED
        )
    )
    @UISchema(label = "Tipo", required = true, maxLength = 100, group = "Principal", order = 10, helpText = "Motivo da ausência (ex: Férias, Licença médica).", icon = "event_busy")
    @Schema(
            description = "Natureza do afastamento (ex. Ferias, Licenca saude, Suspensao disciplinar). Valor discursivo: impacta aprovacoes e folha consoante politica; nao e enum fechado aqui.",
            example = "Ferias")
    private String tipo;

    @NotNull
    @UISchema(label = "Data de Início", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, group = "Principal", order = 20, helpText = "Primeiro dia do afastamento.", icon = "event")
    @Schema(
            description = "Primeiro dia fora (inclusive) do regime normal de trabalho; usado com dataFim em calculos de cobertura e conflito com missao.",
            example = "2025-07-01")
    private LocalDate dataInicio;

    @NotNull
    @UISchema(label = "Data de Fim", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, group = "Principal", order = 30, helpText = "Último dia do afastamento.", icon = "event_available")
    @Schema(
            description = "Ultimo dia do afastamento (inclusive); apos essa data o colaborador retorna a disponibilidade operacional (salvo outro registo).",
            example = "2025-07-15")
    private LocalDate dataFim;

    @Size(max = 2000)
    @DomainGovernance(
        kind = DomainGovernanceKind.PRIVACY,
        classification = DomainClassification.RESTRICTED,
        dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
        complianceTags = {"LGPD", "GDPR"},
        reason = "Pode conter laudos, diagnósticos e transcrições de atestados médicos protegidos por sigilo. Não deve ser lido sem máscara por IA.",
        aiUsage = @AiUsagePolicy(
            visibility = AiUsageMode.MASK,
            trainingUse = AiUsageMode.DENY,
            ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
            reasoningUse = AiUsageMode.REVIEW_REQUIRED
        )
    )
    @UISchema(label = "Observações", controlType = FieldControlType.TEXTAREA, maxLength = 2000, group = "Principal", order = 40, helpText = "Detalhes e anotações de aprovação.", icon = "notes")
    @Schema(
            description = "Detalhe (substituto, aprovador, atestado, contacto) para apoio a RH; nao substitui documento comprobatorio armazenado a parte.",
            example = "Aprovado DRH em 12/06; substituicao: equipe B")
    private String observacoes;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP, group = "Relacionamentos", order = 10, icon = "badge",
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            helpText = "Colaborador em afastamento.")
    @Schema(
            description = "Colaborador afastado: FK ao `Funcionario` afetado pelo periodo; obrigatorio para vincular ausencia a pessoa certa.",
            example = "5")
    private Integer funcionarioId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
}
