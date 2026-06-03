package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.math.BigDecimal;

@UISchema(label = "Ranking de Reputação", readOnly = true, icon = "leaderboard")
@Schema(
        name = "VwRankingReputacaoDTO",
        description = "Linha de vista so-leitura: ranking de reputacao do colaborador face a pares (midia vs governo, media e posicao). "
                + "Nao e entidade persistida; deriva de agregados e joins do demo. Serve listagens, dashboards e descricao semantica para consumo/LLM.")
public class VwRankingReputacaoDTO {
    @Schema(
            description = "Identificador do funcionario na base; chave de ligacao ao cadastro e ao restante dominio RH.",
            example = "3")
    @UISchema(label = "Cód. Herói", helpText = "Identificador interno do herói no ranking.", formHidden = true, icon = "badge")
    private Integer funcionarioId;

    @UISchema(label = "Nome Completo", helpText = "Nome do colaborador no ranking.", icon = "badge")
    @Schema(description = "Nome civil usado em documentos e contrato; coluna exibida no ranking.")
    private String nomeCompleto;

    @UISchema(label = "Codinome", helpText = "Identidade heroica (se existir).", icon = "theater_comedy")
    @Schema(description = "Identidade publica ou de missao vinculada ao mesmo funcionario; pode vazio se nao houver registo de alter ego.")
    private String codinome;

    @UISchema(label = "Equipe", helpText = "Equipe de vínculo principal.", icon = "groups")
    @Schema(description = "Rotulo da equipa operacional principal do heroi no contexto analitico (desnormalizado na vista).")
    private String equipe;

    @UISchema(label = "Score Público", type = FieldDataType.NUMBER, helpText = "Score público para comparação.", icon = "badge")
    @Schema(
            description = "Indice sintetico de visibilidade/sentimento de midia no periodo considerado pela vista (escala do quickstart).",
            example = "72")
    private Integer scorePublico;

    @UISchema(label = "Score Governamental", type = FieldDataType.NUMBER, helpText = "Score governamental para comparação.", icon = "gavel")
    @Schema(
            description = "Indice de alinhamento a politicas internas e missoes reguladas; independente do score publico.",
            example = "85")
    private Integer scoreGovernamental;

    @UISchema(label = "Média", type = FieldDataType.NUMBER, helpText = "Score médio composto para rankeamento.", icon = "analytics")
    @Schema(
            description = "Media ponderada ou composta usada para ordenar o ranking (regra da query; nao editavel pelo cliente).",
            example = "78.5")
    private BigDecimal media;

    @UISchema(label = "Posição", type = FieldDataType.NUMBER, helpText = "Posição geral no ranking calculado.", icon = "format_list_numbered")
    @Schema(
            description = "Posicao ordinal no ranking (1 = topo no criterio atual da consulta).",
            example = "1")
    private Long posicao;

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getCodinome() { return codinome; }
    public void setCodinome(String codinome) { this.codinome = codinome; }
    public String getEquipe() { return equipe; }
    public void setEquipe(String equipe) { this.equipe = equipe; }
    public Integer getScorePublico() { return scorePublico; }
    public void setScorePublico(Integer scorePublico) { this.scorePublico = scorePublico; }
    public Integer getScoreGovernamental() { return scoreGovernamental; }
    public void setScoreGovernamental(Integer scoreGovernamental) { this.scoreGovernamental = scoreGovernamental; }
    public BigDecimal getMedia() { return media; }
    public void setMedia(BigDecimal media) { this.media = media; }
    public Long getPosicao() { return posicao; }
    public void setPosicao(Long posicao) { this.posicao = posicao; }
}
