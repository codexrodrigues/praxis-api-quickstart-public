package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "UpdateAcordosRegulatorioDTO",
        description = "Comando para editar os metadados do acordo sem alterar o ciclo de vida. "
                + "O status e governado exclusivamente pelas workflow actions do recurso."
)
public class UpdateAcordosRegulatorioDTO {

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Designacao publica do acordo ou programa de atendimento a norma.")
    @UISchema(label = "Nome", controlType = FieldControlType.INPUT, required = true, maxLength = 200, icon = "badge")
    private String nome;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Pais, estado ou ente regulador competente.")
    @UISchema(label = "Jurisdição", controlType = FieldControlType.INPUT, required = true, maxLength = 200, icon = "label")
    private String jurisdicao;

    @Size(max = 4000)
    @Schema(description = "Texto de escopo, obrigacoes, evidencias e calendario de provas.")
    @UISchema(label = "Descrição", controlType = FieldControlType.TEXTAREA, maxLength = 4000, icon = "description")
    private String descricao;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getJurisdicao() { return jurisdicao; }
    public void setJurisdicao(String jurisdicao) { this.jurisdicao = jurisdicao; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
