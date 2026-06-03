package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "DepartamentoDTO",
        description = "Unidade organizacional no demo de RH: agrupa lotacoes e reporting. O codigo curto identifica o departamento em integracoes; "
                + "o responsavel e o heroi de referencia para aprovacoes operacionais dentro do escopo do departamento.")
public class DepartamentoDTO {
    @Schema(
            description = "Chave do departamento. Referenciada por colaboradores (lotacao), cargos e consultas; exposta em URLs REST.",
            example = "1")
    private Integer id;

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Nome", controlType = FieldControlType.INPUT, required = true, maxLength = 200, group = "Principal", order = 10, helpText = "Nome oficial do departamento.", icon = "apartment")
    @Schema(
            description = "Designacao oficial do departamento (ex. Contingente Metropolitano, Inteligencia de Ameacas) para listagens e organogramas.",
            example = "Operacoes Estrategicas")
    private String nome;

    @NotBlank
    @Size(max = 20)
    @UISchema(label = "Código", required = true, maxLength = 20, group = "Principal", order = 20, tableHidden = true, helpText = "Sigla ou código interno para integração.", icon = "tag")
    @Schema(
            description = "Codigo interno estavel (sigla ou slug) para relatorios, filtros e referencias cruzadas; distinto do nome longo.",
            example = "OPS-EST")
    private String codigo;

    @UISchema(label = "Responsável", controlType = org.praxisplatform.uischema.FieldControlType.SELECT, group = "Relacionamentos", order = 10, icon = "supervisor_account",
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter",
            tableHidden = true, helpText = "Líder responsável pelas aprovações.")
    @Schema(
            description = "Identificador do funcionario (heroi) responsavel pelo departamento: aprovacoes de alocacao e ponto de contacto operacional.")
    private Integer responsavelId;

    @UISchema(label = "Responsável", readOnly = true, formHidden = true, group = "Relacionamentos", order = 11, helpText = "Nome do responsável (preenchido automaticamente).", icon = "supervisor_account")
    @Schema(
            description = "Nome de exibicao do responsavel (desnormalizado, somente leitura), resolvido pelo backend para a UI e listagens.")
    private String responsavelNome;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Integer getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Integer responsavelId) { this.responsavelId = responsavelId; }
    public String getResponsavelNome() { return responsavelNome; }
    public void setResponsavelNome(String responsavelNome) { this.responsavelNome = responsavelNome; }
}
