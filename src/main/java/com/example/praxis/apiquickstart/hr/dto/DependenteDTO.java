package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.AiControlledUseMode;
import org.praxisplatform.uischema.annotation.AiTrainingUseMode;
import org.praxisplatform.uischema.annotation.AiVisibilityMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;

import java.time.LocalDate;

/**
 * DTO do dependente vinculado ao funcionario.
 *
 * <p>Representa o recorte familiar usado pelo dominio de RH para beneficios,
 * elegibilidade e contexto pessoal do colaborador.
 */
@Schema(
        name = "DependenteDTO",
        description = "Membro da familia ou outro vinculo declarado associado a um colaborador no dominio de RH. "
                + "Sustenta cenarios de beneficios, elegibilidade, IR e registo civil; tratar com o mesmo cuidado de dados pessoais (LGPD) que o titular.")
public class DependenteDTO {
    @Schema(
            description = "Chave do registo de dependente. Vinculada a um unico titular (funcionario); usada em URLs e listagens de beneficiarios.",
            example = "1")
    private Integer id;

    @NotBlank
    @Size(max = 200)
    @DomainGovernance(
        kind = DomainGovernanceKind.PRIVACY,
        classification = DomainClassification.RESTRICTED,
        dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
        complianceTags = {"LGPD", "GDPR"},
        reason = "Nome civil do dependente. Identificador Pessoal (PII) sujeito a proteção legal de menores ou cônjuges.",
        aiUsage = @AiUsagePolicy(
            visibility = AiVisibilityMode.MASK,
            trainingUse = AiTrainingUseMode.DENY,
            ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
            reasoningUse = AiControlledUseMode.REVIEW_REQUIRED
        )
    )
    @UISchema(label = "Nome Completo", controlType = FieldControlType.INPUT, required = true, maxLength = 200, group = "Identificação", order = 10, helpText = "Nome civil completo do dependente.", icon = "person")
    @Schema(
            description = "Nome civil usado em declaracoes de imposto, plano de saude e contactos de emergencia; nao e pseudonimo de identidade secreta do heroi titular.",
            example = "Ana Soares")
    private String nomeCompleto;

    @NotBlank
    @Size(max = 100)
    @UISchema(label = "Parentesco", controlType = FieldControlType.INPUT, required = true, maxLength = 100, group = "Identificação", order = 20, helpText = "Grau de parentesco (ex: filho, cônjuge).", icon = "family_restroom")
    @Schema(
            description = "Grau de parentesco ou dependencia (ex. conjuge, filha, enteado) usado em regras de desconto e comprovacao; texto livre sujeito a validacao de negocio fora do DTO.",
            example = "filha")
    private String parentesco;

    @NotNull
    @Past
    @DomainGovernance(
        kind = DomainGovernanceKind.PRIVACY,
        classification = DomainClassification.RESTRICTED,
        dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
        complianceTags = {"LGPD", "GDPR"},
        reason = "Data de nascimento do dependente. Dado usado para inferir idade e elegibilidade; requer mascaramento.",
        aiUsage = @AiUsagePolicy(
            visibility = AiVisibilityMode.MASK,
            trainingUse = AiTrainingUseMode.DENY,
            ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
            reasoningUse = AiControlledUseMode.REVIEW_REQUIRED
        )
    )
    @UISchema(label = "Data de Nascimento", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, group = "Identificação", order = 30, helpText = "Data de nascimento para cálculo de idade.", icon = "cake")
    @Schema(
            description = "Data de nascimento do dependente: base para idade, elegibilidade de auxilio-escola e, em declaracoes, idade fim de dependencia (referencia, nao regra legal automatica).",
            example = "2016-04-20")
    private LocalDate dataNascimento;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP, group = "Relacionamentos", order = 10, icon = "badge",
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            tableHidden = true, helpText = "Colaborador titular vinculado ao dependente.")
    @Schema(
            description = "Colaborador titular do vinculo familiar: o dependente liga-se a este `funcionarioId` (heroi) para beneficios e relatorios.",
            example = "3")
    private Integer funcionarioId;

    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, group = "Relacionamentos", order = 11, helpText = "Nome do titular (preenchido automaticamente).", icon = "badge")
    @Schema(description = "Nome do titular (desnormalizado, somente leitura) para exibicao em tabelas sem lookup extra.")
    private String funcionarioNome;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public void setFuncionarioNome(String funcionarioNome) { this.funcionarioNome = funcionarioNome; }
}
