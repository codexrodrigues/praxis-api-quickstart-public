package com.example.praxis.apiquickstart.operations.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.enums.MissaoEventoTipo;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "MissaoEventoFilterDTO",
        description = "Criterios de busca no diario/ timeline de eventos de missao (nao e o evento a editar so com filtrar). "
                + "Filtro por missao, tipo, texto e tempo; GenericFilter / POST /filter (demo).")
public class MissaoEventoFilterDTO implements GenericFilterDTO {
    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.MISSOES + "/options/filter", icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "missao.id")
    @Schema(
            description = "Apenas eventos desta missao; EQUAL (FK) (demo).")
    private Integer missaoId;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 20, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "ocorridoEm")
    @Schema(
            description = "Janela do marco; BETWEEN em ocorridoEm (demo).")
    private List<OffsetDateTime> ocorridoEmBetween;

    @UISchema(controlType = FieldControlType.SELECT, order = 30, icon = "category")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Um tipo; EQUAL MissaoEventoTipo (demo).")
    private MissaoEventoTipo tipo;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 4000, order = 40, icon = "description")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Texto de log, comunicacao, dano; LIKE (demo).")
    private String descricao;

    @UISchema(label = "Tipo (Incluir)", controlType = FieldControlType.SELECT, order = 50, icon = "category")
    @Filterable(operation = Filterable.FilterOperation.IN)
    @Schema(
            description = "Uniao de tipos; operacao IN (demo).")
    private List<MissaoEventoTipo> tiposIn;

    @UISchema(label = "Ocorrido em (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 60, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "ocorridoEm")
    @Schema(
            description = "Dia civil do evento; ON_DATE (demo).")
    private LocalDate ocorridoEmOn;

    @UISchema(label = "Ocorrido em (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 70, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "ocorridoEm")
    @Schema(
            description = "Corte movel; IN_LAST_DAYS (demo).")
    private Integer ocorridoEmLastDays;

    public Integer getMissaoId() { return missaoId; }
    public void setMissaoId(Integer missaoId) { this.missaoId = missaoId; }
    public List<OffsetDateTime> getOcorridoEmBetween() { return ocorridoEmBetween; }
    public void setOcorridoEmBetween(List<OffsetDateTime> ocorridoEmBetween) { this.ocorridoEmBetween = ocorridoEmBetween; }
    public MissaoEventoTipo getTipo() { return tipo; }
    public void setTipo(MissaoEventoTipo tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public List<MissaoEventoTipo> getTiposIn() { return tiposIn; }
    public void setTiposIn(List<MissaoEventoTipo> tiposIn) { this.tiposIn = tiposIn; }
    public LocalDate getOcorridoEmOn() { return ocorridoEmOn; }
    public void setOcorridoEmOn(LocalDate ocorridoEmOn) { this.ocorridoEmOn = ocorridoEmOn; }
    public Integer getOcorridoEmLastDays() { return ocorridoEmLastDays; }
    public void setOcorridoEmLastDays(Integer ocorridoEmLastDays) { this.ocorridoEmLastDays = ocorridoEmLastDays; }
}
