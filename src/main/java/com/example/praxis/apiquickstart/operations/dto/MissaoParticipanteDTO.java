package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.PapelMissao;
import com.example.praxis.apiquickstart.operations.enums.ResultadoMissao;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "MissaoParticipanteDTO",
        description = "Escala de um colaborador em uma missão. Representa a composição da equipe, "
                + "incluindo papel, ordem de atuação, liderança e resultado individual.")
public class MissaoParticipanteDTO {
    @Schema(description = "Identificador interno do vínculo no serviço; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @NotNull
    @UISchema(label = "Missão", controlType = FieldControlType.ENTITY_LOOKUP, required = true, order = 10,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Operations.MISSOES_MISSION_LOOKUP_OPTIONS,
            tableHidden = true, icon = "flag")
    @Schema(
            description = "Missão operacional em que o colaborador será escalado.")
    private Integer missaoId;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP, required = true, order = 20,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            tableHidden = true, icon = "badge")
    @Schema(
            description = "Colaborador escalado para participar da missão.")
    private Integer funcionarioId;

    @UISchema(label = "Missão", readOnly = true, formHidden = true, icon = "flag")
    @Schema(
            description = "Título da missão projetado para leitura sem nova consulta.")
    private String missaoTitulo;

    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, icon = "badge")
    @Schema(
            description = "Nome do colaborador projetado para leitura sem nova consulta.")
    private String funcionarioNome;

    @UISchema(label = "Foto", tableHidden = true, formHidden = true)
    @Schema(
            description = "URL da foto de perfil do funcionário (read model).")
    private String funcionarioFotoUrl;

    @UISchema(label = "Papel", controlType = FieldControlType.SELECT, icon = "flag", order = 30, width = "md")
    @Schema(
            description = "Função tática desempenhada pelo colaborador durante a missão.")
    private PapelMissao papel;

    @NotNull
    @UISchema(label = "Ordem", controlType = FieldControlType.NUMERIC_TEXT_BOX, required = true, icon = "flag", order = 40, width = "md")
    @Schema(
            description = "Ordem de atuação no plano da missão; zero representa a primeira posição.")
    private Integer ordem = 0;

    @NotNull
    @UISchema(label = "Principal", controlType = FieldControlType.TOGGLE, required = true, icon = "toggle_on", order = 50, width = "md")
    @Schema(
            description = "Indica se o colaborador é a principal referência tática desta missão.")
    private Boolean principal = false;

    @UISchema(label = "Resultado", controlType = FieldControlType.SELECT, icon = "flag", order = 60, width = "md")
    @Schema(
            description = "Resultado individual registrado após o encerramento ou debriefing da missão.")
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
