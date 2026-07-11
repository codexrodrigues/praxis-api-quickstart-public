package com.example.praxis.apiquickstart.hr.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.LocalDate;

/** Command that records the governed decision to deactivate an employee. */
@Schema(description = "Comando de inativação de funcionário. Registra a vigência e a justificativa da decisão de RH; não é uma edição cadastral do campo ativo.")
public class FuncionarioDeactivateRequestDTO {

    @NotNull
    @UISchema(label = "Data efetiva", controlType = FieldControlType.DATE_PICKER, required = true, order = 10)
    @Schema(description = "Data a partir da qual a inativação produz efeitos operacionais para o vínculo do funcionário.")
    private LocalDate effectiveAt;

    @NotBlank
    @Size(max = 120)
    @UISchema(label = "Código do motivo", controlType = FieldControlType.INPUT, required = true, maxLength = 120, order = 20)
    @Schema(description = "Código de negócio que classifica a razão da inativação para auditoria e relatórios de RH.")
    private String reasonCode;

    @NotBlank
    @Size(max = 1000)
    @UISchema(label = "Comentário", controlType = FieldControlType.TEXTAREA, required = true, maxLength = 1000, order = 30)
    @Schema(description = "Justificativa contextual da decisão, preservada no histórico de transições do funcionário.")
    private String comment;

    public LocalDate getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(LocalDate effectiveAt) { this.effectiveAt = effectiveAt; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
