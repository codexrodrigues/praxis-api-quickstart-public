package com.example.praxis.apiquickstart.procurement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@lombok.Getter
@lombok.Setter
@Entity
@Table(name = "procurement_purchase_orders")
public class ProcurementPurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(name = "supplier_id", nullable = false)
    private Integer supplierId;

    @Column(name = "contract_id")
    private Integer contractId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "currency")
    private String currency;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "status")
    private String status;

    @Column(name = "disabled_reason")
    private String disabledReason;

    @Column(name = "approved_at")
    private LocalDate approvedAt;

    @Column(name = "cancelled_at")
    private LocalDate cancelledAt;

    @Column(name = "received_at")
    private LocalDate receivedAt;
}
