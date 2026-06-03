package com.example.praxis.apiquickstart.operations.entity;

import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import com.example.praxis.apiquickstart.operations.converter.ResultadoMissaoConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "MissaoParticipante.detail",
        attributeNodes = {
                @NamedAttributeNode("missao"),
                @NamedAttributeNode("funcionario")
        }
)
@Table(name = "missao_participantes", schema = "public", indexes = {
        @Index(name = "idx_miss_part_missao", columnList = "missao_id"),
        @Index(name = "idx_miss_part_func", columnList = "funcionario_id"),
        @Index(name = "idx_miss_part_missao_ordem", columnList = "missao_id, ordem")
}, uniqueConstraints = {
        @UniqueConstraint(name = "missao_participantes_missao_id_funcionario_id_key", columnNames = {"missao_id", "funcionario_id"})
})
public class MissaoParticipante {
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel")
    private com.example.praxis.apiquickstart.operations.enums.PapelMissao papel;

    @NotNull
    @Column(name = "ordem", nullable = false)
    private Integer ordem = 0;

    @NotNull
    @Column(name = "principal", nullable = false)
    private Boolean principal = false;

    @Convert(converter = ResultadoMissaoConverter.class)
    @Column(name = "resultado")
    private com.example.praxis.apiquickstart.operations.enums.ResultadoMissao resultado;

}


