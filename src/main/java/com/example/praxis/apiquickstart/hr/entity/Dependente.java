package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.*;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

import java.time.LocalDate;

@Entity
@NamedEntityGraph(
        name = "Dependente.detail",
        attributeNodes = @NamedAttributeNode("funcionario")
)
@Table(name = "dependentes")
public class Dependente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome_completo", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = true)
    private String nomeCompleto;

    @Column(name = "parentesco", nullable = false)
    private String parentesco;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }
}
