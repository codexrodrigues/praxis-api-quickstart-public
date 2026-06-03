package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/**
 * Mapping for DB view
 */
@lombok.Getter
@lombok.Setter
@Entity
@Immutable
@Table(name = "vw_ranking_reputacao", schema = "public")
public class VwRankingReputacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "funcionario_id")
    private Integer funcionarioId;

    @Column(name = "nome_completo", length = Integer.MAX_VALUE)
    private String nomeCompleto;

    @Column(name = "codinome", length = Integer.MAX_VALUE)
    private String codinome;

    @Column(name = "equipe", length = Integer.MAX_VALUE)
    private String equipe;

    @Column(name = "score_publico")
    private Integer scorePublico;

    @Column(name = "score_governamental")
    private Integer scoreGovernamental;

    @Column(name = "media")
    private BigDecimal media;

    @Column(name = "posicao")
    private Long posicao;

}
