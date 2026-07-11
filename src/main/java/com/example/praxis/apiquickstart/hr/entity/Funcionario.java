package com.example.praxis.apiquickstart.hr.entity;

import com.example.praxis.apiquickstart.hr.enums.EstadoCivil;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@NamedEntityGraph(
        name = "Funcionario.detail",
        attributeNodes = {
                @NamedAttributeNode("cargo"),
                @NamedAttributeNode("departamento")
        }
)
@Table(name = "funcionarios")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Versão técnica usada somente para detectar atualizações concorrentes do cadastro. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "nome_completo", nullable = false)
    @DefaultSortColumn(priority = 2, ascending = true)
    private String nomeCompleto;

    @Column(name = "cpf", nullable = false, length = 11)
    private String cpf;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "telefone", nullable = false, length = 30)
    private String telefone;

    @Column(name = "salario", nullable = false)
    private BigDecimal salario;

    @Column(name = "data_admissao", nullable = false)
    @DefaultSortColumn(priority = 1, ascending = false)
    private LocalDate dataAdmissao;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id", nullable = false)
    private Cargo cargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;

    @Column(name = "foto_perfil_url")
    private String fotoPerfilUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_civil")
    private EstadoCivil estadoCivil;

    @Column(name = "pais_nascimento")
    private String paisNascimento;

    @Column(name = "cidade_nascimento")
    private String cidadeNascimento;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Long getVersion() { return version; }
    @OptionLabel
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = normalizeCpf(cpf); }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public BigDecimal getSalario() { return salario; }
    public void setSalario(BigDecimal salario) { this.salario = salario; }
    public LocalDate getDataAdmissao() { return dataAdmissao; }
    public void setDataAdmissao(LocalDate dataAdmissao) { this.dataAdmissao = dataAdmissao; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public Departamento getDepartamento() { return departamento; }
    public void setDepartamento(Departamento departamento) { this.departamento = departamento; }
    public String getFotoPerfilUrl() { return fotoPerfilUrl; }
    public void setFotoPerfilUrl(String fotoPerfilUrl) { this.fotoPerfilUrl = fotoPerfilUrl; }
    public EstadoCivil getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(EstadoCivil estadoCivil) { this.estadoCivil = estadoCivil; }
    public String getPaisNascimento() { return paisNascimento; }
    public void setPaisNascimento(String paisNascimento) { this.paisNascimento = paisNascimento; }
    public String getCidadeNascimento() { return cidadeNascimento; }
    public void setCidadeNascimento(String cidadeNascimento) { this.cidadeNascimento = cidadeNascimento; }

    private static String normalizeCpf(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
