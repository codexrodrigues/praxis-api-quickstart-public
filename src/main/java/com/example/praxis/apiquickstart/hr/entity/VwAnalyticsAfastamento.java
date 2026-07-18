package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@lombok.Getter
@lombok.Setter
@Entity
@Immutable
@Table(name = "vw_analytics_afastamentos", schema = "public")
public class VwAnalyticsAfastamento {
    @Id
    @Column(name = "analytics_id", length = Integer.MAX_VALUE)
    private String analyticsId;

    @Column(name = "funcionario_id")
    private Integer funcionarioId;

    @Column(name = "departamento_id")
    private Integer departamentoId;

    @Column(name = "departamento_codigo", length = Integer.MAX_VALUE)
    private String departamentoCodigo;

    @Column(name = "departamento", length = Integer.MAX_VALUE)
    private String departamento;

    @Column(name = "competencia")
    private LocalDate competencia;

    @Column(name = "ano")
    private Integer ano;

    @Column(name = "mes")
    private Integer mes;

    @Column(name = "periodo_inicio")
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim")
    private LocalDate periodoFim;

    @Column(name = "dias_afastado")
    private Long diasAfastado;

    @Column(name = "criticality_level", length = Integer.MAX_VALUE)
    private String criticalityLevel;

    @Column(name = "criticality_policy_id", length = Integer.MAX_VALUE)
    private String criticalityPolicyId;

    @Column(name = "criticality_policy_version", length = Integer.MAX_VALUE)
    private String criticalityPolicyVersion;
}
