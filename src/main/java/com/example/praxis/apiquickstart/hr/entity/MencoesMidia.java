package com.example.praxis.apiquickstart.hr.entity;

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
        name = "MencoesMidia.detail",
        attributeNodes = @NamedAttributeNode("funcionario")
)
@Table(name = "mencoes_midia", schema = "public", indexes = {
        @Index(name = "idx_midias_funcionario", columnList = "funcionario_id"),
        @Index(name = "idx_midias_publicado_em", columnList = "publicado_em")
})
public class MencoesMidia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    @NotNull
    @Column(name = "veiculo", nullable = false, length = Integer.MAX_VALUE)
    private String veiculo;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentimento")
    private com.example.praxis.apiquickstart.hr.enums.Sentimento sentimento;

    @Column(name = "url", length = Integer.MAX_VALUE)
    private String url;

    @ColumnDefault("now()")
    @Column(name = "publicado_em")
    @DefaultSortColumn(priority = 1, ascending = false)
    private OffsetDateTime publicadoEm;

}
