package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.annotation.AiUsageMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;

/**
 * DTO da identidade secreta/civil do heroi.
 *
 * <p>Traduz para o dominio ludico do quickstart a ideia de perfil publico:
 * codinome, universo e grau de exposicao vinculados ao mesmo funcionario.
 */
@Schema(
        name = "IdentidadeSecretaDTO",
        description = "Perfil de codinome e persona publica do colaborador, ligado 1-1 ao funcionario. "
                + "Distingue a persona publica (midia, missoes) do nome civil; combinar com governanca de privacidade e APIs que mascaram dados sensiveis.")
public class IdentidadeSecretaDTO {
    @Schema(description = "Chave do registo de identidade secreta. Cada heroi possui no maximo um bloco; usado em URLs e em telas de perfil *operativo*.", example = "1")
    private Integer id;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, required = true,
            tableHidden = true, helpText = "Colaborador associado ao perfil heroico.", icon = "badge")
    @Schema(
            description = "Colaborador dono do alter ego: identificador do `Funcionario` a quem o codinome e o universo se aplicam.",
            example = "2")
    private Integer funcionarioId;

    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, helpText = "Nome civil do colaborador (preenchido automaticamente).", icon = "badge")
    @Schema(description = "Nome civil (desnormalizado, leitura) para cruzar com o codinome em UI de auditoria interna; nao e o heroi em missao.")
    private String funcionarioNome;

    @NotBlank
    @Size(max = 120)
    @DomainGovernance(
        kind = DomainGovernanceKind.SECURITY,
        classification = DomainClassification.RESTRICTED,
        dataCategory = DomainDataCategory.OPERATIONAL,
        complianceTags = {"INTERNAL_POLICY", "SECURITY"},
        reason = "Vincular publicamente a identidade civil a este codinome compromete o sigilo e segurança operacional das missões.",
        aiUsage = @AiUsagePolicy(
            visibility = AiUsageMode.MASK,
            trainingUse = AiUsageMode.DENY,
            ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
            reasoningUse = AiUsageMode.DENY
        )
    )
    @UISchema(label = "Codinome", controlType = FieldControlType.INPUT, required = true, maxLength = 120, helpText = "Nome público (alter ego) do herói.", icon = "theater_comedy")
    @Schema(
            description = "Nome de guerra / codinome usado em comunicacao publica e relatorios de missao; nao substitui o CPF ou nome completo (RG/HR).",
            example = "Sombra Azul")
    private String codinome;

    @NotBlank
    @Size(max = 120)
    @UISchema(label = "Universo", controlType = FieldControlType.INPUT, required = true, maxLength = 120, helpText = "Universo narrativo de origem (ex: Terra-1).", icon = "public")
    @Schema(
            description = "Universo ou linha de origem usado para contextualizar a persona publica; livre, mas indexavel em catalogo narrativo e analitico.",
            example = "Terra-616")
    private String universo;

    @NotNull
    @UISchema(label = "Exposição Pública", helpText = "Permite a divulgação externa deste perfil.", icon = "visibility")
    @Schema(
            description = "Se o codinome e divulgaveis a midia e parceiros externos; false implica restringir a equipas internas e operacoes classificadas (politica de exibicao).",
            example = "true")
    private Boolean exposicaoPublica;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public void setFuncionarioNome(String funcionarioNome) { this.funcionarioNome = funcionarioNome; }
    public String getCodinome() { return codinome; }
    public void setCodinome(String codinome) { this.codinome = codinome; }
    public String getUniverso() { return universo; }
    public void setUniverso(String universo) { this.universo = universo; }
    public Boolean getExposicaoPublica() { return exposicaoPublica; }
    public void setExposicaoPublica(Boolean exposicaoPublica) { this.exposicaoPublica = exposicaoPublica; }
}
