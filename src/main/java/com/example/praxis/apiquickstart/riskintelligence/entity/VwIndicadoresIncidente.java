package com.example.praxis.apiquickstart.riskintelligence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Mapping for DB view
 */
@lombok.Getter
@lombok.Setter
@Entity
@Immutable
@Table(name = "vw_indicadores_incidentes", schema = "public")
public class VwIndicadoresIncidente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "incidente_id")
    private Integer incidenteId;

    @Column(name = "missao", length = Integer.MAX_VALUE)
    private String missao;

    @Column(name = "descricao", length = Integer.MAX_VALUE)
    private String descricao;

    @Column(name = "local", length = Integer.MAX_VALUE)
    private String local;

    @Column(name = "severidade", length = Integer.MAX_VALUE)
    private String severidade;

    @Column(name = "danos_civis")
    private BigDecimal danosCivis;

    @Column(name = "total_indenizacoes")
    private BigDecimal totalIndenizacoes;

    @Column(name = "total_pago")
    private BigDecimal totalPago;

    @Column(name = "total_pendente")
    private BigDecimal totalPendente;

    @Column(name = "ocorrido_em")
    private OffsetDateTime ocorridoEm;

}

