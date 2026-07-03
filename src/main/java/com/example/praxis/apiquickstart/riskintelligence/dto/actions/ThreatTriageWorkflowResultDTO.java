package com.example.praxis.apiquickstart.riskintelligence.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ThreatTriageWorkflowResultDTO",
        description = "Resultado da triagem de ameaca, com status anterior, status atual, contexto de risco e mensagem operacional.")
public class ThreatTriageWorkflowResultDTO {

    @Schema(description = "Identificador da ameaca afetada pela decisao.")
    private Integer id;
    @Schema(description = "Nome ou designacao operacional da ameaca.")
    private String nome;
    @Schema(description = "Classe taxonomica da ameaca no catalogo de risco.")
    private String classe;
    @Schema(description = "Nivel de risco registrado no momento da decisao.")
    private Integer nivel;
    @Schema(description = "Status operacional antes da triagem.")
    private String statusAnterior;
    @Schema(description = "Status operacional apos a persistencia da triagem.")
    private String statusAtual;
    @Schema(description = "Justificativa registrada para explicar a decisao.")
    private String motivo;
    @Schema(description = "Mensagem amigavel para operador, cockpit e consumidores semanticos.")
    private String mensagem;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getClasse() {
        return classe;
    }

    public void setClasse(String classe) {
        this.classe = classe;
    }

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public String getStatusAnterior() {
        return statusAnterior;
    }

    public void setStatusAnterior(String statusAnterior) {
        this.statusAnterior = statusAnterior;
    }

    public String getStatusAtual() {
        return statusAtual;
    }

    public void setStatusAtual(String statusAtual) {
        this.statusAtual = statusAtual;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
