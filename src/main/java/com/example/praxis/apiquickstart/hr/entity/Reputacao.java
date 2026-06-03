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
        name = "Reputacao.detail",
        attributeNodes = @NamedAttributeNode("funcionario")
)
@Table(name = "reputacoes", schema = "public", indexes = {
        @Index(name = "idx_reputacoes_funcionario", columnList = "funcionario_id"),
        @Index(name = "idx_reputacoes_scores", columnList = "score_publico, score_governamental")
}, uniqueConstraints = {
        @UniqueConstraint(name = "reputacoes_funcionario_id_key", columnNames = {"funcionario_id"})
})
public class Reputacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Column(name = "score_publico")
    private Integer scorePublico;

    @Column(name = "score_governamental")
    private Integer scoreGovernamental;

    @ColumnDefault("now()")
    @Column(name = "atualizado_em")
    @DefaultSortColumn(priority = 1, ascending = false)
    private OffsetDateTime atualizadoEm;

}
