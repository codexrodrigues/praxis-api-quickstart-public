package com.example.praxis.apiquickstart.operations.entity;

import com.example.praxis.apiquickstart.operations.enums.BaseSigilo;
import com.example.praxis.apiquickstart.operations.enums.BaseTipo;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;
import jakarta.persistence.*;

import java.math.BigDecimal;

@lombok.Getter
@lombok.Setter
@Entity
@Table(name = "bases")
public class Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "nome", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = true)
    @OptionLabel
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private BaseTipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "sigilo")
    private BaseSigilo sigilo;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "planeta")
    private String planeta;
}


