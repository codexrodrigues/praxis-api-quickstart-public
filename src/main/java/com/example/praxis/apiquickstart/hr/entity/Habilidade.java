package com.example.praxis.apiquickstart.hr.entity;

import com.example.praxis.apiquickstart.hr.enums.HabilidadeCategoria;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;
import jakarta.persistence.*;

@Entity
@Table(name = "habilidades")
public class Habilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false)
    @DefaultSortColumn(priority = 2, ascending = true)
    @OptionLabel
    private String nome;

    @Enumerated(EnumType.STRING)
    @DefaultSortColumn(priority = 1, ascending = true)
    @Column(name = "categoria")
    private HabilidadeCategoria categoria;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "nivel_poder")
    private Integer nivelPoder;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public HabilidadeCategoria getCategoria() { return categoria; }
    public void setCategoria(HabilidadeCategoria categoria) { this.categoria = categoria; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Integer getNivelPoder() { return nivelPoder; }
    public void setNivelPoder(Integer nivelPoder) { this.nivelPoder = nivelPoder; }
}
