package com.example.praxis.apiquickstart.hr.dto;

import com.example.praxis.apiquickstart.hr.enums.EstadoCivil;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.annotation.AiUsageMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contrato principal de funcionario no dominio de RH.
 *
 * <p>Este DTO concentra os campos usados pelo CRUD base, pelos formulários de edicao
 * e pelas listagens com contexto organizacional. Ele combina dados cadastrais,
 * dados profissionais e relacionamentos por id que depois sao resolvidos em nomes
 * legiveis para a UI.
 */
@Schema(
    name = "FuncionarioDTO",
    description = "Cadastro de colaborador no universo de RH: identificacao civil, contato, remuneracao, "
        + "vinculo (cargo, departamento) e sinalizadores operacionais. Valores pessoais e financeiros seguem governanca de dados (LGPD)."
)
public class FuncionarioDTO {
    @Schema(description = "Identificador interno do colaborador no servico de RH.", example = "1")
    private Integer id;

    @UISchema(
        label = "Foto",
        type = FieldDataType.URL,
        controlType = FieldControlType.AVATAR,
        group = "Identificação",
        order = 5,
        icon = "account_circle",
        description = "Pré-visualização da foto do perfil",
        readOnly = true
    )
    @Schema(
        description = "Foto de perfil exibida como avatar; derivada da URL de foto ou placeholder."
    )
    private String avatarUrl;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Nome civil completo usado em documentos oficiais e exibicao em listagens.", example = "Maria Souza")
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
            visibility = AiUsageMode.MASK,
            trainingUse = AiUsageMode.DENY,
            ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
            reasoningUse = AiUsageMode.REVIEW_REQUIRED
        ),
        reason = "Documento pessoal usado para identificacao fiscal do colaborador."
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
    @Schema(
        description = "Cadastro de Pessoa Fisica (CPF) do colaborador. Dado pessoal sensivel; uso condicionado a finalidades de RH, folha e conformidade."
    )
    private String cpf;

    @NotNull
    @Past
    @DomainGovernance(
        kind = DomainGovernanceKind.PRIVACY,
        classification = DomainClassification.CONFIDENTIAL,
        dataCategory = DomainDataCategory.PERSONAL,
        complianceTags = {"LGPD", "GDPR"},
        aiUsage = @AiUsagePolicy(
            visibility = AiUsageMode.MASK,
            trainingUse = AiUsageMode.DENY,
            ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
            reasoningUse = AiUsageMode.REVIEW_REQUIRED
        ),
        reason = "Data de nascimento usada para identificacao pessoal."
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
    @Schema(
        description = "Data de nascimento do colaborador; impacta regras de idade, beneficios e, quando aplicavel, validacoes de elegibilidade."
    )
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
            visibility = AiUsageMode.MASK,
            trainingUse = AiUsageMode.DENY,
            ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
            reasoningUse = AiUsageMode.REVIEW_REQUIRED
        ),
        reason = "Contato pessoal do colaborador."
    )
    @Schema(description = "Endereco de e-mail corporativo ou pessoal usado para notificacoes e recuperacao de conta.")
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
            visibility = AiUsageMode.MASK,
            trainingUse = AiUsageMode.DENY,
            ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
            reasoningUse = AiUsageMode.REVIEW_REQUIRED
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
    @Schema(
        description = "Telefone de contato em formato E.164 ou nacional; usado para SMS ou validacao de identidade leve em fluxos de RH."
    )
    private String telefone;

    @NotNull
    @DecimalMin("0.00")
    @DomainGovernance(
        kind = DomainGovernanceKind.PRIVACY,
        classification = DomainClassification.CONFIDENTIAL,
        dataCategory = DomainDataCategory.FINANCIAL,
        complianceTags = {"LGPD", "INTERNAL_POLICY"},
        aiUsage = @AiUsagePolicy(
            visibility = AiUsageMode.MASK,
            trainingUse = AiUsageMode.DENY,
            ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
            reasoningUse = AiUsageMode.ALLOW
        ),
        reason = "Remuneracao individual do colaborador."
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
        helpText = "Remuneração base atual."
    )
    @Schema(
        description = "Remuneracao base ou contratual referente ao periodo; dado financeiro e pessoal, sujeito a controle de acesso e politicas internas."
    )
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
    @Schema(
        description = "Data de inicio do vinculo empregaticio; ancora requisitos de experiencia, ferias e historicos de folha."
    )
    private LocalDate dataAdmissao;

    @NotNull
    @Schema(description = "Indica se o colaborador esta ativo no cadastro; inativos podem ser retidos por auditoria sem aparecer em operacoes de rotina.")
    @UISchema(label = "Ativo", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, group = "Profissional", order = 30, helpText = "Indica se o colaborador está ativo no sistema.", icon = "toggle_on")
    private Boolean ativo;

    @NotNull
    @UISchema(label = "Cargo", controlType = FieldControlType.SELECT, group = "Profissional", order = 40, icon = "work",
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.CARGOS + "/options/filter",
            tableHidden = true, helpText = "Cargo ocupado atualmente.")
    @Schema(description = "Referencia ao cargo atualmente atribuido; define faixa, competencias e relatorios de RH associados.", example = "1")
    private Integer cargoId;

    @NotNull
    @UISchema(label = "Departamento", controlType = FieldControlType.SELECT, group = "Profissional", order = 50, icon = "apartment",
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.DEPARTAMENTOS + "/options/filter",
            tableHidden = true, helpText = "Unidade organizacional alocada.")
    @Schema(
        description = "Unidade organizacional a qual o colaborador esta alocado; usada em aprovacoes, custeio e visibilidade de dados."
    )
    private Integer departamentoId;



    // Campos somente para exibição na grade (legibilidade em listagens)
    @Schema(description = "Denominacao resolvida do cargo para exibicao em tabelas (somente leitura).")
    @UISchema(label = "Cargo", readOnly = true, formHidden = true, group = "Profissional", order = 41, helpText = "Nome do cargo (preenchido automaticamente).", icon = "work")
    private String cargoNome;

    @Schema(description = "Nome resolvido do departamento para exibicao em tabelas (somente leitura).")
    @UISchema(label = "Departamento", readOnly = true, formHidden = true, group = "Profissional", order = 51, helpText = "Nome do departamento (preenchido automaticamente).", icon = "apartment")
    private String departamentoNome;

    @Size(max = 300)
    @Schema(description = "URL completa da imagem de perfil armazenada; pode ser restringida por politica de conteudo e privacidade.")
    @UISchema(label = "Foto (URL)", type = FieldDataType.URL, maxLength = 300, group = "Identificação", order = 40, tableHidden = true, formHidden = true, helpText = "URL completa da foto do colaborador.", icon = "link")
    private String fotoPerfilUrl;

    @Schema(description = "Situacao civil informada para beneficios, dependentes e, quando exigido, relatorios regulatorios.")
    @UISchema(label = "Estado Civil", controlType = FieldControlType.SELECT, group = "Identificação", order = 35, helpText = "Situação civil atual.", icon = "family_restroom")
    private EstadoCivil estadoCivil;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
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
    public String getCargoNome() { return cargoNome; }
    public void setCargoNome(String cargoNome) { this.cargoNome = cargoNome; }
    public String getDepartamentoNome() { return departamentoNome; }
    public void setDepartamentoNome(String departamentoNome) { this.departamentoNome = departamentoNome; }
    public String getFotoPerfilUrl() { return fotoPerfilUrl; }
    public void setFotoPerfilUrl(String fotoPerfilUrl) { this.fotoPerfilUrl = fotoPerfilUrl; }
    public EstadoCivil getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(EstadoCivil estadoCivil) { this.estadoCivil = estadoCivil; }
}
