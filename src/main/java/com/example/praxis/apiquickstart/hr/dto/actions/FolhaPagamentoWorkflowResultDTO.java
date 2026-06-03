package com.example.praxis.apiquickstart.hr.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Resultado canonico devolvido pelos workflows de item da folha.
 *
 * <p>Ele resume a transicao de estado em termos compreensiveis para UI, auditoria e clientes
 * semanticos: estado anterior, estado atual, justificativa, contadores operacionais e mensagem
 * final.</p>
 */
@Schema(
        name = "FolhaPagamentoWorkflowResultDTO",
        description = "Resposta de workflow sobre item (ou lote) de folha: resume transicao de estado, justificativa usada, contadores e mensagem humana. "
                + "Serve a UI, auditoria e clientes que consomem o mesmo payload sem inferir a maquina de estados a partir de codigos HTTP (demo).")
public class FolhaPagamentoWorkflowResultDTO {

    @Schema(
            description = "Chave de contexto devolvida pelo servico: tipicamente id do evento de folha ou do cabecalho, conforme a action; usada para rastreio e refresh de tela.",
            example = "42")
    private Integer id;
    @Schema(
            description = "Estado de workflow anterior a esta transicao (ex.: PENDENTE, EM_ANALISE); string do catalogo de estados do quickstart, nao enumeracao OpenAPI fixa.",
            example = "PENDENTE")
    private String estadoAnterior;
    @Schema(
            description = "Novo estado apos a operacao; confirma sucesso idempotente quando igual ao pedido re-repetido e pode divergir se regra rejeitou (demo).",
            example = "APROVADO")
    private String estadoAtual;
    @Schema(
            description = "Ecoa ou complementa a justificativa do pedido; preenchimento depende do handler (pode vazio se o fluxo nao a persistir de volta).")
    private String justificativa;
    @Schema(
            description = "Quantidade de linhas de evento ja incorporadas no processamento corrente; ajuda a barra de progresso em fluxos de folha (demo).",
            example = "12")
    private Integer eventosProcessados;
    @Schema(
            description = "Eventos ainda a processar apos a transicao, quando a action e parcial; zero quando o lote concluiu (demo).",
            example = "0")
    private Long eventosPendentes;
    @Schema(
            description = "Data de pagamento em vigor apos a transicao, quando a action fixa ou altera a liquidacao; ausente se irrelevante a esta rota (demo).",
            example = "2025-04-30")
    private LocalDate dataPagamento;
    @Schema(
            description = "Mensagem de sintese para o operador (sucesso, aviso, proximo passo); nao substitui codigos de erro estruturados em falha 4xx/5xx.",
            example = "Folha aprovada. Liquidacao agendada para 2025-04-30.")
    private String mensagem;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getEstadoAtual() {
        return estadoAtual;
    }

    public void setEstadoAtual(String estadoAtual) {
        this.estadoAtual = estadoAtual;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public Integer getEventosProcessados() {
        return eventosProcessados;
    }

    public void setEventosProcessados(Integer eventosProcessados) {
        this.eventosProcessados = eventosProcessados;
    }

    public Long getEventosPendentes() {
        return eventosPendentes;
    }

    public void setEventosPendentes(Long eventosPendentes) {
        this.eventosPendentes = eventosPendentes;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
