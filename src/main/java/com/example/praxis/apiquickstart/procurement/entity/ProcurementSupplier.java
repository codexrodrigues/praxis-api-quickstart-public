package com.example.praxis.apiquickstart.procurement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

@lombok.Getter
@lombok.Setter
@Entity
@Table(name = "procurement_suppliers")
public class ProcurementSupplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(name = "code", nullable = false)
    private String code;

    @OptionLabel
    @DefaultSortColumn(priority = 1, ascending = true)
    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "homologation_status")
    private String homologationStatus;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "status")
    private String status;

    @Column(name = "disabled_reason")
    private String disabledReason;
}
