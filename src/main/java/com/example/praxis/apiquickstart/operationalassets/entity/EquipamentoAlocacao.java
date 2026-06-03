package com.example.praxis.apiquickstart.operationalassets.entity;

import com.example.praxis.apiquickstart.operationalassets.enums.AlocacaoStatus;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
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
        name = "EquipamentoAlocacao.detail",
        attributeNodes = {
                @NamedAttributeNode("equipamento"),
                @NamedAttributeNode("funcionario")
        }
)
@Table(name = "equipamento_alocacoes", schema = "public", indexes = {
        @Index(name = "idx_eq_aloc_equip", columnList = "equipamento_id"),
        @Index(name = "idx_eq_aloc_func", columnList = "funcionario_id")
})
public class EquipamentoAlocacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private Equipamento equipamento;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "inicio", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = false)
    private OffsetDateTime inicio;

    @Column(name = "fim")
    private OffsetDateTime fim;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AlocacaoStatus status;

}


