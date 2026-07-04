package com.example.praxis.apiquickstart.hr.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(
        name = "AbsenceCoverageWorkflowResultDTO",
        description = "Resultado da decisao de cobertura de ausencia, com periodo afetado, plano aceito, substituto opcional e mensagem para atualizar cockpit, auditoria e UI.")
public class AbsenceCoverageWorkflowResultDTO {

    @Schema(description = "Identificador da ausencia cuja cobertura foi planejada.", example = "42")
    private Integer id;
    @Schema(description = "Tipo de ausencia em vigor no registro, como FERIAS ou AFASTAMENTO.", example = "FERIAS")
    private String tipo;
    @Schema(description = "Inicio da janela de indisponibilidade coberta.", example = "2026-07-01")
    private LocalDate dataInicio;
    @Schema(description = "Fim da janela de indisponibilidade coberta.", example = "2026-07-15")
    private LocalDate dataFim;
    @Schema(description = "Plano operacional aceito para cobrir a ausencia.")
    private String planoCobertura;
    @Schema(description = "Funcionario substituto informado na decisao, quando houver.", example = "12")
    private Integer substitutoFuncionarioId;
    @Schema(description = "Justificativa operacional ou de governanca enviada pelo operador.")
    private String justificativa;
    @Schema(description = "Observacoes persistidas depois da decisao; usadas como evidencias simples no host de referencia.")
    private String observacoes;
    @Schema(description = "Mensagem de sintese para operador, cockpit ou runtime.", example = "Cobertura da ausencia registrada.")
    private String mensagem;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public String getPlanoCobertura() { return planoCobertura; }
    public void setPlanoCobertura(String planoCobertura) { this.planoCobertura = planoCobertura; }
    public Integer getSubstitutoFuncionarioId() { return substitutoFuncionarioId; }
    public void setSubstitutoFuncionarioId(Integer substitutoFuncionarioId) { this.substitutoFuncionarioId = substitutoFuncionarioId; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
}
