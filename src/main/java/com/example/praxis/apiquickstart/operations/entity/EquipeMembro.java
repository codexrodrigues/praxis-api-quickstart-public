package com.example.praxis.apiquickstart.operations.entity;

import com.example.praxis.apiquickstart.operations.converter.PapelEquipeConverter;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "EquipeMembro.detail",
        attributeNodes = {
                @NamedAttributeNode("equipe"),
                @NamedAttributeNode("funcionario")
        }
)
@Table(name = "equipe_membros", schema = "public", indexes = {
        @Index(name = "idx_eq_membros_equipe", columnList = "equipe_id"),
        @Index(name = "idx_eq_membros_func", columnList = "funcionario_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "equipe_membros_equipe_id_funcionario_id_data_entrada_key", columnNames = {"equipe_id", "funcionario_id", "data_entrada"})
})
public class EquipeMembro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "equipe_id", nullable = false)
    private Equipe equipe;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Convert(converter = PapelEquipeConverter.class)
    @Column(name = "papel")
    private com.example.praxis.apiquickstart.operations.enums.PapelEquipe papel;

    @NotNull
    @ColumnDefault("CURRENT_DATE")
    @Column(name = "data_entrada", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = false)
    private LocalDate dataEntrada;

    @Column(name = "data_saida")
    private LocalDate dataSaida;

    @OptionLabel
    public String getLookupLabel() {
        String funcionarioNome = funcionario != null ? funcionario.getNomeCompleto() : null;
        String equipeNome = equipe != null ? equipe.getNome() : null;
        String papelNome = papel != null ? papel.name() : null;

        if (funcionarioNome == null && equipeNome == null && papelNome == null) {
            return id != null ? String.valueOf(id) : "";
        }

        return String.join(" · ", java.util.stream.Stream.of(funcionarioNome, equipeNome, papelNome)
                .filter(value -> value != null && !value.isBlank())
                .toList());
    }
}


