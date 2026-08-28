package com.example.praxis.apiquickstart.procurement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

/** Linha cumulativa e somente leitura do funil de fornecedores por empresa compradora. */
@lombok.Getter
@lombok.Setter
@Entity
@Immutable
@Table(name = "vw_supplier_procurement_funnel", schema = "public")
public class VwSupplierProcurementFunnel {
    @Id
    @Column(name = "analytics_id", nullable = false, length = 80)
    private String analyticsId;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @DefaultSortColumn(priority = 1, ascending = true)
    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Column(name = "stage_key", nullable = false, length = 40)
    private String stageKey;

    @OptionLabel
    @Column(name = "stage_label", nullable = false, length = 80)
    private String stageLabel;

    @Column(name = "volume", nullable = false)
    private Long volume;

    @Column(name = "previous_volume")
    private Long previousVolume;

    @Column(name = "conversion_rate", precision = 12, scale = 6)
    private BigDecimal conversionRate;

    @Column(name = "drop_off_count", nullable = false)
    private Long dropOffCount;

    @Column(name = "drop_off_rate", precision = 12, scale = 6)
    private BigDecimal dropOffRate;
}
