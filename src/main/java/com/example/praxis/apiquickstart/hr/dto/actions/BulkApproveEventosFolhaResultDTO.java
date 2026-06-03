package com.example.praxis.apiquickstart.hr.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado agregado da workflow action de aprovacao em lote.
 *
 * <p>O quickstart devolve tanto o resumo do lote quanto o detalhe por item para demonstrar um
 * padrao de action que continua amigavel para operadores humanos e automacoes.</p>
 */
@Schema(
        name = "BulkApproveEventosFolhaResultDTO",
        description = "Resposta da aprovacao em lote: totais agregados e detalhe por id para o operador e para automacoes. "
                + "Falha parcial e suportada: processed + failed == total se todos os ids tiveram desfecho (demo).")
public class BulkApproveEventosFolhaResultDTO {
    @Schema(
            description = "Numero de ids recebidos no pedido (tamanho logico do lote, antes de deduplicacao interna, se houver).",
            example = "3")
    private int total;
    @Schema(
            description = "Quantidade de aprovacoes concluidas com sucesso (estado APROVADO ou equivalente, conforme regra de negocio).",
            example = "2")
    private int processed;
    @Schema(
            description = "Quantidade de itens com erro (validacao, conflito de estado, nao encontrado); ver details[].",
            example = "1")
    private int failed;
    @Schema(
            description = "Uma linha por id processada: sucesso (ok) ou motivo de falha (error) para diagnostico; ordem nao e garantidamente a do request.")
    private List<ItemResult> details = new ArrayList<>();

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getProcessed() {
        return processed;
    }

    public void setProcessed(int processed) {
        this.processed = processed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public List<ItemResult> getDetails() {
        return details;
    }

    public void setDetails(List<ItemResult> details) {
        this.details = details;
    }

    /**
     * Resultado individual de cada id processado pelo lote.
     */
    @Schema(
            name = "BulkApproveEventosFolhaItemResult",
            description = "Desfecho de um unico id no lote; ok indica aprovacao aplicada, error explica a falha quando ok e false (demo).")
    public static class ItemResult {
        @Schema(
                description = "Identificador do evento de folha submetido; corresponde a um id do request quando presente e encontrado.",
                example = "101")
        private Integer id;
        @Schema(
                description = "true se a aprovacao foi aplicada sem erro; false se o registo nao puder transicionar (estado, lock, inexistente).",
                example = "true")
        private boolean ok;
        @Schema(
                description = "Texto de erro ou codigo amigavel quando ok e false; vazio em sucesso. Nao e stack trace; uso operacional (demo).",
                example = "Evento nao esta pendente de aprovacao")
        private String error;

        public ItemResult() {
        }

        public ItemResult(Integer id, boolean ok, String error) {
            this.id = id;
            this.ok = ok;
            this.error = error;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public boolean isOk() {
            return ok;
        }

        public void setOk(boolean ok) {
            this.ok = ok;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
