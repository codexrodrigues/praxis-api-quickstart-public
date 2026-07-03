package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.math.BigDecimal;

@Schema(
        name = "UpdateEventosFolhaDTO",
        description = "Comando para corrigir ou reclassificar um evento de folha existente, mantendo sua identidade tecnica e atualizando rubrica, tipo, valor ou folha de competencia.")
public class UpdateEventosFolhaDTO {

    @NotBlank
    @Size(max = 255)
    @UISchema(label = "Descrição", required = true, maxLength = 255, group = "Principal", order = 10, helpText = "Novo motivo ou nome do evento.", icon = "description")
    @Schema(
            description = "Historico ou rubrica do lancamento (ex. correcao de lancamento anterior).")
    private String descricao;

    @NotBlank
    @Size(max = 100)
    @UISchema(label = "Tipo", required = true, maxLength = 100, group = "Principal", order = 20, helpText = "Nova classificação (provento ou desconto).", icon = "category")
    @Schema(
            description = "Categoria provento ou desconto (texto do dominio).")
    private String tipo;

    @NotNull
    @DecimalMin("0.00")
    @UISchema(label = "Valor", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, group = "Principal", order = 30, helpText = "Valor absoluto financeiro ajustado.", icon = "payments")
    @Schema(
            description = "Montante ajustado na moeda da folha.")
    private BigDecimal valor;

    @NotNull
    @UISchema(
            label = "Folha de Pagamento",
            controlType = FieldControlType.ENTITY_LOOKUP,
            group = "Relacionamentos",
            order = 10,
            valueField = "id",
            displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FOLHAS_PAGAMENTO_PAYROLL_LOOKUP_OPTIONS,
            tableHidden = true,
            helpText = "Nova folha à qual o evento pertence.",
            icon = "receipt_long"
    )
    @Schema(
            description = "FK; folha alvo se o lancamento mudar de competencia (folhaPagamentoId).")
    private Integer folhaPagamentoId;

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
}
