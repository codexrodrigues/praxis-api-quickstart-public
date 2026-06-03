package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.BaseSigilo;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.math.BigDecimal;

@Schema(
        name = "UpdateBaseOpsContextDTO",
        description = "Patch operacional de base: sigilo, planeta, coordenadas (sem trocar nome/tipo de instalacao). "
                + "OpenAPI 3.1 e x-ui (demo).")
public class UpdateBaseOpsContextDTO {

    @UISchema(label = "Sigilo", controlType = FieldControlType.SELECT, order = 10, icon = "label")
    @Schema(
            description = "Novo nivel de compartilhamento de posicao; BaseSigilo.")
    private BaseSigilo sigilo;

    @Size(max = 120)
    @UISchema(label = "Planeta", controlType = FieldControlType.INPUT, maxLength = 120, order = 20, icon = "public")
    @Schema(
            description = "Mundo de operacao quando o teatro muda (multibase).")
    private String planeta;

    @UISchema(label = "Latitude", type = FieldDataType.NUMBER, order = 30, icon = "label")
    @Schema(
            description = "Atualizacao de latitude (WGS-84).")
    private BigDecimal latitude;

    @UISchema(label = "Longitude", type = FieldDataType.NUMBER, order = 40, icon = "label")
    @Schema(
            description = "Atualizacao de longitude (WGS-84).")
    private BigDecimal longitude;

    public BaseSigilo getSigilo() {
        return sigilo;
    }

    public void setSigilo(BaseSigilo sigilo) {
        this.sigilo = sigilo;
    }

    public String getPlaneta() {
        return planeta;
    }

    public void setPlaneta(String planeta) {
        this.planeta = planeta;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }
}


