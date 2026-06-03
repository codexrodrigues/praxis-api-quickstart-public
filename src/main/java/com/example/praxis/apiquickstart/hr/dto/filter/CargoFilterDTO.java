package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.math.BigDecimal;
import java.util.List;

@Schema(
        name = "CargoFilterDTO",
        description = "Criterios de busca para o catalogo de cargos (nao e o Cargo persistido a editar). "
                + "Usado com GenericFilter / POST /filter no demo RH; paginacao/ordenacao seguem o recurso. Nenhum campo obrigatorio.")
public class CargoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Nome do Cargo", controlType = FieldControlType.INPUT, maxLength = 200, order = 10, helpText = "Filtrar por nome do cargo (ex: Engenheiro).", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Texto do titulo do cargo; operacao LIKE (substring) sobre nome. Casar com tabela e escala salarial (demo).")
    private String nome;

    @UISchema(label = "Nível/Senioridade", controlType = FieldControlType.INPUT, maxLength = 100, order = 20, helpText = "Filtrar por nível ou senioridade.", icon = "trending_up")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Filtro por senioridade, faixa de carreira ou padrao interno; LIKE sobre campo nivel (demo).")
    private String nivel;

    @UISchema(label = "Descrição", controlType = FieldControlType.INPUT, maxLength = 1000, order = 30, helpText = "Filtrar por palavras-chave na descrição.", icon = "description")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Pesquisa em descricao de responsabilidade do cargo; nao e descricao de vaga, e texto do master data.")
    private String descricao;

    @UISchema(label = "Piso Salarial", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 40,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Buscar cargos pelo piso salarial (mínimo e máximo).", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "salarioMinimo")
    @Schema(
            description = "Intervalo de piso minimo ofertado pelo cargo; BETWEEN na moeda do backend. Refina catalogo e benchmarks internos (demo).")
    private List<BigDecimal> salarioMinimoBetween;

    @UISchema(label = "Teto Salarial", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 50,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Buscar cargos pelo teto salarial (mínimo e máximo).", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "salarioMaximo")
    @Schema(
            description = "Intervalo de teto maximo; BETWEEN. Usa-se em conjunto com piso para faixas de requisicoes (demo).")
    private List<BigDecimal> salarioMaximoBetween;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public List<BigDecimal> getSalarioMinimoBetween() { return salarioMinimoBetween; }
    public void setSalarioMinimoBetween(List<BigDecimal> salarioMinimoBetween) { this.salarioMinimoBetween = salarioMinimoBetween; }
    public List<BigDecimal> getSalarioMaximoBetween() { return salarioMaximoBetween; }
    public void setSalarioMaximoBetween(List<BigDecimal> salarioMaximoBetween) { this.salarioMaximoBetween = salarioMaximoBetween; }
}
