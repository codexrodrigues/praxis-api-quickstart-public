package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "ReviewAcordosRegulatorioDTO",
        description = "Corpo de submissao ou revisao de acordo regulatorio: jurisdicao e texto normativo. "
                + "Atualiza o contexto documental de compliance sem executar transicao de workflow do acordo.")
public class ReviewAcordosRegulatorioDTO {

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Jurisdição", controlType = FieldControlType.INPUT, required = true, maxLength = 200, icon = "label")
    @Schema(
            description = "Orgao ou esfera legal competente (pacto, ANV, TSI, etc.).")
    private String jurisdicao;

    @Size(max = 4000)
    @UISchema(label = "Descrição", controlType = FieldControlType.TEXTAREA, maxLength = 4000, icon = "description")
    @Schema(
            description = "Resumo, obrigacoes e limites de uso da licenca derivada do acordo.")
    private String descricao;

    public String getJurisdicao() {
        return jurisdicao;
    }

    public void setJurisdicao(String jurisdicao) {
        this.jurisdicao = jurisdicao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}

