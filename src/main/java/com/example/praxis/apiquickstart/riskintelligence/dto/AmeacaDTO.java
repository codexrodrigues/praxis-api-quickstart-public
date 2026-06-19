package com.example.praxis.apiquickstart.riskintelligence.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.riskintelligence.enums.AmeacaClasse;
import com.example.praxis.apiquickstart.riskintelligence.enums.AmeacaStatus;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.math.BigDecimal;

@Schema(
        name = "AmeacaDTO",
        description = "Registro mestre de uma ameaca acompanhada pela inteligencia de risco, reunindo identidade operacional, classe, teatro principal, nivel de perigo, status e recompensa.")
public class AmeacaDTO {
    @Schema(description = "Identificador da ameaca no catalogo de risco; referencia o recurso em URLs, missoes relacionadas e relacionamentos semanticos.")
    private Integer id;

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Nome", controlType = FieldControlType.INPUT, required = true, maxLength = 200, icon = "warning")
    @Schema(
            description = "Codinome ou designacao publica da ameaca.")
    private String nome;

    @UISchema(label = "Classe", controlType = FieldControlType.SELECT, icon = "category")
    @Schema(
            description = "Taxonomia (villain, organizacao, fenomeno, etc.); AmeacaClasse.")
    private AmeacaClasse classe;

    @Size(max = 120)
    @UISchema(label = "Planeta", controlType = FieldControlType.INPUT, maxLength = 120, icon = "public")
    @Schema(
            description = "Planeta ou regiao principal de atuacao.")
    private String planeta;

    @Min(0)
    @UISchema(label = "Nível", type = FieldDataType.NUMBER, icon = "trending_up")
    @Schema(
            description = "Nivel de ameaca operacional (escala interna; 0 = baseline).")
    private Integer nivel;

    @NotNull
    @UISchema(label = "Status", controlType = FieldControlType.SELECT, required = true, icon = "toggle_on")
    @Schema(
            description = "Ciclo de vida no catalogo (ativo, neutralizado, etc.); AmeacaStatus.")
    private AmeacaStatus status;

    @DecimalMin("0.00")
    @UISchema(label = "Recompensa", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, icon = "payments")
    @Schema(
            description = "Valor oferecido por captura ou intel (moeda de referencia do tenant).")
    private BigDecimal recompensa;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public AmeacaClasse getClasse() { return classe; }
    public void setClasse(AmeacaClasse classe) { this.classe = classe; }
    public String getPlaneta() { return planeta; }
    public void setPlaneta(String planeta) { this.planeta = planeta; }
    public Integer getNivel() { return nivel; }
    public void setNivel(Integer nivel) { this.nivel = nivel; }
    public AmeacaStatus getStatus() { return status; }
    public void setStatus(AmeacaStatus status) { this.status = status; }
    public BigDecimal getRecompensa() { return recompensa; }
    public void setRecompensa(BigDecimal recompensa) { this.recompensa = recompensa; }
}

