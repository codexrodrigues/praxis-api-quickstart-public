package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.*;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "FeriasAfastamento.detail",
        attributeNodes = @NamedAttributeNode("funcionario")
)
@Table(name = "ferias_afastamentos", schema = "public")
public class FeriasAfastamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "tipo", nullable = false, length = Integer.MAX_VALUE)
    private String tipo;

    @NotNull
    @Column(name = "data_inicio", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = false)
    private LocalDate dataInicio;

    @NotNull
    @Column(name = "data_fim", nullable = false)
    @DefaultSortColumn(priority = 2, ascending = false)
    private LocalDate dataFim;

    @Column(name = "observacoes", length = Integer.MAX_VALUE)
    private String observacoes;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @OptionLabel
    public String getLabel() {
        String inicio = dataInicio != null ? dataInicio.toString() : "";
        String fim = dataFim != null ? dataFim.toString() : "";
        return (tipo != null ? tipo : "") + " " + inicio + (fim.isEmpty() ? "" : " → " + fim);
    }
}
