package com.example.praxis.apiquickstart.hr.entity;

import com.example.praxis.apiquickstart.operations.entity.Incidente;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

import java.math.BigDecimal;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "Indenizacao.detail",
        attributeNodes = @NamedAttributeNode("incidente")
)
@Table(name = "indenizacoes", schema = "public", indexes = {
        @Index(name = "idx_indenizacoes_incidente", columnList = "incidente_id"),
        @Index(name = "idx_indenizacoes_pago", columnList = "pago")
})
public class Indenizacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "incidente_id", nullable = false)
    private Incidente incidente;

    @NotNull
    @Column(name = "valor", nullable = false)
    @DefaultSortColumn(priority = 2, ascending = false)
    private BigDecimal valor;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "pago", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = true)
    private Boolean pago = false;

    @Column(name = "seguradora", length = Integer.MAX_VALUE)
    private String seguradora;

    @Column(name = "processo_num", length = Integer.MAX_VALUE)
    private String processoNum;

}
