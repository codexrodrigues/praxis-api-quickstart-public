package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "ReviewBaseAcessoDTO",
        description = "Corpo de aprovacao de credencial de base: classificacao de acesso a conceder. "
                + "OpenAPI 3.1 e x-ui (demo).")
public class ReviewBaseAcessoDTO {

    @NotBlank
    @Size(max = 255)
    @UISchema(label = "Nivel de acesso", type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, required = true, maxLength = 255, order = 10, icon = "trending_up")
    @Schema(
            description = "Nivel aprovado (ex. TSI-2); alinha-se a politica de compartilhamento.")
    private String nivelAcesso;

    public String getNivelAcesso() {
        return nivelAcesso;
    }

    public void setNivelAcesso(String nivelAcesso) {
        this.nivelAcesso = nivelAcesso;
    }
}

