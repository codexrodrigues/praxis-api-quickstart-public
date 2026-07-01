package com.example.praxis.apiquickstart.hr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

@Entity
@Table(name = "legacy_pay_codes")
public class LegacyPayCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, length = 40)
    @DefaultSortColumn(priority = 1, ascending = true)
    private String code;

    @Column(name = "description", nullable = false, length = 240)
    @OptionLabel
    private String description;

    @Column(name = "payroll_category", nullable = false, length = 80)
    private String payrollCategory;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPayrollCategory() { return payrollCategory; }
    public void setPayrollCategory(String payrollCategory) { this.payrollCategory = payrollCategory; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
