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
        description = "Criterios de busca em pedidos de socorro/ alertas (nao e o ticket a encerrar so com filtrar). "
                + "Janelas abertura/ fecho; GenericFilter / POST /filter (demo).")
public class SinaisSocorroFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 10, icon = "source")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Fonte do alerta (torre, cidadao, sensor); LIKE (demo).")
    private String origem;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 20, icon = "location_on")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Cenario onde o sinal foi aberto; LIKE (demo).")
    private String local;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 30, icon = "warning")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "nivelAmeaca")
    @Schema(
            description = "Escala de ameaca iminente; BETWEEN (triagem) (demo).")
    private List<Integer> nivelAmeacaBetween;

    @UISchema(controlType = FieldControlType.SELECT, order = 40, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Estado unico (aberto, atendido, falso); EQUAL SinalSocorroStatus (demo).")
    private SinalSocorroStatus status;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 50, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "abertoEm")
    @Schema(
            description = "Janela de abertura; BETWEEN (fila) (demo).")
    private List<OffsetDateTime> abertoEmBetween;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 60, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "fechadoEm")
    @Schema(
            description = "Janela de fechamento; BETWEEN (resolucao) (demo).")
    private List<OffsetDateTime> fechadoEmBetween;

    @UISchema(label = "Status (Incluir)", controlType = FieldControlType.SELECT, order = 70, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.IN)
    @Schema(
            description = "Uniao de estados; operacao IN (demo).")
    private List<SinalSocorroStatus> statusIn;

    @UISchema(label = "Status (Excluir)", controlType = FieldControlType.SELECT, order = 80, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN)
    @Schema(
            description = "Excluir estados; NOT_IN (demo).")
    private List<SinalSocorroStatus> statusNotIn;

    @UISchema(label = "Aberto em (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 90, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "abertoEm")
    @Schema(
            description = "Dia civil do inicio; ON_DATE (demo).")
    private LocalDate abertoEmOn;

    @UISchema(label = "Aberto em (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 100, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "abertoEm")
    @Schema(
            description = "Corte movel; IN_LAST_DAYS (demo).")
    private Integer abertoEmLastDays;

    @UISchema(label = "Fechado em (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 110, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "fechadoEm")
    @Schema(
            description = "Fechamentos neste dia; ON_DATE (demo).")
    private LocalDate fechadoEmOn;

    @UISchema(label = "Fechado em (Últimos N dias)", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 120, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "fechadoEm")
    @Schema(
            description = "Fechados recentes; IN_LAST_DAYS (demo).")
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
