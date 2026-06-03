package com.example.praxis.apiquickstart.operations.entity;

import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

import java.time.LocalDate;

@lombok.Getter
@lombok.Setter
@Entity
@NamedEntityGraph(
        name = "LicencasOperacao.detail",
        attributeNodes = {
                @NamedAttributeNode("acordo"),
                @NamedAttributeNode("funcionario"),
                @NamedAttributeNode("equipe")
        }
)
@Table(name = "licencas_operacao", schema = "public", indexes = {
        @Index(name = "idx_licencas_acordo", columnList = "acordo_id"),
        @Index(name = "idx_licencas_func", columnList = "funcionario_id"),
        @Index(name = "idx_licencas_equipe", columnList = "equipe_id")
})
public class LicencasOperacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "acordo_id", nullable = false)
    private AcordosRegulatorio acordo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id")
    private Equipe equipe;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel")
    private com.example.praxis.apiquickstart.operations.enums.LicencaNivel nivel;

    @NotNull
    @Column(name = "valido_de", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = false)
    private LocalDate validoDe;

    @Column(name = "valido_ate")
    private LocalDate validoAte;

    @OptionLabel
    public String getLookupLabel() {
        String acordoNome = acordo != null ? acordo.getNome() : "Sem acordo";
        String scope = funcionario != null
                ? funcionario.getNomeCompleto()
                : (equipe != null ? equipe.getNome() : "Escopo indefinido");
        String nivelLabel = nivel != null ? nivel.name() : "SEM_NIVEL";
        return acordoNome + " · " + scope + " · " + nivelLabel;
    }

}


