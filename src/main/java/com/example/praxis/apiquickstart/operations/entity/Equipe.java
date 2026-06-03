package com.example.praxis.apiquickstart.operations.entity;

import com.example.praxis.apiquickstart.operations.enums.EquipeStatus;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;
import jakarta.persistence.*;

@Entity
@NamedEntityGraph(
        name = "Equipe.detail",
        attributeNodes = @NamedAttributeNode("basePrincipal")
)
@Table(name = "equipes")
public class Equipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = true)
    @OptionLabel
    private String nome;

    @Column(name = "sigla", length = 12)
    private String sigla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_principal_id")
    private Base basePrincipal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EquipeStatus status;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }
    public Base getBasePrincipal() { return basePrincipal; }
    public void setBasePrincipal(Base basePrincipal) { this.basePrincipal = basePrincipal; }
    public EquipeStatus getStatus() { return status; }
    public void setStatus(EquipeStatus status) { this.status = status; }
}


