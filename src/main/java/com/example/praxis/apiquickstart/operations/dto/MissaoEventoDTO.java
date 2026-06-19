package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.MissaoEventoTipo;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.OffsetDateTime;

@Schema(
        name = "MissaoEventoDTO",
        description = "Diario de bordo: marco da missao (timestamp, categoria, narrativa). "
                + "Registra acontecimentos da linha do tempo operacional para auditoria, debriefing e acompanhamento tatico.")
public class MissaoEventoDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @NotNull
    @UISchema(label = "Missão", controlType = FieldControlType.ENTITY_LOOKUP, required = true,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Operations.MISSOES_MISSION_LOOKUP_OPTIONS,
            tableHidden = true, icon = "flag")
    @Schema(
            description = "Missao operacional a que o evento da linha do tempo pertence.")
    private Integer missaoId;

    @UISchema(label = "Missão", readOnly = true, formHidden = true, icon = "flag")
    @Schema(
            description = "Titulo da missao denormalizado (read model).")
    private String missaoTitulo;

    @NotNull
    @UISchema(label = "Ocorrido Em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, required = true, icon = "event")
    @Schema(
            description = "Instante do acontecimento (linha de tempo tatica).")
    private OffsetDateTime ocorridoEm;

    @UISchema(label = "Tipo", controlType = FieldControlType.SELECT, icon = "category")
    @Schema(
            description = "Categoria (partida, contato, extracao, etc.); MissaoEventoTipo.")
    private MissaoEventoTipo tipo;

    @Size(max = 4000)
    @UISchema(label = "Descrição", controlType = FieldControlType.TEXTAREA, maxLength = 4000, icon = "description")
    @Schema(
            description = "Detalhe do evento; suporta SITREP e anexar decisoes de campo.")
    private String descricao;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getMissaoId() { return missaoId; }
    public void setMissaoId(Integer missaoId) { this.missaoId = missaoId; }
    public String getMissaoTitulo() { return missaoTitulo; }
    public void setMissaoTitulo(String missaoTitulo) { this.missaoTitulo = missaoTitulo; }
    public OffsetDateTime getOcorridoEm() { return ocorridoEm; }
    public void setOcorridoEm(OffsetDateTime ocorridoEm) { this.ocorridoEm = ocorridoEm; }
    public MissaoEventoTipo getTipo() { return tipo; }
    public void setTipo(MissaoEventoTipo tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}


