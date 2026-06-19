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
        description = "Criterios de busca no quadro de membros de equipes taticas. "
                + "Relaciona equipe, colaborador, papel exercido e janelas de entrada ou saida da composicao.")
public class EquipeMembroFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Equipe", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.EQUIPES_TEAM_LOOKUP_OPTIONS,
            helpText = "Mostra membros vinculados a uma equipe específica.", icon = "groups")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "equipe.id")
    @Schema(
            description = "Equipe tatica cuja composicao de membros deve ser consultada.")
    private Integer equipeId;

    @UISchema(label = "Colaborador", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            helpText = "Mostra a participação de um colaborador em equipes.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador ou heroi cuja participacao em equipes deve ser localizada.")
    private Integer funcionarioId;

    @UISchema(label = "Papel na equipe", controlType = FieldControlType.SELECT, order = 30,
            helpText = "Filtra por uma função específica exercida na equipe.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Funcao exercida pelo colaborador dentro da equipe tatica.")
    private PapelEquipe papel;

    @UISchema(label = "Período de entrada", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 40,
            helpText = "Filtra pela janela em que o membro entrou na equipe.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataEntrada")
    @Schema(
            description = "Janela de entrada do colaborador na composicao da equipe.")
    private List<LocalDate> dataEntradaBetween;

    @UISchema(label = "Período de saída", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 50,
            helpText = "Filtra pela janela em que o membro saiu ou foi transferido.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataSaida")
    @Schema(
            description = "Janela de saida, desligamento ou transferencia do colaborador da equipe.")
    private List<LocalDate> dataSaidaBetween;

    @UISchema(label = "Mostrar papéis", controlType = FieldControlType.INLINE_MULTISELECT, order = 60,
            helpText = "Inclui membros que tenham qualquer um dos papéis selecionados.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "papel")
    @Schema(
            description = "Conjunto de papeis de equipe aceitos para compor o resultado da busca.")
    private List<PapelEquipe> papeisIn;

    @UISchema(label = "Ocultar papéis", controlType = FieldControlType.INLINE_MULTISELECT, order = 70,
            helpText = "Remove do resultado membros com os papéis selecionados.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN, relation = "papel")
    @Schema(
            description = "Conjunto de papeis de equipe que devem ser excluidos do resultado da busca.")
    private List<PapelEquipe> papeisNotIn;

    @UISchema(label = "Entrada em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 80,
            helpText = "Mostra membros que entraram na equipe em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "dataEntrada")
    @Schema(
            description = "Dia civil usado para localizar entradas de membros na equipe.")
    private LocalDate dataEntradaOn;

    @UISchema(label = "Saída em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 90,
            helpText = "Mostra membros que saíram da equipe em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "dataSaida")
    @Schema(
            description = "Dia civil usado para localizar saidas de membros da equipe.")
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
