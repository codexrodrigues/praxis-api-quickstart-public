package com.example.praxis.apiquickstart.operationalassets.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operationalassets.enums.AlocacaoStatus;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.OffsetDateTime;

@Schema(
        name = "EquipamentoAlocacaoDTO",
        description = "Vínculo temporal de custódia entre um equipamento e um colaborador, registrando início, encerramento e estado da responsabilidade operacional.")
public class EquipamentoAlocacaoDTO {
    @Schema(description = "Identificador da alocação de equipamento; referencia o período de custódia em URLs e trilhas de auditoria.")
    private Integer id;

    @NotNull
    @UISchema(label = "Equipamento", controlType = FieldControlType.ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Assets.EQUIPAMENTOS_EQUIPMENT_LOOKUP_OPTIONS, required = true, icon = "construction")
    @Schema(
            description = "Equipamento entregue ao colaborador durante o período de custódia.")
    private Integer equipamentoId;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP, order = 20,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, required = true, icon = "badge")
    @Schema(
            description = "Colaborador responsável pela custódia do equipamento.")
    private Integer funcionarioId;

    @NotNull
    @UISchema(label = "Início", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, required = true, icon = "event", order = 30)
    @Schema(
            description = "Data e hora de início da responsabilidade de custódia.")
    private OffsetDateTime inicio;

    @UISchema(label = "Fim", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "event", order = 40)
    @Schema(
            description = "Data e hora de encerramento; permanece vazia enquanto a custódia estiver ativa.")
    private OffsetDateTime fim;

    @UISchema(label = "Status", controlType = FieldControlType.SELECT, icon = "toggle_on", order = 50)
    @Schema(
            description = "Situação atual da responsabilidade de custódia do equipamento.")
    private AlocacaoStatus status;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getEquipamentoId() { return equipamentoId; }
    public void setEquipamentoId(Integer equipamentoId) { this.equipamentoId = equipamentoId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public OffsetDateTime getInicio() { return inicio; }
    public void setInicio(OffsetDateTime inicio) { this.inicio = inicio; }
    public OffsetDateTime getFim() { return fim; }
    public void setFim(OffsetDateTime fim) { this.fim = fim; }
    public AlocacaoStatus getStatus() { return status; }
    public void setStatus(AlocacaoStatus status) { this.status = status; }
}

