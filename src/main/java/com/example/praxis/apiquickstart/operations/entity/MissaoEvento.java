package com.example.praxis.apiquickstart.operations.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

import java.time.OffsetDateTime;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "MissaoEvento.detail",
        attributeNodes = @NamedAttributeNode("missao")
)
@Table(name = "missao_eventos", schema = "public", indexes = {
        @Index(name = "idx_missao_eventos_missao", columnList = "missao_id"),
        @Index(name = "idx_missao_eventos_quando", columnList = "ocorrido_em")
})
public class MissaoEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "missao_id", nullable = false)
    private Missao missao;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "ocorrido_em", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = false)
    private OffsetDateTime ocorridoEm;

    @Column(name = "tipo")
    private com.example.praxis.apiquickstart.operations.enums.MissaoEventoTipo tipo;

    @Column(name = "descricao", length = Integer.MAX_VALUE)
    private String descricao;

}


