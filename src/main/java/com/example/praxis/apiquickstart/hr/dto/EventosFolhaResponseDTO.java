package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import com.example.praxis.apiquickstart.hr.enums.StatusEventoFolha;

import java.math.BigDecimal;

@Schema(
        name = "EventosFolhaResponseDTO",
        description = "Linha de evento de folha vinculada a uma folha de pagamento, representando provento, desconto ou base de calculo. "
                + "A soma e o tipo alimentam conciliacao e auditoria; valor e nao-negativo na validacao, semantica de credito/debito fica no tipo (string livre/ catalogo UI).")
public class EventosFolhaResponseDTO {
    @Schema(
            description = "Chave do evento de folha. Identifica o item no recurso e nas acoes de aprovacao em lote.",
            example = "1")
    private Integer id;

    @NotBlank
    @Size(max = 255)
    @UISchema(label = "Descrição", required = true, maxLength = 255, group = "Principal", order = 10, helpText = "Motivo ou nome do evento na folha.", icon = "description")
    @Schema(
            description = "Explicacao legivel do evento (ex.: 'Bonus missao', 'Pensao judicial'); aparece em extratos e trilha de aprovacao.",
            example = "Adicional de periculosidade - Operacoes")
    private String descricao;

    @NotBlank
    @Size(max = 100)
    @UISchema(label = "Tipo", required = true, maxLength = 100, group = "Principal", order = 20, helpText = "Provento ou desconto.", icon = "category")
    @Schema(
            description = "Classificacao operacional do evento, tipicamente provento, desconto, base de calculo ou rubrica definida pelo catalogo.",
            example = "provento")
    private String tipo;

    @NotNull
    @DecimalMin("0.00")
    @UISchema(label = "Valor", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, group = "Principal", order = 30, helpText = "Valor absoluto financeiro do evento.", icon = "payments")
    @Schema(
            description = "Montante absoluto do evento na moeda do backend; sinal e interpretacao (credito/debito) alinhada ao tipo e ao layout contabilistico (fora do DTO).",
            example = "1500.00")
    private BigDecimal valor;

    @NotNull
    @UISchema(
            label = "Folha de Pagamento",
            controlType = FieldControlType.ENTITY_LOOKUP,
            group = "Relacionamentos",
            order = 10,
            icon = "receipt_long",
            valueField = "id",
            displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FOLHAS_PAGAMENTO_PAYROLL_LOOKUP_OPTIONS,
            tableHidden = true,
            helpText = "Folha à qual o evento pertence."
    )
    @Schema(
            description = "Folha de pagamento a que o evento pertence, agregando competencia e janela de fechamento.",
            example = "5")
    private Integer folhaPagamentoId;

    @UISchema(label = "Folha", readOnly = true, formHidden = true, group = "Relacionamentos", order = 11, helpText = "Competência ou Folha (preenchido automaticamente).", icon = "receipt_long")
    @Schema(
            description = "Rotulo resumido da folha (competencia ou nome) para listagens sem lookup; espelha folhaPagamentoId.")
    private String folhaPagamentoNome;

    @NotNull
    @UISchema(label = "Status", controlType = FieldControlType.INPUT, readOnly = true, group = "Workflow", order = 10,
            helpText = "Situação atual do evento no fluxo de conferência e fechamento.", icon = "approval")
    @Schema(description = "Estado persistido do evento no workflow de folha. PENDENTE ainda pode ser conferido; APROVADO integra o fechamento; REJEITADO exige tratamento antes de novo processamento.", example = "PENDENTE")
    private StatusEventoFolha status;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Integer getFolhaPagamentoId() {
        return folhaPagamentoId;
    }

    public void setFolhaPagamentoId(Integer folhaPagamentoId) {
        this.folhaPagamentoId = folhaPagamentoId;
    }

    public String getFolhaPagamentoNome() {
        return folhaPagamentoNome;
    }

    public void setFolhaPagamentoNome(String folhaPagamentoNome) {
        this.folhaPagamentoNome = folhaPagamentoNome;
    }

    public StatusEventoFolha getStatus() {
        return status;
    }

    public void setStatus(StatusEventoFolha status) {
        this.status = status;
    }
}
