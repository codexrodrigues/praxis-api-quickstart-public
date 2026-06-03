package com.example.praxis.apiquickstart.operations.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.praxisplatform.uischema.annotation.OptionLabel;

@lombok.Getter
@lombok.Setter
@Entity
@Table(name = "acordos_regulatorios", schema = "public", indexes = {
        @Index(name = "ux_acordos_nome_juris", columnList = "nome, jurisdicao", unique = true)
})
public class AcordosRegulatorio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "nome", nullable = false, length = Integer.MAX_VALUE)
    @OptionLabel
    private String nome;

    @NotNull
    @Column(name = "jurisdicao", nullable = false, length = Integer.MAX_VALUE)
    private String jurisdicao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private com.example.praxis.apiquickstart.operations.enums.AcordoStatus status;

    @Column(name = "descricao", length = Integer.MAX_VALUE)
    private String descricao;

}


