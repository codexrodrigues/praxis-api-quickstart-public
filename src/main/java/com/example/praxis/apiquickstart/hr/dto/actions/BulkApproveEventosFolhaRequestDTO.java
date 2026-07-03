package com.example.praxis.apiquickstart.hr.dto.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.util.List;

/**
 * Payload da action de aprovacao em lote de eventos de folha.
 *
 * <p>O contrato e centrado apenas nos ids selecionados porque a semantica da action ja esta
 * embutida no proprio endpoint/workflow publicado pelo recurso.</p>
 */
@Schema(
        name = "BulkApproveEventosFolhaRequestDTO",
        description = "Selecao de eventos de folha a aprovar numa unica acao. "
                + "A semantica de aprovacao vem do endpoint; este payload delimita quais eventos entram no lote e a ordem nao implica prioridade de processamento.")
public class BulkApproveEventosFolhaRequestDTO {

    @NotEmpty
    @Size(max = 200)
    @UISchema(label = "IDs dos eventos", required = true, group = "Ação", order = 10, helpText = "Lista de identificadores dos eventos a aprovar.", icon = "format_list_numbered")
    @Schema(
            description = "Lista de chaves de EventosFolha a submeter; nao vazia, ate 200 itens. Duplicatas ou inexistentes podem ser ignoradas ou falhar por item (ver resultado). Ex.: 101, 102, 105 como ids distintos.")
    private List<@NotNull Integer> ids;

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }
}
