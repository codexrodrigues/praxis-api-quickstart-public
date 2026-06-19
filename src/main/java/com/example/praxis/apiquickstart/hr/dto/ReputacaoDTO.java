package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.OffsetDateTime;

/**
 * DTO do snapshot reputacional do funcionario.
 *
 * <p>Concentra os scores sinteticos usados pelas views analiticas e
 * pelos rankings, mantendo a data de atualizacao como pista de frescor do dado.
 */
@Schema(
        name = "ReputacaoDTO",
        description = "Snapshot agregado de reputacao do colaborador: indicadores sinteticos (midia, confianca publica) alimentam rankings e views analiticas. "
                + "Nao e veredicto legal; actualizado em batch ou por job conforme atualizadoEm.")
public class ReputacaoDTO {
    @Schema(
            description = "Chave do registo de reputacao. Em geral 1-1 com funcionario; exposta em APIs de perfil e analise.",
            example = "1")
    private Integer id;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, required = true,
            tableHidden = true, helpText = "Colaborador avaliado.", icon = "badge")
    @Schema(
            description = "Colaborador a quem os scores se referem; FK ao Funcionario. Sem este id o snapshot nao tem dono.",
            example = "3")
    private Integer funcionarioId;

    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, helpText = "Nome do colaborador (preenchido automaticamente).", icon = "badge")
    @Schema(description = "Nome para listagens de ranking e dashboards sem lookup adicional; espelha o funcionarioId.")
    private String funcionarioNome;

    @UISchema(label = "Score Público", type = FieldDataType.NUMBER, helpText = "Índice de reputação junto ao público (0 a 100).", icon = "analytics")
    @Schema(
            description = "Indice derivado de visibilidade e sentimento de midia; escala e pesos pertencem ao modelo operacional do quickstart, nao a auditoria externa.",
            example = "72")
    private Integer scorePublico;

    @UISchema(label = "Score Governamental", type = FieldDataType.NUMBER, helpText = "Índice de alinhamento com regras governamentais.", icon = "gavel")
    @Schema(
            description = "Indice de alinhamento a politicas internas e compliance em missoes; separado do score publico para confrontar risco operacional vs imagem.",
            example = "88")
    private Integer scoreGovernamental;

    @UISchema(label = "Atualizado Em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, helpText = "Data da última atualização dos scores.", icon = "event")
    @Schema(
            description = "Momento (com offset) da ultima recomputacao dos scores; uso para frescor do dado e caches de UI.",
            example = "2025-04-20T14:30:00Z")
    private OffsetDateTime atualizadoEm;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public void setFuncionarioNome(String funcionarioNome) { this.funcionarioNome = funcionarioNome; }
    public Integer getScorePublico() { return scorePublico; }
    public void setScorePublico(Integer scorePublico) { this.scorePublico = scorePublico; }
    public Integer getScoreGovernamental() { return scoreGovernamental; }
    public void setScoreGovernamental(Integer scoreGovernamental) { this.scoreGovernamental = scoreGovernamental; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(OffsetDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
