package com.example.praxis.apiquickstart.operationalassets.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "VeiculoMissaoUsoFilterDTO",
        description = "Criterios de busca em registos de sortie (veiculo alocado a missao, piloto, janela partida/ chegada); nao e o log a corrigir so com filtrar. "
                + "Usado para rastrear uso de frota em missoes, janela de deslocamento, piloto e observacoes operacionais.")
public class VeiculoMissaoUsoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Veículo", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Assets.VEICULOS_VEHICLE_LOOKUP_OPTIONS,
            helpText = "Mostra usos de missão de um veículo específico.", icon = "directions_car")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "veiculo.id")
    @Schema(
            description = "Veiculo cuja participacao em missoes deve ser consultada.")
    private Integer veiculoId;

    @UISchema(label = "Missão", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.MISSOES_MISSION_LOOKUP_OPTIONS,
            helpText = "Filtra usos de frota associados a uma missão.", icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "missao.id")
    @Schema(
            description = "Missao operacional associada ao registro de uso da frota.")
    private Integer missaoId;

    @UISchema(label = "Piloto", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 30,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            helpText = "Filtra registros conduzidos ou assinados por um piloto.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "piloto.id")
    @Schema(
            description = "Piloto ou responsavel que conduziu, assinou ou acompanhou a sortie.")
    private Integer pilotoId;

    @UISchema(label = "Período de partida", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 40,
            helpText = "Filtra pela janela de saída ou decolagem do veículo.", icon = "stacked_bar_chart")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "partida")
    @Schema(
            description = "Janela de partida, saida ou decolagem do veiculo na missao.")
    private List<OffsetDateTime> partidaBetween;

    @UISchema(label = "Período de chegada", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 50,
            helpText = "Filtra pela janela de retorno ou pouso do veículo.", icon = "stacked_bar_chart")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "chegada")
    @Schema(
            description = "Janela de chegada, retorno ou pouso registrada para o uso do veiculo.")
    private List<OffsetDateTime> chegadaBetween;

    @UISchema(label = "Partida em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 60,
            helpText = "Mostra registros com partida em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "partida")
    @Schema(
            description = "Dia civil usado para localizar partidas registradas nessa data.")
    private LocalDate partidaOn;

    @UISchema(label = "Partida nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 70,
            helpText = "Informe quantos dias recentes devem ser considerados para a partida.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "partida")
    @Schema(
            description = "Janela relativa para localizar partidas ocorridas nos ultimos N dias.")
    private Integer partidaLastDays;

    @UISchema(label = "Chegada em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 80,
            helpText = "Mostra registros com chegada em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "chegada")
    @Schema(
            description = "Dia civil usado para localizar chegadas registradas nessa data.")
    private LocalDate chegadaOn;

    @UISchema(label = "Chegada nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 90,
            helpText = "Informe quantos dias recentes devem ser considerados para a chegada.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "chegada")
    @Schema(
            description = "Janela relativa para localizar chegadas ou encerramentos de sortie nos ultimos N dias.")
    private Integer chegadaLastDays;

    @UISchema(label = "Observação", controlType = FieldControlType.INPUT, maxLength = 2000, order = 100,
            helpText = "Busca por notas de operação, dano, combustível ou logística.", icon = "notes")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho de observacoes sobre operacao, dano, combustivel, logistica ou contexto da sortie.")
    private String observacao;

    public Integer getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Integer veiculoId) { this.veiculoId = veiculoId; }
    public Integer getMissaoId() { return missaoId; }
    public void setMissaoId(Integer missaoId) { this.missaoId = missaoId; }
    public Integer getPilotoId() { return pilotoId; }
    public void setPilotoId(Integer pilotoId) { this.pilotoId = pilotoId; }
    public List<OffsetDateTime> getPartidaBetween() { return partidaBetween; }
    public void setPartidaBetween(List<OffsetDateTime> partidaBetween) { this.partidaBetween = partidaBetween; }
    public List<OffsetDateTime> getChegadaBetween() { return chegadaBetween; }
    public void setChegadaBetween(List<OffsetDateTime> chegadaBetween) { this.chegadaBetween = chegadaBetween; }
    public LocalDate getPartidaOn() { return partidaOn; }
    public void setPartidaOn(LocalDate partidaOn) { this.partidaOn = partidaOn; }
    public Integer getPartidaLastDays() { return partidaLastDays; }
    public void setPartidaLastDays(Integer partidaLastDays) { this.partidaLastDays = partidaLastDays; }
    public LocalDate getChegadaOn() { return chegadaOn; }
    public void setChegadaOn(LocalDate chegadaOn) { this.chegadaOn = chegadaOn; }
    public Integer getChegadaLastDays() { return chegadaLastDays; }
    public void setChegadaLastDays(Integer chegadaLastDays) { this.chegadaLastDays = chegadaLastDays; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
