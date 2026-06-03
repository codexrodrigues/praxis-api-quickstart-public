package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "HistoricosCargoFilterDTO",
        description = "Criterios de busca na linha do tempo de cargos/ lotacao (nao e designacao a gravar so por filtrar). "
                + "Ancora plano de carreira e alocacao; GenericFilter / POST /filter (demo).")
public class HistoricosCargoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Colaborador", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Filtrar histórico de um colaborador específico.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Evolucao de cargos de um unico colaborador; EQUAL (FK) (demo).")
    private Integer funcionarioId;

    @UISchema(label = "Cargo", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.CARGOS + "/options/filter", helpText = "Filtrar por cargo ocupado.", icon = "work")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "cargo.id")
    @Schema(
            description = "Quem ja ocupou (ou ocupa) este cargo; EQUAL por id de Cargo (demo).")
    private Integer cargoId;

    @UISchema(label = "Início da Lotação", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 30, helpText = "Buscar por período de início da lotação.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataInicio")
    @Schema(
            description = "Mandato/ periodo: inicio; BETWEEN; cruza com dataFim (demo).")
    private List<LocalDate> dataInicioBetween;

    @UISchema(label = "Término da Lotação", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 40, helpText = "Buscar por período de término da lotação.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataFim")
    @Schema(
            description = "Mandato/ periodo: fim; BETWEEN (fim nulo fora do filtro) (demo).")
    private List<LocalDate> dataFimBetween;

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public Integer getCargoId() { return cargoId; }
    public void setCargoId(Integer cargoId) { this.cargoId = cargoId; }
    public List<LocalDate> getDataInicioBetween() { return dataInicioBetween; }
    public void setDataInicioBetween(List<LocalDate> dataInicioBetween) { this.dataInicioBetween = dataInicioBetween; }
    public List<LocalDate> getDataFimBetween() { return dataFimBetween; }
    public void setDataFimBetween(List<LocalDate> dataFimBetween) { this.dataFimBetween = dataFimBetween; }
}
