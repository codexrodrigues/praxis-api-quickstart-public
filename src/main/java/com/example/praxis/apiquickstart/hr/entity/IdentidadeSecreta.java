package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "IdentidadeSecreta.detail",
        attributeNodes = @NamedAttributeNode("funcionario")
)
@Table(name = "identidades_secretas", schema = "public", indexes = {
        @Index(name = "idx_identidades_secretas_funcionario", columnList = "funcionario_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "identidades_secretas_funcionario_id_codinome_key", columnNames = {"funcionario_id", "codinome"})
})
public class IdentidadeSecreta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @NotNull
    @Column(name = "codinome", nullable = false, length = Integer.MAX_VALUE)
    @DefaultSortColumn(priority = 1, ascending = true)
    private String codinome;

    @NotNull
    @Column(name = "universo", nullable = false, length = Integer.MAX_VALUE)
    private String universo;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "exposicao_publica", nullable = false)
    private Boolean exposicaoPublica = false;

}
