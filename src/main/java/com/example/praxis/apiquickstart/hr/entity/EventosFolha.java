package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.praxisplatform.uischema.annotation.OptionLabel;
import com.example.praxis.apiquickstart.hr.enums.StatusEventoFolha;

import java.math.BigDecimal;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "EventosFolha.detail",
        attributeNodes = @NamedAttributeNode("folhaPagamento")
)
@Table(name = "eventos_folha", schema = "public")
public class EventosFolha {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "descricao", nullable = false, length = Integer.MAX_VALUE)
    private String descricao;

    @NotNull
    @Column(name = "tipo", nullable = false, length = Integer.MAX_VALUE)
    private String tipo;

    @NotNull
    @Column(name = "valor", nullable = false)
    private BigDecimal valor;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folha_pagamento_id", nullable = false)
    private FolhasPagamento folhaPagamento;

    /**
     * Campo de domínio opcional.
     * Não é persistido por JPA para manter compatibilidade com bancos legados
     * que ainda não possuem a coluna `eventos_folha.status`.
     */
    @Transient
    private StatusEventoFolha status = StatusEventoFolha.PENDENTE;

    @OptionLabel
    public String getLabel() { return descricao; }
}
