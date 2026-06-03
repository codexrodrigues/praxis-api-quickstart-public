package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.*;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "FolhasPagamento.detail",
        attributeNodes = @NamedAttributeNode("funcionario")
)
@Table(name = "folhas_pagamento", schema = "public")
public class FolhasPagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "ano", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = false)
    private Integer ano;

    @NotNull
    @Column(name = "mes", nullable = false)
    @DefaultSortColumn(priority = 2, ascending = false)
    private Integer mes;

    @NotNull
    @Column(name = "salario_bruto", nullable = false)
    private BigDecimal salarioBruto;

    @NotNull
    @Column(name = "total_descontos", nullable = false)
    private BigDecimal totalDescontos;

    @NotNull
    @Column(name = "salario_liquido", nullable = false)
    private BigDecimal salarioLiquido;

    @NotNull
    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @OneToMany
    @JoinColumn(name = "folha_pagamento_id")
    private Set<EventosFolha> eventosFolhas = new LinkedHashSet<>();

    @OptionLabel
    public String getLabel() {
        String y = ano != null ? String.format("%04d", ano) : "";
        String m = mes != null ? String.format("%02d", mes) : "";
        return (y + "-" + m).trim();
    }
}
