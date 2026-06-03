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
@Table(name = "procurement_products")
public class ProcurementProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(name = "contract_id")
    private Integer contractId;

    @Column(name = "sku", nullable = false)
    private String sku;

    @OptionLabel
    @DefaultSortColumn(priority = 1, ascending = true)
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "stock_available")
    private Integer stockAvailable;

    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    @Column(name = "status")
    private String status;

    @Column(name = "disabled_reason")
    private String disabledReason;
}
