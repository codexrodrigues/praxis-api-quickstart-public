package com.example.praxis.apiquickstart.operationalassets.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operationalassets.enums.AlocacaoStatus;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(
        name = "EquipamentoAlocacaoFilterDTO",
        description = "Criterios de busca no historico de alocacoes (equipamento x colaborador, janela e status); nao e a cedencia a ajustar so com filtrar. "
                + "Usado para rastrear cadeia de custodia, responsavel, vigencia e estado da alocacao.")
public class EquipamentoAlocacaoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Equipamento", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Assets.EQUIPAMENTOS_EQUIPMENT_LOOKUP_OPTIONS,
            helpText = "Mostra o histórico de alocações de um item específico.", icon = "construction")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "equipamento.id")
    @Schema(
            description = "Equipamento cuja cadeia de custodia deve ser consultada.")
    private Integer equipamentoId;

    @UISchema(label = "Colaborador", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            helpText = "Filtra as alocações vinculadas a um colaborador ou herói.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador ou operador vinculado as alocacoes de equipamento buscadas.")
    private Integer funcionarioId;

    @UISchema(label = "Status da alocação", controlType = FieldControlType.SELECT, order = 30,
            helpText = "Diferencia alocações ativas, encerradas ou em transição.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Status da alocacao, usado para separar custodias ativas, encerradas ou em transicao.")
    private AlocacaoStatus status;

    @UISchema(label = "Período de início", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 40,
            helpText = "Filtra pela janela em que a custódia do equipamento começou.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "inicio")
    @Schema(
            description = "Janela em que a custodia do equipamento foi iniciada.")
    private List<OffsetDateTime> inicioBetween;

    @UISchema(label = "Período de término", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 50,
            helpText = "Filtra pela janela em que a custódia foi encerrada.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "fim")
    @Schema(
            description = "Janela em que a custodia foi encerrada; alocacoes ativas podem nao ter fim preenchido.")
    private List<OffsetDateTime> fimBetween;

    public Integer getEquipamentoId() { return equipamentoId; }
    public void setEquipamentoId(Integer equipamentoId) { this.equipamentoId = equipamentoId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public AlocacaoStatus getStatus() { return status; }
    public void setStatus(AlocacaoStatus status) { this.status = status; }
    public List<OffsetDateTime> getInicioBetween() { return inicioBetween; }
    public void setInicioBetween(List<OffsetDateTime> inicioBetween) { this.inicioBetween = inicioBetween; }
    public List<OffsetDateTime> getFimBetween() { return fimBetween; }
    public void setFimBetween(List<OffsetDateTime> fimBetween) { this.fimBetween = fimBetween; }
}
