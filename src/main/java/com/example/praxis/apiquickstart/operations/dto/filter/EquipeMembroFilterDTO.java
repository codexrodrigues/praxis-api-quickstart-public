package com.example.praxis.apiquickstart.operations.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.enums.PapelEquipe;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "EquipeMembroFilterDTO",
        description = "Criterios de busca no quadro de membros (equipe x colaborador); nao e a nomeacao a editar so com filtrar. "
                + "Janelas de entrada/ saida; GenericFilter / POST /filter (demo).")
public class EquipeMembroFilterDTO implements GenericFilterDTO {
    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.EQUIPES + "/options/filter", icon = "groups")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "equipe.id")
    @Schema(
            description = "Apenas escalados nesta equipe; EQUAL (FK) (demo).")
    private Integer equipeId;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Todas as equipas de um colaborador; EQUAL (demo).")
    private Integer funcionarioId;

    @UISchema(controlType = FieldControlType.SELECT, order = 30, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Papel unico; EQUAL PapelEquipe (demo).")
    private PapelEquipe papel;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 40, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataEntrada")
    @Schema(
            description = "Janela de alistamento; BETWEEN (demo).")
    private List<LocalDate> dataEntradaBetween;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 50, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataSaida")
    @Schema(
            description = "Janela de desligamento/ transferencia; BETWEEN (demo).")
    private List<LocalDate> dataSaidaBetween;

    @UISchema(label = "Papel (Incluir)", controlType = FieldControlType.INLINE_MULTISELECT, order = 60, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.IN)
    @Schema(
            description = "Uniao de funcoes; operacao IN (demo).")
    private List<PapelEquipe> papeisIn;

    @UISchema(label = "Papel (Excluir)", controlType = FieldControlType.INLINE_MULTISELECT, order = 70, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN)
    @Schema(
            description = "Excluir funcoes; NOT_IN (demo).")
    private List<PapelEquipe> papeisNotIn;

    @UISchema(label = "Entrada (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 80, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "dataEntrada")
    @Schema(
            description = "Alistamento neste dia; ON_DATE (demo).")
    private LocalDate dataEntradaOn;

    @UISchema(label = "Saída (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 90, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "dataSaida")
    @Schema(
            description = "Saidas neste dia; ON_DATE (demo).")
    private LocalDate dataSaidaOn;

    public Integer getEquipeId() { return equipeId; }
    public void setEquipeId(Integer equipeId) { this.equipeId = equipeId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public PapelEquipe getPapel() { return papel; }
    public void setPapel(PapelEquipe papel) { this.papel = papel; }
    public List<LocalDate> getDataEntradaBetween() { return dataEntradaBetween; }
    public void setDataEntradaBetween(List<LocalDate> dataEntradaBetween) { this.dataEntradaBetween = dataEntradaBetween; }
    public List<LocalDate> getDataSaidaBetween() { return dataSaidaBetween; }
    public void setDataSaidaBetween(List<LocalDate> dataSaidaBetween) { this.dataSaidaBetween = dataSaidaBetween; }

    public List<PapelEquipe> getPapeisIn() { return papeisIn; }
    public void setPapeisIn(List<PapelEquipe> papeisIn) { this.papeisIn = papeisIn; }
    public List<PapelEquipe> getPapeisNotIn() { return papeisNotIn; }
    public void setPapeisNotIn(List<PapelEquipe> papeisNotIn) { this.papeisNotIn = papeisNotIn; }
    public LocalDate getDataEntradaOn() { return dataEntradaOn; }
    public void setDataEntradaOn(LocalDate dataEntradaOn) { this.dataEntradaOn = dataEntradaOn; }
    public LocalDate getDataSaidaOn() { return dataSaidaOn; }
    public void setDataSaidaOn(LocalDate dataSaidaOn) { this.dataSaidaOn = dataSaidaOn; }
}
