package com.example.praxis.apiquickstart.operationalassets.entity;

import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import com.example.praxis.apiquickstart.operations.entity.Missao;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

import java.time.OffsetDateTime;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "VeiculoMissaoUso.detail",
        attributeNodes = {
                @NamedAttributeNode("veiculo"),
                @NamedAttributeNode("missao"),
                @NamedAttributeNode("piloto")
        }
)
@Table(name = "veiculo_missao_usos", schema = "public", indexes = {
        @Index(name = "idx_vm_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_vm_missao", columnList = "missao_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "veiculo_missao_usos_veiculo_id_missao_id_key", columnNames = {"veiculo_id", "missao_id"})
})
public class VeiculoMissaoUso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "missao_id", nullable = false)
    private Missao missao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piloto_id")
    private Funcionario piloto;

    @Column(name = "partida")
    @DefaultSortColumn(priority = 1, ascending = false)
    private OffsetDateTime partida;

    @Column(name = "chegada")
    private OffsetDateTime chegada;

    @Column(name = "observacao", length = Integer.MAX_VALUE)
    private String observacao;

}


