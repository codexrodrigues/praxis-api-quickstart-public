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
                + "GenericFilter / POST /filter (demo).")
public class EquipamentoAlocacaoFilterDTO implements GenericFilterDTO {
    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Assets.EQUIPAMENTOS + "/options/filter", icon = "construction")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "equipamento.id")
    @Schema(
            description = "Cadeia de custodia de um item; EQUAL equipamentoId (demo).")
    private Integer equipamentoId;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Alocacoes vinculadas a um colaborador; EQUAL funcionarioId (demo).")
    private Integer funcionarioId;

    @UISchema(controlType = FieldControlType.SELECT, order = 30, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Ciclo de vida da alocacao; EQUAL AlocacaoStatus (demo).")
    private AlocacaoStatus status;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 40, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "inicio")
    @Schema(
            description = "Inicio da custodia; BETWEEN (demo).")
    private List<OffsetDateTime> inicioBetween;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_RANGE, order = 50, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "fim")
    @Schema(
            description = "Fim da custodia; BETWEEN (nulo se ativo) (demo).")
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
