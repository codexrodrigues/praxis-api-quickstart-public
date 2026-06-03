package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.BaseSigilo;
import com.example.praxis.apiquickstart.operations.enums.BaseTipo;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.math.BigDecimal;

@Schema(
        name = "BaseDTO",
        description = "Instalacao operacional (nome, tipo, classificacao de sigilo, georeferencia, planeta). "
                + "Payload de borda; nao e criterio de filtro. OpenAPI 3.1 e x-ui (demo).")
public class BaseDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Nome", controlType = FieldControlType.INPUT, required = true, maxLength = 200, icon = "badge")
    @Schema(
            description = "Designacao conhecida da base ou codinome nao classificado.")
    private String nome;

    @UISchema(label = "Tipo", controlType = FieldControlType.SELECT, icon = "category")
    @Schema(
            description = "Categoria tatica; BaseTipo (torre, bunker, hangar, etc.).")
    private BaseTipo tipo;

    @UISchema(label = "Sigilo", controlType = FieldControlType.SELECT, icon = "label")
    @Schema(
            description = "Nivel de compartilhamento de localizacao; BaseSigilo.")
    private BaseSigilo sigilo;

    @UISchema(label = "Latitude", type = FieldDataType.NUMBER, icon = "label")
    @Schema(
            description = "Coordenada de latitude (WGS-84) quando a base e georreferenciada.")
    private BigDecimal latitude;

    @UISchema(label = "Longitude", type = FieldDataType.NUMBER, icon = "label")
    @Schema(
            description = "Coordenada de longitude (WGS-84) quando a base e georreferenciada.")
    private BigDecimal longitude;

    @Size(max = 120)
    @UISchema(label = "Planeta", controlType = FieldControlType.INPUT, maxLength = 120, icon = "public")
    @Schema(
            description = "Mundo de operacao ou corpo celeste; ancora a missoes multiteatro.")
    private String planeta;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BaseTipo getTipo() { return tipo; }
    public void setTipo(BaseTipo tipo) { this.tipo = tipo; }
    public BaseSigilo getSigilo() { return sigilo; }
    public void setSigilo(BaseSigilo sigilo) { this.sigilo = sigilo; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getPlaneta() { return planeta; }
    public void setPlaneta(String planeta) { this.planeta = planeta; }
}


