package com.example.praxis.apiquickstart.operations.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

import java.time.OffsetDateTime;

@lombok.Getter
@lombok.Setter
@Entity
@Table(name = "sinais_socorro", schema = "public", indexes = {
        @Index(name = "idx_sinais_nivel", columnList = "nivel_ameaca"),
        @Index(name = "idx_sinais_status", columnList = "status")
})
public class SinaisSocorro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "origem", nullable = false, length = Integer.MAX_VALUE)
    private String origem;

    @Column(name = "local", length = Integer.MAX_VALUE)
    private String local;

    @Column(name = "nivel_ameaca")
    private Integer nivelAmeaca;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private com.example.praxis.apiquickstart.operations.enums.SinalSocorroStatus status;

    @ColumnDefault("now()")
    @Column(name = "aberto_em")
    @DefaultSortColumn(priority = 1, ascending = false)
    private OffsetDateTime abertoEm;

    @Column(name = "fechado_em")
    private OffsetDateTime fechadoEm;

}


