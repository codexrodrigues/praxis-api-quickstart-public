package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.Severidade;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(
        name = "IncidenteDTO",
        description = "Registo de incidente pos-missao (relato, severidade, local, vitimas, impacto economico). "
                + "Consolida evidencias operacionais para resposta, auditoria, cobertura indenizatoria e analise de risco.")
public class IncidenteDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @UISchema(label = "Missão", controlType = FieldControlType.ENTITY_LOOKUP,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Operations.MISSOES_MISSION_LOOKUP_OPTIONS, icon = "flag")
    @Schema(
            description = "FK; missao onde o incidente ocorreu (missaoId).")
    private Integer missaoId;

    @NotBlank
    @Size(max = 2000)
    @UISchema(label = "Descrição", controlType = FieldControlType.TEXTAREA, required = true, maxLength = 2000, icon = "description")
    @Schema(
            description = "Narrativa do fato e evolucao; texto livre.")
    private String descricao;

    @UISchema(label = "Severidade", controlType = FieldControlType.SELECT, icon = "emergency")
    @Schema(
            description = "Escala tatica; Severidade.")
    private Severidade severidade;

    @Size(max = 200)
    @UISchema(label = "Local", controlType = FieldControlType.INPUT, maxLength = 200, icon = "location_on")
    @Schema(
            description = "Teatro ou ponto de impacto (endereco, coordenada textual).")
    private String local;

    @NotNull
    @UISchema(label = "Ocorrido em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, required = true, icon = "event")
    @Schema(
            description = "Timestamp do incidente (fuso ancorado no registo).")
    private OffsetDateTime ocorridoEm;

    @UISchema(label = "Danos Civis", type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, icon = "calendar_today")
    @Schema(
            description = "Prejuizo estimado a patrimonio civil; moeda de referencia do tenant.")
    private BigDecimal danosCivis;

    @Min(0)
    @UISchema(label = "Feridos", type = FieldDataType.NUMBER, icon = "health_and_safety")
    @Schema(
            description = "Contagem de lesoes; inclui nao-heroi quando aplicavel.")
    private Integer feridos;

    @Min(0)
    @UISchema(label = "Mortos", type = FieldDataType.NUMBER, icon = "health_and_safety")
    @Schema(
            description = "Falecidos vinculados ao incidente; para acordo com regulador.")
    private Integer mortos;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getMissaoId() { return missaoId; }
    public void setMissaoId(Integer missaoId) { this.missaoId = missaoId; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Severidade getSeveridade() { return severidade; }
    public void setSeveridade(Severidade severidade) { this.severidade = severidade; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public OffsetDateTime getOcorridoEm() { return ocorridoEm; }
    public void setOcorridoEm(OffsetDateTime ocorridoEm) { this.ocorridoEm = ocorridoEm; }
    public BigDecimal getDanosCivis() { return danosCivis; }
    public void setDanosCivis(BigDecimal danosCivis) { this.danosCivis = danosCivis; }
    public Integer getFeridos() { return feridos; }
    public void setFeridos(Integer feridos) { this.feridos = feridos; }
    public Integer getMortos() { return mortos; }
    public void setMortos(Integer mortos) { this.mortos = mortos; }
}


