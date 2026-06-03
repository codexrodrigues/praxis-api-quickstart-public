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
                + "GenericFilter / POST /filter (demo).")
public class VeiculoMissaoUsoFilterDTO implements GenericFilterDTO {
    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Assets.VEICULOS + "/options/filter", icon = "directions_car")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "veiculo.id")
    @Schema(
            description = "Sorties deste ativo; EQUAL veiculoId (demo).")
    private Integer veiculoId;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 20,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.MISSOES + "/options/filter", icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "missao.id")
    @Schema(
            description = "Uso de frota numa missao; EQUAL (demo).")
    private Integer missaoId;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 30,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "piloto.id")
    @Schema(
            description = "Quem conduz/ assina; EQUAL pilotoId (demo).")
    private Integer pilotoId;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 40, icon = "stacked_bar_chart")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "partida")
    @Schema(
            description = "Janela de decolagem/ saida; BETWEEN (demo).")
    private List<OffsetDateTime> partidaBetween;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 50, icon = "stacked_bar_chart")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "chegada")
    @Schema(
            description = "Janela de retorno/ pouso; BETWEEN (demo).")
    private List<OffsetDateTime> chegadaBetween;

    @UISchema(label = "Partida (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 60, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "partida")
    @Schema(
            description = "Saidas neste dia civil; ON_DATE (demo).")
    private LocalDate partidaOn;

    @UISchema(label = "Partida (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 70, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "partida")
    @Schema(
            description = "Sorties recentes; IN_LAST_DAYS (demo).")
    private Integer partidaLastDays;

    @UISchema(label = "Chegada (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 80, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "chegada")
    @Schema(
            description = "Chegadas neste dia; ON_DATE (demo).")
    private LocalDate chegadaOn;

    @UISchema(label = "Chegada (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 90, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "chegada")
    @Schema(
            description = "Fins de sortie recentes; IN_LAST_DAYS (demo).")
    private Integer chegadaLastDays;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 2000, order = 100, icon = "notes")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Notas tecnicas/ danos/ combustivel; LIKE (demo).")
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
