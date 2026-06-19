package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;

import java.math.BigDecimal;

@Schema(
        name = "CargoDTO",
        description = "Definicao de funcao no catalogo de RH: titulo, senioridade, texto livre e faixa salarial de referencia "
                + "usada em lotacao, progressao interna, planejamento de pessoal e governanca remuneratoria.")
public class CargoDTO {
    @Schema(
            description = "Chave do cargo no servico. Referenciado por colaboradores, lotacoes e ofertas internas; "
                    + "incluido em URLs e projecoes de catalogo.",
            example = "1")
    private Integer id;

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Nome", required = true, maxLength = 200, group = "Principal", order = 10, helpText = "Nome de exibição do cargo no catálogo.", icon = "work")
    @Schema(
            description = "Designacao comercial do cargo (ex. Analista de Operacoes) exibida em formularios e listagens de RH.",
            example = "Lider de Contingente")
    private String nome;

    @NotBlank
    @Size(max = 100)
    @UISchema(label = "Nível", required = true, maxLength = 100, group = "Principal", order = 20, helpText = "Grau de senioridade (ex: Júnior, Pleno).", icon = "trending_up")
    @Schema(
            description = "Nivel ou faixa de senioridade associada ao cargo (ex. Junior, Pleno, Especialista); "
                    + "governa regras de alocacao e progressao interna.",
            example = "Pleno")
    private String nivel;

    @Size(max = 1000)
    @UISchema(label = "Descrição", controlType = FieldControlType.TEXTAREA, maxLength = 1000, group = "Principal", order = 30, helpText = "Resumo das responsabilidades do cargo.", icon = "description")
    @Schema(
            description = "Resumo das responsabilidades, competencias-chave e contexto de missao; "
                    + "informacao para alinhamento operacional, nao requisito legal em si neste quickstart.")
    private String descricao;

    @DecimalMin("0.00")
    @UISchema(label = "Salário Mínimo", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, group = "Remuneração", order = 10, helpText = "Piso salarial de referência.", icon = "payments")
    @Schema(
            description = "Piso de referencia da faixa salarial (moeda e escala do backend). Uso: benchmark interno; "
                    + "nao e historico de pagamento de folha.")
    private BigDecimal salarioMinimo;

    @DecimalMin("0.00")
    @UISchema(label = "Salário Máximo", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, group = "Remuneração", order = 20, helpText = "Teto salarial de referência.", icon = "account_balance_wallet")
    @Schema(
            description = "Tecto de referencia da faixa salarial, emparelhado a salario minimo. Define o intervalo ofertavel "
                    + "para aprovacao e enquadramento remuneratorio.")
    private BigDecimal salarioMaximo;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getSalarioMinimo() { return salarioMinimo; }
    public void setSalarioMinimo(BigDecimal salarioMinimo) { this.salarioMinimo = salarioMinimo; }
    public BigDecimal getSalarioMaximo() { return salarioMaximo; }
    public void setSalarioMaximo(BigDecimal salarioMaximo) { this.salarioMaximo = salarioMaximo; }
}
