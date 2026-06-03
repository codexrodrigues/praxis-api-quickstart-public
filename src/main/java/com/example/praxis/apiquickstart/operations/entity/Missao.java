package com.example.praxis.apiquickstart.operations.entity;

import com.example.praxis.apiquickstart.riskintelligence.entity.Ameaca;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

import java.time.OffsetDateTime;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "Missao.detail",
        attributeNodes = @NamedAttributeNode("ameaca")
)
@Table(name = "missoes", schema = "public", indexes = {
        @Index(name = "idx_missoes_status_prioridade", columnList = "status, prioridade"),
        @Index(name = "idx_missoes_ameaca", columnList = "ameaca_id")
})
public class Missao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "titulo", nullable = false, length = Integer.MAX_VALUE)
    @DefaultSortColumn(priority = 2, ascending = true)
    @OptionLabel
    private String titulo;

    @Column(name = "objetivo", length = Integer.MAX_VALUE)
    private String objetivo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false)
    private com.example.praxis.apiquickstart.operations.enums.MissaoPrioridade prioridade;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private com.example.praxis.apiquickstart.operations.enums.MissaoStatus status;

    @Column(name = "local", length = Integer.MAX_VALUE)
    private String local;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ameaca_id")
    private Ameaca ameaca;

    @Column(name = "inicio_prev")
    @DefaultSortColumn(priority = 1, ascending = false)
    private OffsetDateTime inicioPrev;

    @Column(name = "fim_prev")
    private OffsetDateTime fimPrev;

    @Column(name = "inicio_real")
    private OffsetDateTime inicioReal;

    @Column(name = "fim_real")
    private OffsetDateTime fimReal;

}



