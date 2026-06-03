package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

import java.math.BigDecimal;
import java.time.LocalDate;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "HistoricoSalarial.detail",
        attributeNodes = @NamedAttributeNode("funcionario")
)
@Table(name = "historicos_salariais", schema = "public")
public class HistoricoSalarial {
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
    @Column(name = "salario", nullable = false)
    private BigDecimal salario;

    @NotNull
    @Column(name = "data_inicio", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "motivo", length = Integer.MAX_VALUE)
    private String motivo;

}
