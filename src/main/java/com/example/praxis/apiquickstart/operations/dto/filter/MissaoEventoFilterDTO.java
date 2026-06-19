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
        description = "Criterios de busca na linha do tempo de eventos de uma missao. "
                + "Apoia reconstrucao operacional por missao, tipo de evento, narrativa e periodo de ocorrencia.")
public class MissaoEventoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Missão", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.MISSOES_MISSION_LOOKUP_OPTIONS,
            helpText = "Mostra eventos registrados para uma missão específica.", icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "missao.id")
    @Schema(
            description = "Missao cuja linha do tempo de eventos deve ser consultada.")
    private Integer missaoId;

    @UISchema(label = "Período do evento", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 20,
            helpText = "Filtra pela janela de data e hora em que o evento ocorreu.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "ocorridoEm")
    @Schema(
            description = "Janela de data e hora em que os eventos da missao ocorreram.")
    private List<OffsetDateTime> ocorridoEmBetween;

    @UISchema(label = "Tipo de evento", controlType = FieldControlType.SELECT, order = 30,
            helpText = "Seleciona uma categoria específica da linha do tempo.", icon = "category")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Categoria do evento registrada na linha do tempo da missao.")
    private MissaoEventoTipo tipo;

    @UISchema(label = "Descrição do evento", controlType = FieldControlType.INPUT, maxLength = 4000, order = 40,
            helpText = "Busca por palavras-chave no log, comunicação ou relato do evento.", icon = "description")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do log, comunicacao, relato ou dano descrito no evento da missao.")
    private String descricao;

    @UISchema(label = "Mostrar tipos", controlType = FieldControlType.SELECT, order = 50,
            helpText = "Inclui eventos com qualquer um dos tipos selecionados.", icon = "category")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "tipo")
    @Schema(
            description = "Conjunto de tipos de evento aceitos para compor a linha do tempo retornada.")
    private List<MissaoEventoTipo> tiposIn;

    @UISchema(label = "Ocorrido em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 60,
            helpText = "Mostra eventos ocorridos em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "ocorridoEm")
    @Schema(
            description = "Dia civil usado para localizar eventos de missao ocorridos em uma data especifica.")
    private LocalDate ocorridoEmOn;

    @UISchema(label = "Ocorrido nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 70,
            helpText = "Informe quantos dias recentes devem ser considerados para a linha do tempo.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "ocorridoEm")
    @Schema(
            description = "Janela relativa para localizar eventos de missao ocorridos nos ultimos N dias.")
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
