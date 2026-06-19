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
        name = "CreateEventosFolhaDTO",
        description = "Comando para lancar uma rubrica ou evento financeiro em folha de pagamento, vinculando descricao, tipo, valor absoluto e competencia de folha para conciliacao e aprovacao.")
public class CreateEventosFolhaDTO {

    @NotBlank
    @Size(max = 255)
    @UISchema(label = "DescriÃ§Ã£o", required = true, maxLength = 255, group = "Principal", order = 10, helpText = "Motivo ou nome do evento na folha.", icon = "description")
    @Schema(
            description = "Historico ou rubrica do lancamento (ex. hora extra, bonus).")
    private String descricao;

    @NotBlank
    @Size(max = 100)
    @UISchema(label = "Tipo", required = true, maxLength = 100, group = "Principal", order = 20, helpText = "Provento ou desconto.", icon = "category")
    @Schema(
            description = "Categoria contabil/ provento ou desconto (texto controlado pelo dominio).")
    private String tipo;

    @NotNull
    @DecimalMin("0.00")
    @UISchema(label = "Valor", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, group = "Principal", order = 30, helpText = "Valor absoluto financeiro do evento.", icon = "payments")
    @Schema(
            description = "Montante na moeda da folha; credito ou debito conforme tipo.")
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
            helpText = "Folha Ã  qual o evento pertence.",
            icon = "receipt_long"
    )
    @Schema(
            description = "FK; competencia/folha onde o evento se apropria (folhaPagamentoId).")
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
