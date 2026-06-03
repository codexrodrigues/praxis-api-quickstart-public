package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Filtro dos eventos individuais que compoem uma folha.
 *
 * <p>Ele prioriza os eixos mais uteis para operacao e workflow: descricao,
 * tipo, faixa de valor e a folha dona do evento.
 */
@Schema(
        name = "EventosFolhaFilterDTO",
        description = "Criterios de busca em linhas de evento de folha (rubrica), nao a folha inteira. "
                + "Aplica-se a conferencia e workflow de aprovacao; GenericFilter / POST /filter (ver javadoc de classe).")
public class EventosFolhaFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Descrição do Evento", controlType = FieldControlType.INPUT, maxLength = 255, order = 10, helpText = "Filtrar evento por descrição.", icon = "description")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Pesquisa em descricao de rubrica (ex.: bonus, pensao); LIKE.")
    private String descricao;

    @UISchema(label = "Tipo de Evento", controlType = FieldControlType.INPUT, maxLength = 100, order = 20, helpText = "Filtrar pelo tipo do evento (ex: provento).", icon = "event_note")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Filtro por classificacao de evento (provento, desconto, etc.); string livre no demo; LIKE.")
    private String tipo;

    @UISchema(label = "Faixa de Valor", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 30,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Buscar eventos dentro de uma faixa de valor.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "valor")
    @Schema(
            description = "Faixa de valor absoluto do evento na moeda do backend; BETWEEN (min, max) para auditoria (demo).")
    private List<BigDecimal> valorBetween;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 40,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FOLHAS_PAGAMENTO + "/options/filter", helpText = "Filtrar eventos de uma folha específica.", icon = "receipt_long")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "folhaPagamento.id")
    @Schema(
            description = "Folha de pagamento a que o evento pertence; EQUAL por id (competencia / lote).")
    private Integer folhaPagamentoId;

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public List<BigDecimal> getValorBetween() { return valorBetween; }
    public void setValorBetween(List<BigDecimal> valorBetween) { this.valorBetween = valorBetween; }

    public Integer getFolhaPagamentoId() { return folhaPagamentoId; }
    public void setFolhaPagamentoId(Integer folhaPagamentoId) { this.folhaPagamentoId = folhaPagamentoId; }
}
