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
        name = "Veiculo.detail",
        attributeNodes = @NamedAttributeNode("proprietario")
)
@Table(name = "veiculos", schema = "public", indexes = {
        @Index(name = "idx_veiculos_prop", columnList = "proprietario_id"),
        @Index(name = "idx_veiculos_status", columnList = "status")
})
public class Veiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "nome", nullable = false, length = Integer.MAX_VALUE)
    @DefaultSortColumn(priority = 1, ascending = true)
    @OptionLabel
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private com.example.praxis.apiquickstart.operationalassets.enums.VeiculoTipo tipo;

    @Column(name = "capacidade")
    private Integer capacidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietario_id")
    private Funcionario proprietario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private com.example.praxis.apiquickstart.operationalassets.enums.VeiculoStatus status;

}



