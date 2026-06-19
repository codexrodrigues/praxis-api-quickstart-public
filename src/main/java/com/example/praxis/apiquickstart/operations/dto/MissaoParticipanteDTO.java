package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.PapelMissao;
import com.example.praxis.apiquickstart.operations.enums.ResultadoMissao;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "MissaoParticipanteDTO",
        description = "Escalacao de colaborador a missao (papel, ordem, lider, resultado alocado). "
                + "Representa a composicao da equipe de missao, incluindo funcao, lideranca e resultado individual.")
public class MissaoParticipanteDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @NotNull
    @UISchema(label = "Missão", controlType = FieldControlType.ENTITY_LOOKUP, required = true,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Operations.MISSOES_MISSION_LOOKUP_OPTIONS,
            tableHidden = true, icon = "flag")
    @Schema(
            description = "Missao operacional em que o colaborador foi escalado.")
    private Integer missaoId;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP, required = true,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            tableHidden = true, icon = "badge")
    @Schema(
            description = "Colaborador ou heroi escalado para participar da missao.")
    private Integer funcionarioId;

    @UISchema(label = "Missão", readOnly = true, formHidden = true, icon = "flag")
    @Schema(
            description = "Titulo denormalizado (read model).")
    private String missaoTitulo;

    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, icon = "badge")
    @Schema(
            description = "Nome do colaborador denormalizado (read model).")
    private String funcionarioNome;

    @UISchema(label = "Foto", tableHidden = true, formHidden = true)
    @Schema(
            description = "URL da foto de perfil do funcionário (read model).")
    private String funcionarioFotoUrl;

    @UISchema(label = "Papel", controlType = FieldControlType.SELECT, icon = "flag")
    @Schema(
            description = "Funcao tatica; PapelMissao.")
    private PapelMissao papel;

    @NotNull
    @UISchema(label = "Ordem", controlType = FieldControlType.NUMERIC_TEXT_BOX, required = true, icon = "flag")
    @Schema(
            description = "Sequencia de breafing/ ordem de insercao no teatro (0 = primeiro).")
    private Integer ordem = 0;

    @NotNull
    @UISchema(label = "Principal", controlType = FieldControlType.TOGGLE, required = true, icon = "toggle_on")
    @Schema(
            description = "VERDADEIRO se responsavel tatico/ face da missao para esta linha de escalonamento.")
    private Boolean principal = false;

    @UISchema(label = "Resultado", controlType = FieldControlType.SELECT, icon = "flag")
    @Schema(
            description = "Situacao alocada ao heroi; ResultadoMissao (apos debrief).")
    private ResultadoMissao resultado;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getMissaoId() { return missaoId; }
    public void setMissaoId(Integer missaoId) { this.missaoId = missaoId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getMissaoTitulo() { return missaoTitulo; }
    public void setMissaoTitulo(String missaoTitulo) { this.missaoTitulo = missaoTitulo; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public void setFuncionarioNome(String funcionarioNome) { this.funcionarioNome = funcionarioNome; }
    public String getFuncionarioFotoUrl() { return funcionarioFotoUrl; }
    public void setFuncionarioFotoUrl(String funcionarioFotoUrl) { this.funcionarioFotoUrl = funcionarioFotoUrl; }
    public PapelMissao getPapel() { return papel; }
    public void setPapel(PapelMissao papel) { this.papel = papel; }
    public Integer getOrdem() { return ordem; }
    public void setOrdem(Integer ordem) { this.ordem = ordem; }
    public Boolean getPrincipal() { return principal; }
    public void setPrincipal(Boolean principal) { this.principal = principal; }
    public ResultadoMissao getResultado() { return resultado; }
    public void setResultado(ResultadoMissao resultado) { this.resultado = resultado; }
}

