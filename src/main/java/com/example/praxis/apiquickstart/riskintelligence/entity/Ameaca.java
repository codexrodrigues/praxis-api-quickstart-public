package com.example.praxis.apiquickstart.riskintelligence.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.hibernate.annotations.ColumnDefault;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

import java.math.BigDecimal;

@lombok.Getter
@lombok.Setter
@Entity
@Table(name = "ameacas", schema = "public", indexes = {
        @Index(name = "ux_ameacas_nome", columnList = "nome", unique = true),
        @Index(name = "idx_ameacas_status_nivel", columnList = "status, nivel")
})
public class Ameaca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "nome", nullable = false, length = Integer.MAX_VALUE)
    @DefaultSortColumn(priority = 2, ascending = true)
    @OptionLabel
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "classe")
    private com.example.praxis.apiquickstart.riskintelligence.enums.AmeacaClasse classe;

    @ColumnDefault("'Terra'")
    @Column(name = "planeta", length = Integer.MAX_VALUE)
    private String planeta;

    @Column(name = "nivel")
    @DefaultSortColumn(priority = 1, ascending = false)
    private Integer nivel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private com.example.praxis.apiquickstart.riskintelligence.enums.AmeacaStatus status;

    @Column(name = "recompensa")
    private BigDecimal recompensa;

}

