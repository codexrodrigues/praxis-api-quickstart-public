package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.*;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

@Entity
@NamedEntityGraph(
        name = "Endereco.detail",
        attributeNodes = @NamedAttributeNode("funcionario")
)
@Table(name = "enderecos")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "logradouro", nullable = false)
    @DefaultSortColumn(priority = 2, ascending = true)
    private String logradouro;

    @Column(name = "numero", nullable = false, length = 50)
    private String numero;

    @Column(name = "complemento")
    private String complemento;

    @Column(name = "bairro", nullable = false)
    private String bairro;

    @Column(name = "cidade", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = true)
    private String cidade;

    @Column(name = "estado", nullable = false, length = 2)
    private String estado;

    @Column(name = "cep", nullable = false, length = 20)
    private String cep;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", unique = true, nullable = false)
    private Funcionario funcionario;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }
}
