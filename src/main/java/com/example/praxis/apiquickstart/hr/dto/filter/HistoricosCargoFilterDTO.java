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
        description = "Criterios de busca na linha do tempo de cargos e lotacoes de colaboradores. "
                + "Apoia analise de carreira, ocupacao de funcoes e periodos de vigencia em cada designacao.")
public class HistoricosCargoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Colaborador", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, helpText = "Filtrar histórico de um colaborador específico.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador cuja trajetoria de cargos e lotacoes deve ser consultada.")
    private Integer funcionarioId;

    @UISchema(label = "Cargo", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_OPTIONS, helpText = "Filtrar por cargo ocupado.", icon = "work")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "cargo.id")
    @Schema(
            description = "Cargo usado para localizar colaboradores que ja ocuparam ou ocupam a funcao.")
    private Integer cargoId;

    @UISchema(label = "Início da Lotação", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 30, helpText = "Buscar por período de início da lotação.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataInicio")
    @Schema(
            description = "Janela de inicio da designacao, usada para analisar entrada em cargo ou lotacao.")
    private List<LocalDate> dataInicioBetween;

    @UISchema(label = "Término da Lotação", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 40, helpText = "Buscar por período de término da lotação.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataFim")
    @Schema(
            description = "Janela de termino da designacao, usada para localizar encerramentos de cargo ou lotacao.")
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
