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
@Table(name = "vw_perfil_heroi", schema = "public")
public class VwPerfilHeroi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "funcionario_id")
    private Integer funcionarioId;

    @Column(name = "avatar_url", length = Integer.MAX_VALUE)
    private String avatarUrl;

    @Column(name = "nome_completo", length = Integer.MAX_VALUE)
    private String nomeCompleto;

    @Column(name = "codinome", length = Integer.MAX_VALUE)
    private String codinome;

    @Column(name = "universo", length = Integer.MAX_VALUE)
    private String universo;

    @Column(name = "exposicao_publica")
    private Boolean exposicaoPublica;

    @Column(name = "cargo", length = Integer.MAX_VALUE)
    private String cargo;

    @Column(name = "departamento", length = Integer.MAX_VALUE)
    private String departamento;

    @Column(name = "score_publico")
    private Integer scorePublico;

    @Column(name = "score_governamental")
    private Integer scoreGovernamental;

    @Column(name = "score_medio")
    private BigDecimal scoreMedio;

    @Column(name = "habilidades", length = Integer.MAX_VALUE)
    private String habilidades;

    @Column(name = "equipe_principal", length = Integer.MAX_VALUE)
    private String equipePrincipal;

    @Column(name = "base_principal", length = Integer.MAX_VALUE)
    private String basePrincipal;

}
