package com.example.praxis.apiquickstart.hr.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.FieldControlType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Payload da action de aprovacao em lote de eventos de folha.
 *
 * <p>Além da seleção, o comando registra vigência e justificativa da decisão. Cada item que
 * transiciona recebe sua própria evidência de auditoria, embora a decisão seja tomada em lote.</p>
 */
@Schema(
        name = "BulkApproveEventosFolhaRequestDTO",
        description = "Decisão de aprovação em lote para eventos de folha. "
                + "Delimita os eventos, a vigência e a justificativa auditável; a ordem dos ids não implica prioridade de processamento.")
public class BulkApproveEventosFolhaRequestDTO {

    @NotEmpty
    @Size(max = 200)
    @UISchema(label = "IDs dos eventos", required = true, group = "Ação", order = 10, helpText = "Lista de identificadores dos eventos a aprovar.", icon = "format_list_numbered")
    @Schema(
            description = "Lista de chaves de EventosFolha a submeter; nao vazia, ate 200 itens. Duplicatas ou inexistentes podem ser ignoradas ou falhar por item (ver resultado). Ex.: 101, 102, 105 como ids distintos.")
    private List<@NotNull Integer> ids;

    @NotNull
    @UISchema(label = "Data efetiva", controlType = FieldControlType.DATE_PICKER, required = true, group = "Ação", order = 20,
            helpText = "Data em que a aprovação deve produzir efeitos no fechamento.", icon = "event")
    @Schema(description = "Data de eficácia da aprovação para o fechamento e a trilha auditável. Não altera retroativamente a data de criação do evento.", example = "2026-07-11")
    private LocalDate effectiveAt;

    @NotBlank
    @Size(max = 120)
    @UISchema(label = "Código do motivo", controlType = FieldControlType.INPUT, required = true, maxLength = 120, group = "Ação", order = 30,
            helpText = "Classificação de negócio da decisão de aprovação.", icon = "sell")
    @Schema(description = "Código de negócio que classifica a razão da aprovação em lote para auditoria e relatórios de folha.", example = "FECHAMENTO_CONFERIDO")
    private String reasonCode;

    @NotBlank
    @Size(max = 1000)
    @UISchema(label = "Comentário", controlType = FieldControlType.TEXTAREA, required = true, maxLength = 1000, group = "Ação", order = 40,
            helpText = "Justificativa da conferência que embasou a aprovação.", icon = "comment")
    @Schema(description = "Justificativa contextual da aprovação, preservada em cada transição confirmada do lote.", example = "Valores conferidos com os documentos da competência.")
    private String comment;

    @NotEmpty
    @UISchema(label = "Versões esperadas", required = true, formHidden = true, tableHidden = true, group = "Controle de concorrência", order = 50)
    @Schema(description = "ETag esperado por id do evento, obtido da leitura mais recente de cada linha. O lote é rejeitado antes de mudar dados se qualquer versão estiver ausente, inválida ou desatualizada.", example = "{\"101\": \"\\\"rv1-example\\\"\"}")
    private Map<@NotNull Integer, @NotBlank String> expectedVersions;

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }

    public LocalDate getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(LocalDate effectiveAt) { this.effectiveAt = effectiveAt; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Map<Integer, String> getExpectedVersions() { return expectedVersions; }
    public void setExpectedVersions(Map<Integer, String> expectedVersions) { this.expectedVersions = expectedVersions; }
}
