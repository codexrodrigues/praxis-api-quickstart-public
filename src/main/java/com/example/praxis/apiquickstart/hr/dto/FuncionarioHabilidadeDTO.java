package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/**
 * DTO de associacao entre funcionario e habilidade.
 *
 * <p>A skill nao vive apenas no catalogo: proficiencia e origem pertencem
 * ao vinculo entre a pessoa e a habilidade.
 */
@Schema(
        name = "FuncionarioHabilidadeDTO",
        description = "Vinculo N-N entre colaborador e entrada do catalogo de habilidades. A proficiencia e a origem pertencem ao vinculo (nao ao catalogo): "
                + "usado em alocacao a missao, relatorios de equipa e matriz de competencias (demo).")
public class FuncionarioHabilidadeDTO {
    @Schema(
            description = "Chave do vinculo. Cada par (funcionario, habilidade) pode aparecer no maximo uma vez; listagens de skill do heroi leem deste recurso.",
            example = "1")
    private Integer id;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.SELECT, required = true, icon = "badge",
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter",
            tableHidden = true, helpText = "Colaborador associado à habilidade.")
    @Schema(
            description = "Colaborador possuidor da proficiencia: referencia o recurso Funcionario (heroi) que acumula esta habilidade.",
            example = "2")
    private Integer funcionarioId;

    @NotNull
    @UISchema(label = "Habilidade", controlType = FieldControlType.SELECT, required = true, icon = "psychology",
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.HABILIDADES + "/options/filter",
            tableHidden = true, helpText = "Habilidade do catálogo a ser vinculada.")
    @Schema(
            description = "Ponte para o registo de HabilidadeDTO no catalogo; define qual competencia esta a ser classificada para este colaborador.",
            example = "7")
    private Integer habilidadeId;

    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, helpText = "Nome do colaborador (preenchido automaticamente).", icon = "badge")
    @Schema(description = "Nome do colaborador (desnormalizado, leitura) para tabelas de competencias sem join extra.")
    private String funcionarioNome;

    @UISchema(label = "Habilidade", readOnly = true, formHidden = true, helpText = "Nome da habilidade (preenchido automaticamente).", icon = "psychology")
    @Schema(description = "Nome da habilidade (desnormalizado, leitura) alinhada ao habilidadeId.")
    private String habilidadeNome;

    @NotNull
    @Min(0)
    @Max(10)
    @UISchema(label = "Proficiência", type = FieldDataType.NUMBER, required = true, helpText = "Nível de domínio do colaborador (escala de 0 a 10).", icon = "workspace_premium")
    @Schema(
            description = "Escala 0-10 de dominio *deste* colaborador sobre a habilidade; distinta do nivelPoder do catalogo (potencia intrinseca do poder).",
            example = "8")
    private Integer proficiencia;

    @Size(max = 120)
    @UISchema(label = "Origem", controlType = FieldControlType.INPUT, maxLength = 120, helpText = "Onde a habilidade foi adquirida (ex: curso, certificação).", icon = "school")
    @Schema(
            description = "Proveniencia do treino ou certificacao (ex. Academia, missao 42); texto livre para auditoria e credito de competencia (demo).",
            example = "Academia de Defesa Metropolitana")
    private String origem;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public Integer getHabilidadeId() { return habilidadeId; }
    public void setHabilidadeId(Integer habilidadeId) { this.habilidadeId = habilidadeId; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public void setFuncionarioNome(String funcionarioNome) { this.funcionarioNome = funcionarioNome; }
    public String getHabilidadeNome() { return habilidadeNome; }
    public void setHabilidadeNome(String habilidadeNome) { this.habilidadeNome = habilidadeNome; }
    public Integer getProficiencia() { return proficiencia; }
    public void setProficiencia(Integer proficiencia) { this.proficiencia = proficiencia; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
}
