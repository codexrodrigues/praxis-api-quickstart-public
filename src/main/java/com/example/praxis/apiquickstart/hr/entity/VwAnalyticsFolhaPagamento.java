package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

@lombok.Getter
@lombok.Setter
@Entity
@Immutable
@Table(name = "vw_analytics_folha_pagamento", schema = "public")
public class VwAnalyticsFolhaPagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folha_pagamento_id")
    private Integer folhaPagamentoId;

    @Column(name = "funcionario_id")
    private Integer funcionarioId;

    @Column(name = "nome_completo", length = Integer.MAX_VALUE)
    private String nomeCompleto;

    @Column(name = "codinome", length = Integer.MAX_VALUE)
    private String codinome;

    @Column(name = "universo", length = Integer.MAX_VALUE)
    private String universo;

    @Column(name = "exposicao_publica")
    private Boolean exposicaoPublica;

    @Column(name = "cargo_id")
    private Integer cargoId;

    @Column(name = "cargo", length = Integer.MAX_VALUE)
    private String cargo;

    @Column(name = "departamento_id")
    private Integer departamentoId;

    @Column(name = "departamento", length = Integer.MAX_VALUE)
    private String departamento;

    @Column(name = "equipe_id")
    private Integer equipeId;

    @Column(name = "equipe", length = Integer.MAX_VALUE)
    private String equipe;

    @Column(name = "papel_equipe", length = Integer.MAX_VALUE)
    private String papelEquipe;

    @Column(name = "base_id")
    private Integer baseId;

    @Column(name = "base", length = Integer.MAX_VALUE)
    private String base;

    @Column(name = "tipo_base", length = Integer.MAX_VALUE)
    private String tipoBase;

    @Column(name = "sigilo_base", length = Integer.MAX_VALUE)
    private String sigiloBase;

    @Column(name = "ano")
    private Integer ano;

    @Column(name = "mes")
    private Integer mes;

    @Column(name = "competencia")
    private LocalDate competencia;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(name = "salario_bruto")
    private BigDecimal salarioBruto;

    @Column(name = "total_descontos")
    private BigDecimal totalDescontos;

    @Column(name = "salario_liquido")
    private BigDecimal salarioLiquido;

    @Column(name = "qtd_eventos")
    private Long qtdEventos;

    @Column(name = "qtd_proventos")
    private Long qtdProventos;

    @Column(name = "qtd_descontos")
    private Long qtdDescontos;

    @Column(name = "qtd_adicionais")
    private Long qtdAdicionais;

    @Column(name = "qtd_tipos_evento")
    private Long qtdTiposEvento;

    @Column(name = "valor_proventos")
    private BigDecimal valorProventos;

    @Column(name = "valor_descontos_eventos")
    private BigDecimal valorDescontosEventos;

    @Column(name = "valor_adicionais")
    private BigDecimal valorAdicionais;

    @Column(name = "saldo_eventos")
    private BigDecimal saldoEventos;

    @Column(name = "saldo_liquido_vs_bruto")
    private BigDecimal saldoLiquidoVsBruto;

    @Column(name = "pct_desconto")
    private BigDecimal pctDesconto;

    @Column(name = "pct_liquido")
    private BigDecimal pctLiquido;

    @Column(name = "pct_adicionais_sobre_bruto")
    private BigDecimal pctAdicionaisSobreBruto;

    @Column(name = "pct_eventos_desconto_sobre_bruto")
    private BigDecimal pctEventosDescontoSobreBruto;

    @Column(name = "faixa_salario_bruto", length = Integer.MAX_VALUE)
    private String faixaSalarioBruto;

    @Column(name = "faixa_salario_liquido", length = Integer.MAX_VALUE)
    private String faixaSalarioLiquido;

    @Column(name = "faixa_pct_desconto", length = Integer.MAX_VALUE)
    private String faixaPctDesconto;

    @Column(name = "faixa_valor_adicionais", length = Integer.MAX_VALUE)
    private String faixaValorAdicionais;

    @Column(name = "payroll_profile", length = Integer.MAX_VALUE)
    private String payrollProfile;

    @Column(name = "composicao_folha", length = Integer.MAX_VALUE)
    private String composicaoFolha;

    @Column(name = "eventos_descricao", length = Integer.MAX_VALUE)
    private String eventosDescricao;
}
