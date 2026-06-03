package com.example.praxis.apiquickstart.procurement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

import java.time.LocalDate;

@lombok.Getter
@lombok.Setter
@Entity
@Table(name = "procurement_contracts")
public class ProcurementContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(name = "supplier_id", nullable = false)
    private Integer supplierId;

    @OptionLabel
    @DefaultSortColumn(priority = 1, ascending = true)
    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "currency")
    private String currency;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "status")
    private String status;

    @Column(name = "disabled_reason")
    private String disabledReason;
}
