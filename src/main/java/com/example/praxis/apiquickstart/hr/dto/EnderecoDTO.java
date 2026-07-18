package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.annotation.AiControlledUseMode;
import org.praxisplatform.uischema.annotation.AiTrainingUseMode;
import org.praxisplatform.uischema.annotation.AiVisibilityMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;

/**
 * DTO do endereco do funcionario.
 *
 * <p>Demonstra um caso classico de composicao cadastral em RH:
 * dados de localizacao persistidos em recurso proprio, mas fortemente ligados
 * ao colaborador dono do cadastro.
 */
@Schema(
        name = "EnderecoDTO",
        description = "Endereco residencial ou de correspondencia do colaborador. Persistido em recurso dedicado, ligado 1-N ao funcionario; "
                + "dados de localizacao sao sensiveis (LGPD): uso em folha, contacto de emergencia e logistica operacional, nao em dominios publicos sem consentimento.")
public class EnderecoDTO {
    @Schema(description = "Chave do endereco. Liga-se a um unico funcionario; atualizacoes substituem a visualizacao de lista por recurso sem historico de versao neste contrato.", example = "1")
    private Integer id;

    @NotBlank
    @DomainGovernance(
        kind = DomainGovernanceKind.PRIVACY,
        classification = DomainClassification.CONFIDENTIAL,
        dataCategory = DomainDataCategory.PERSONAL,
        complianceTags = {"LGPD", "PHYSICAL_SECURITY"},
        reason = "A via pública e localização de residência do colaborador oferecem riscos à segurança física e identificação (LGPD).",
        aiUsage = @AiUsagePolicy(
            visibility = AiVisibilityMode.MASK,
            trainingUse = AiTrainingUseMode.DENY,
            ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
            reasoningUse = AiControlledUseMode.REVIEW_REQUIRED
        )
    )
    @UISchema(label = "Logradouro", required = true, group = "Endereço", order = 10, helpText = "Rua, avenida ou alameda principal.", icon = "home")
    @Schema(
            description = "Via publica (rua, avenida, alameda) sem numero; alinhado a normas de correio para integracao e geocodificacao futura.",
            example = "Rua das Missoes")
    private String logradouro;

    @NotBlank
    @Size(max = 50)
    @UISchema(label = "Número", required = true, maxLength = 50, group = "Endereço", order = 20, helpText = "Número do imóvel.", icon = "pin")
    @Schema(description = "Numero do imovel, bloco ou lote; pode incluir letra (ex. 1001 A) conforme regra de tamanho maximo.", example = "100")
    private String numero;

    @UISchema(label = "Complemento", group = "Endereço", order = 30, helpText = "Apto, bloco ou ponto de referência.", icon = "pin")
    @Schema(
            description = "Apartamento, sala, ponto de referencia interna; opcional, mas evita duplicidade de entregas e visitas tecnicas.",
            example = "Apto 42")
    private String complemento;

    @NotBlank
    @UISchema(label = "Bairro", required = true, group = "Endereço", order = 40, helpText = "Região administrativa ou bairro.", icon = "location_city")
    @Schema(description = "Regiao administrativa local usada em rotas, correio e risco (incidente proximo).", example = "Nucleo 7")
    private String bairro;

    @NotBlank
    @UISchema(label = "Cidade", required = true, group = "Endereço", order = 50, helpText = "Município de residência.", icon = "location_city")
    @Schema(description = "Municipio ou cidade perante a UF; relevante para calculos fiscais, logistica e agregacoes de RH regionais.", example = "Metropolis")
    private String cidade;

    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "^[A-Z]{2}$", message = "UF deve conter 2 letras maiúsculas")
    @UISchema(label = "Estado", required = true, maxLength = 2, group = "Endereço", order = 60, helpText = "UF em duas letras maiúsculas (ex: SP, RJ).", icon = "map")
    @Schema(description = "Unidade federativa (BR) em duas letras maiusculas, conforme validacao; define jurisdicao e feriados locais (referencia).", example = "SP")
    private String estado;

    @NotBlank
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP deve ser no formato 00000-000")
    @DomainGovernance(
        kind = DomainGovernanceKind.PRIVACY,
        classification = DomainClassification.CONFIDENTIAL,
        dataCategory = DomainDataCategory.PERSONAL,
        complianceTags = {"LGPD", "PHYSICAL_SECURITY"},
        reason = "O CEP delimita uma área geográfica muito pequena que expõe a região de residência do colaborador.",
        aiUsage = @AiUsagePolicy(
            visibility = AiVisibilityMode.MASK,
            trainingUse = AiTrainingUseMode.DENY,
            ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED,
            reasoningUse = AiControlledUseMode.ALLOW
        )
    )
    @UISchema(label = "CEP", required = true, group = "Endereço", order = 70, helpText = "Código postal no formato 00000-000.", icon = "local_post_office")
    @Schema(description = "CEP brasileiro (8 digitos) com ou sem hifen; usado em validacoes de correio e normalizacao de rota.", example = "01310-100")
    private String cep;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP, group = "Relacionamentos", order = 10, icon = "badge",
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            tableHidden = true, helpText = "Colaborador residente.")
    @Schema(
            description = "Dono do endereco: identificador do colaborador (heroi) cujo registo de morada este bloco descreve.",
            example = "5")
    private Integer funcionarioId;

    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, group = "Relacionamentos", order = 11, helpText = "Nome do residente (preenchido automaticamente).", icon = "badge")
    @Schema(description = "Nome de exibicao do colaborador (desnormalizado, somente leitura) para listagens sem join adicional na UI.")
    private String funcionarioNome;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public void setFuncionarioNome(String funcionarioNome) { this.funcionarioNome = funcionarioNome; }
}
