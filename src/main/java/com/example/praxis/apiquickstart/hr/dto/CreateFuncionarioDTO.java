package com.example.praxis.apiquickstart.hr.dto;

import com.example.praxis.apiquickstart.hr.enums.EstadoCivil;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.annotation.AiControlledUseMode;
import org.praxisplatform.uischema.annotation.AiTrainingUseMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.AiVisibilityMode;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Comando de criação de funcionário.
 *
 * <p>O contrato declara somente os valores aceitos pela operação de cadastro. Identidade,
 * versão, ETag e projeções resolvidas para leitura pertencem a {@link FuncionarioDTO} e não são
 * herdadas pelo request.</p>
 */
@Schema(
        name = "CreateFuncionarioDTO",
        description = "Comando para cadastrar um colaborador com identificação civil, contato, vínculo organizacional e remuneração inicial. Identidade técnica, versão e projeções de leitura são produzidas pelo servidor."
)
public class CreateFuncionarioDTO {

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Nome civil completo usado em documentos oficiais e exibição em listagens.", example = "Maria Souza")
    @UISchema(label = "Nome Completo", required = true, maxLength = 200, group = "Identificação", order = 10, helpText = "Nome civil completo do colaborador.", icon = "badge")
    private String nomeCompleto;

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11})$", message = "CPF inválido")
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.PERSONAL,
            complianceTags = {"LGPD", "GDPR"},
            aiUsage = @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY,
                    ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
                    reasoningUse = AiControlledUseMode.REVIEW_REQUIRED
            ),
            reason = "Documento pessoal usado para identificação fiscal do colaborador."
    )
    @UISchema(
            label = "CPF",
            required = true,
            maxLength = 20,
            group = "Identificação",
            order = 20,
            icon = "fingerprint",
            controlType = FieldControlType.CPF_CNPJ_INPUT,
            mask = "000.000.000-00",
            tableHidden = true,
            extraProperties = {
                    @ExtensionProperty(name = "documentType", value = "cpf"),
                    @ExtensionProperty(name = "allowFormattedInput", value = "true")
            },
            helpText = "Documento no formato 000.000.000-00."
    )
    @Schema(description = "Cadastro de Pessoa Física usado para identificação fiscal do colaborador, sujeito às finalidades e políticas de privacidade do domínio de RH.")
    private String cpf;

    @NotNull
    @Past
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.PERSONAL,
            complianceTags = {"LGPD", "GDPR"},
            aiUsage = @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY,
                    ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
                    reasoningUse = AiControlledUseMode.REVIEW_REQUIRED
            ),
            reason = "Data de nascimento usada para identificação pessoal."
    )
    @UISchema(
            label = "Data de Nascimento",
            type = FieldDataType.DATE,
            controlType = FieldControlType.DATE_PICKER,
            group = "Identificação",
            order = 30,
            icon = "cake",
            mask = "dd/MM/yyyy",
            numericFormat = NumericFormat.DATE,
            tableHidden = true,
            extraProperties = {
                    @ExtensionProperty(name = "locale", value = "pt-BR"),
                    @ExtensionProperty(name = "displayFormat", value = "dd/MM/yyyy")
            },
            helpText = "Data de nascimento oficial do colaborador."
    )
    @Schema(description = "Data de nascimento do colaborador; participa de regras de idade, benefícios e elegibilidade.")
    private LocalDate dataNascimento;

    @NotBlank
    @Email
    @Size(max = 200)
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.PERSONAL,
            complianceTags = {"LGPD", "GDPR"},
            aiUsage = @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY,
                    ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
                    reasoningUse = AiControlledUseMode.REVIEW_REQUIRED
            ),
            reason = "Contato pessoal do colaborador."
    )
    @Schema(description = "Endereço de e-mail corporativo ou pessoal usado para notificações e recuperação de conta.")
    @UISchema(label = "Email", type = FieldDataType.EMAIL, maxLength = 200, group = "Contato", order = 10, tableHidden = true, helpText = "Endereço de e-mail de contato.", icon = "email")
    private String email;

    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^\\+?\\d{8,15}$", message = "Telefone inválido (use formato E.164: +5581999999999)")
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.PERSONAL,
            complianceTags = {"LGPD", "GDPR"},
            aiUsage = @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY,
                    ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
                    reasoningUse = AiControlledUseMode.REVIEW_REQUIRED
            ),
            reason = "Canal de contato pessoal do colaborador."
    )
    @UISchema(
            label = "Telefone",
            maxLength = 30,
            group = "Contato",
            order = 20,
            controlType = FieldControlType.PHONE,
            icon = "phone",
            mask = "+55 (00) 00000-0000",
            tableHidden = true,
            extraProperties = {
                    @ExtensionProperty(name = "phoneFormat", value = "international"),
                    @ExtensionProperty(name = "defaultCountry", value = "BR"),
                    @ExtensionProperty(name = "autoFormat", value = "true")
            },
            helpText = "Telefone com DDD (ex: +55 11 99999-9999)."
    )
    @Schema(description = "Telefone de contato em formato E.164 ou nacional usado nos fluxos operacionais de RH.")
    private String telefone;

    @NotNull
    @DecimalMin("0.00")
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.CONFIDENTIAL,
            dataCategory = DomainDataCategory.FINANCIAL,
            complianceTags = {"LGPD", "INTERNAL_POLICY"},
            aiUsage = @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY,
                    ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
                    reasoningUse = AiControlledUseMode.ALLOW
            ),
            reason = "Remuneração individual do colaborador."
    )
    @UISchema(
            label = "Salário",
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.CURRENCY_INPUT,
            group = "Profissional",
            order = 10,
            icon = "payments",
            numericFormat = NumericFormat.CURRENCY,
            min = "0.00",
            tableHidden = true,
            extraProperties = {
                    @ExtensionProperty(name = "currency", value = "BRL"),
                    @ExtensionProperty(name = "locale", value = "pt-BR"),
                    @ExtensionProperty(name = "decimalPlaces", value = "2")
            },
            helpText = "Remuneração base informada pelo comando."
    )
    @Schema(description = "Remuneração base solicitada pelo comando; dado financeiro sujeito a controle de acesso.")
    private BigDecimal salario;

    @NotNull
    @UISchema(
            label = "Data de Admissão",
            type = FieldDataType.DATE,
            controlType = FieldControlType.DATE_PICKER,
            group = "Profissional",
            order = 20,
            icon = "event_available",
            mask = "dd/MM/yyyy",
            numericFormat = NumericFormat.DATE,
            extraProperties = {
                    @ExtensionProperty(name = "locale", value = "pt-BR"),
                    @ExtensionProperty(name = "displayFormat", value = "dd/MM/yyyy")
            },
            helpText = "Data de início do vínculo empregatício."
    )
    @Schema(description = "Data de início do vínculo empregatício; ancora férias, históricos e requisitos de experiência.")
    private LocalDate dataAdmissao;

    @NotNull
    @Schema(description = "Estado solicitado para o vínculo do colaborador.")
    @UISchema(label = "Ativo", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, group = "Profissional", order = 30, helpText = "Indica se o vínculo deve ficar ativo no sistema.", icon = "toggle_on")
    private Boolean ativo;

    @NotNull
    @UISchema(label = "Cargo", controlType = FieldControlType.SELECT, group = "Profissional", order = 40, icon = "work",
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_OPTIONS,
            tableHidden = true, helpText = "Cargo atribuído pelo comando.")
    @Schema(description = "Referência ao cargo atribuído ao colaborador.", example = "1")
    private Integer cargoId;

    @NotNull
    @UISchema(label = "Departamento", controlType = FieldControlType.SELECT, group = "Profissional", order = 50, icon = "apartment",
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.DEPARTAMENTOS_DEPARTMENT_LOOKUP_OPTIONS,
            tableHidden = true, helpText = "Unidade organizacional atribuída pelo comando.")
    @Schema(description = "Unidade organizacional à qual o colaborador será alocado.")
    private Integer departamentoId;

    @Size(max = 300)
    @Schema(description = "URL completa da imagem de perfil armazenada, quando disponível.")
    @UISchema(label = "Foto (URL)", type = FieldDataType.URL, maxLength = 300, group = "Identificação", order = 40, tableHidden = true, formHidden = true, helpText = "URL completa da foto do colaborador.", icon = "link")
    private String fotoPerfilUrl;

    @Schema(description = "Situação civil informada para benefícios, dependentes e relatórios que a exijam.")
    @UISchema(label = "Estado Civil", controlType = FieldControlType.SELECT, group = "Identificação", order = 35, helpText = "Situação civil atual.", icon = "family_restroom")
    private EstadoCivil estadoCivil;

    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public BigDecimal getSalario() { return salario; }
    public void setSalario(BigDecimal salario) { this.salario = salario; }
    public LocalDate getDataAdmissao() { return dataAdmissao; }
    public void setDataAdmissao(LocalDate dataAdmissao) { this.dataAdmissao = dataAdmissao; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public Integer getCargoId() { return cargoId; }
    public void setCargoId(Integer cargoId) { this.cargoId = cargoId; }
    public Integer getDepartamentoId() { return departamentoId; }
    public void setDepartamentoId(Integer departamentoId) { this.departamentoId = departamentoId; }
    public String getFotoPerfilUrl() { return fotoPerfilUrl; }
    public void setFotoPerfilUrl(String fotoPerfilUrl) { this.fotoPerfilUrl = fotoPerfilUrl; }
    public EstadoCivil getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(EstadoCivil estadoCivil) { this.estadoCivil = estadoCivil; }
}
