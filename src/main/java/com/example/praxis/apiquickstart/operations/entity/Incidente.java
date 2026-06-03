package com.example.praxis.apiquickstart.operations.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.hibernate.annotations.ColumnDefault;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "Incidente.detail",
        attributeNodes = @NamedAttributeNode("missao")
)
@Table(name = "incidentes", schema = "public", indexes = {
        @Index(name = "idx_incidentes_missao", columnList = "missao_id"),
        @Index(name = "idx_incidentes_severidade", columnList = "severidade")
})
public class Incidente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "missao_id")
    private Missao missao;

    @NotNull
    @Column(name = "descricao", nullable = false, length = Integer.MAX_VALUE)
    @OptionLabel
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidade")
    private com.example.praxis.apiquickstart.operations.enums.Severidade severidade;

    @Column(name = "local", length = Integer.MAX_VALUE)
    private String local;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "ocorrido_em", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = false)
    private OffsetDateTime ocorridoEm;

    @Column(name = "danos_civis")
    private BigDecimal danosCivis;

    @Column(name = "feridos")
    private Integer feridos;

    @Column(name = "mortos")
    private Integer mortos;

}


