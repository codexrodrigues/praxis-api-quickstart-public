package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.*;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

import java.math.BigDecimal;

@Entity
@Table(name = "cargos")
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = true)
    @OptionLabel
    private String nome;

    @Column(name = "nivel", nullable = false)
    private String nivel;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "salario_minimo")
    private BigDecimal salarioMinimo;

    @Column(name = "salario_maximo")
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
