package com.example.praxis.apiquickstart.operationalassets.entity;

import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "Equipamento.detail",
        attributeNodes = @NamedAttributeNode("proprietario")
)
@Table(name = "equipamentos", schema = "public", indexes = {
        @Index(name = "idx_equipamentos_prop", columnList = "proprietario_id"),
        @Index(name = "idx_equipamentos_status", columnList = "status")
})
public class Equipamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "nome", nullable = false, length = Integer.MAX_VALUE)
    @DefaultSortColumn(priority = 2, ascending = true)
    @OptionLabel
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private com.example.praxis.apiquickstart.operationalassets.enums.EquipamentoTipo tipo;

    @Column(name = "resistencia")
    private Integer resistencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietario_id")
    private Funcionario proprietario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = true)
    private com.example.praxis.apiquickstart.operationalassets.enums.EquipamentoStatus status;

}



