package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.*;

@Entity
@NamedEntityGraph(
        name = "FuncionarioHabilidade.detail",
        attributeNodes = {
                @NamedAttributeNode("funcionario"),
                @NamedAttributeNode("habilidade")
        }
)
@Table(name = "funcionario_habilidades",
       uniqueConstraints = @UniqueConstraint(columnNames = {"funcionario_id","habilidade_id"}))
public class FuncionarioHabilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidade_id", nullable = false)
    private Habilidade habilidade;

    @Column(name = "proficiencia", nullable = false)
    private Integer proficiencia;

    @Column(name = "origem")
    private String origem;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }
    public Habilidade getHabilidade() { return habilidade; }
    public void setHabilidade(Habilidade habilidade) { this.habilidade = habilidade; }
    public Integer getProficiencia() { return proficiencia; }
    public void setProficiencia(Integer proficiencia) { this.proficiencia = proficiencia; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
}
