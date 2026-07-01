package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "LegacyPayCodeAuditLineDTO",
        description = "Audit line projected from the corporate payroll-code legacy adapter."
)
public class LegacyPayCodeAuditLineDTO {

    @Schema(description = "Stable audit line identifier exposed to related-row selection.")
    private String auditLineId;

    @Schema(description = "Identifier of the parent payroll code whose legacy operation generated the audit line.")
    private Integer payCodeId;

    @Schema(description = "Corporate operation recorded by the legacy adapter.")
    private String operation;

    @Schema(description = "Human-readable audit summary safe for UI related-list display.")
    private String summary;

    public LegacyPayCodeAuditLineDTO() {
    }

    public LegacyPayCodeAuditLineDTO(String auditLineId, Integer payCodeId, String operation, String summary) {
        this.auditLineId = auditLineId;
        this.payCodeId = payCodeId;
        this.operation = operation;
        this.summary = summary;
    }

    public String getAuditLineId() {
        return auditLineId;
    }

    public void setAuditLineId(String auditLineId) {
        this.auditLineId = auditLineId;
    }

    public Integer getPayCodeId() {
        return payCodeId;
    }

    public void setPayCodeId(Integer payCodeId) {
        this.payCodeId = payCodeId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
