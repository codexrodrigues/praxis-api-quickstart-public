package com.example.praxis.apiquickstart.operations.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.SinalSocorroStatus;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "SinaisSocorroFilterDTO",
        description = "Criterios de busca em sinais de socorro e alertas operacionais. "
                + "Apoia triagem por origem, local, nivel de ameaca, status de atendimento e janelas de abertura ou encerramento.")
public class SinaisSocorroFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Origem do alerta", controlType = FieldControlType.INPUT, maxLength = 200, order = 10,
            helpText = "Digite parte da origem do alerta, como torre, sensor ou solicitante.", icon = "source")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho da origem do alerta, como torre, sensor, solicitante ou canal de comunicacao.")
    private String origem;

    @UISchema(label = "Local do alerta", controlType = FieldControlType.INPUT, maxLength = 200, order = 20,
            helpText = "Digite parte do local onde o sinal foi aberto.", icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do local ou cenario operacional onde o sinal de socorro foi aberto.")
    private String local;

    @UISchema(label = "Nível de ameaça", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 30,
            helpText = "Defina uma faixa de gravidade para priorizar a triagem.", icon = "warning")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "nivelAmeaca")
    @Schema(
            description = "Faixa de gravidade usada para priorizar sinais de socorro em triagem operacional.")
    private List<Integer> nivelAmeacaBetween;

    @UISchema(label = "Status do atendimento", controlType = FieldControlType.SELECT, order = 40,
            helpText = "Filtra por um único estado de atendimento.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Estado de atendimento do sinal de socorro, usado para diferenciar chamados abertos, atendidos, encerrados ou falsos.")
    private SinalSocorroStatus status;

    @UISchema(label = "Período de abertura", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 50,
            helpText = "Informe a janela em que o sinal de socorro foi aberto.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "abertoEm")
    @Schema(
            description = "Intervalo de datas em que o sinal de socorro foi aberto.")
    private List<OffsetDateTime> abertoEmBetween;

    @UISchema(label = "Período de fechamento", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 60,
            helpText = "Informe a janela em que o sinal de socorro foi encerrado.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "fechadoEm")
    @Schema(
            description = "Intervalo de datas em que o sinal de socorro foi encerrado.")
    private List<OffsetDateTime> fechadoEmBetween;

    @UISchema(label = "Mostrar status", controlType = FieldControlType.SELECT, order = 70,
            helpText = "Mostra apenas sinais de socorro nos status selecionados.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "status")
    @Schema(
            description = "Conjunto de estados de atendimento que devem aparecer no resultado da busca.")
    private List<SinalSocorroStatus> statusIn;

    @UISchema(label = "Ocultar status", controlType = FieldControlType.SELECT, order = 80,
            helpText = "Remove do resultado os sinais de socorro nos status selecionados.", icon = "toggle_off")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN, relation = "status")
    @Schema(
            description = "Conjunto de estados de atendimento que devem ser removidos do resultado da busca.")
    private List<SinalSocorroStatus> statusNotIn;

    @UISchema(label = "Aberto em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 90,
            helpText = "Escolha o dia exato em que o sinal foi aberto.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "abertoEm")
    @Schema(
            description = "Dia civil exato em que o sinal de socorro foi aberto.")
    private LocalDate abertoEmOn;

    @UISchema(label = "Aberto nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 100,
            helpText = "Informe quantos dias recentes devem ser considerados desde a abertura.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "abertoEm")
    @Schema(
            description = "Quantidade de dias recentes usada para localizar sinais de socorro abertos nesse recorte.")
    private Integer abertoEmLastDays;

    @UISchema(label = "Fechado em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 110,
            helpText = "Escolha o dia exato em que o sinal foi encerrado.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "fechadoEm")
    @Schema(
            description = "Dia civil exato em que o sinal de socorro foi encerrado.")
    private LocalDate fechadoEmOn;

    @UISchema(label = "Fechado nos últimos dias", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 120,
            helpText = "Informe quantos dias recentes devem ser considerados desde o fechamento.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "fechadoEm")
    @Schema(
            description = "Quantidade de dias recentes usada para localizar sinais de socorro encerrados nesse recorte.")
    private Integer fechadoEmLastDays;

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public List<Integer> getNivelAmeacaBetween() { return nivelAmeacaBetween; }
    public void setNivelAmeacaBetween(List<Integer> nivelAmeacaBetween) { this.nivelAmeacaBetween = nivelAmeacaBetween; }
    public SinalSocorroStatus getStatus() { return status; }
    public void setStatus(SinalSocorroStatus status) { this.status = status; }
    public List<OffsetDateTime> getAbertoEmBetween() { return abertoEmBetween; }
    public void setAbertoEmBetween(List<OffsetDateTime> abertoEmBetween) { this.abertoEmBetween = abertoEmBetween; }
    public List<OffsetDateTime> getFechadoEmBetween() { return fechadoEmBetween; }
    public void setFechadoEmBetween(List<OffsetDateTime> fechadoEmBetween) { this.fechadoEmBetween = fechadoEmBetween; }

    public List<SinalSocorroStatus> getStatusIn() { return statusIn; }
    public void setStatusIn(List<SinalSocorroStatus> statusIn) { this.statusIn = statusIn; }
    public List<SinalSocorroStatus> getStatusNotIn() { return statusNotIn; }
    public void setStatusNotIn(List<SinalSocorroStatus> statusNotIn) { this.statusNotIn = statusNotIn; }
    public LocalDate getAbertoEmOn() { return abertoEmOn; }
    public void setAbertoEmOn(LocalDate abertoEmOn) { this.abertoEmOn = abertoEmOn; }
    public Integer getAbertoEmLastDays() { return abertoEmLastDays; }
    public void setAbertoEmLastDays(Integer abertoEmLastDays) { this.abertoEmLastDays = abertoEmLastDays; }
    public LocalDate getFechadoEmOn() { return fechadoEmOn; }
    public void setFechadoEmOn(LocalDate fechadoEmOn) { this.fechadoEmOn = fechadoEmOn; }
    public Integer getFechadoEmLastDays() { return fechadoEmLastDays; }
    public void setFechadoEmLastDays(Integer fechadoEmLastDays) { this.fechadoEmLastDays = fechadoEmLastDays; }
}
