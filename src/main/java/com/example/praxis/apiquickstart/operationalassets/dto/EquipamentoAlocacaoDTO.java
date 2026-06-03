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
        description = "Periodo de custodia de um equipamento por colaborador (inicio, fim, status de alocacao). "
                + "Payload de borda; nao e criterio de filtro. OpenAPI 3.1 e x-ui (demo).")
public class EquipamentoAlocacaoDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @NotNull
    @UISchema(label = "Equipamento", controlType = FieldControlType.SELECT,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Assets.EQUIPAMENTOS + "/options/filter", required = true, icon = "construction")
    @Schema(
            description = "FK; item alocado (equipamentoId).")
    private Integer equipamentoId;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.SELECT,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", required = true, icon = "badge")
    @Schema(
            description = "FK; colaborador que detem o item (funcionarioId).")
    private Integer funcionarioId;

    @NotNull
    @UISchema(label = "Início", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, required = true, icon = "event")
    @Schema(
            description = "Inicio de vigencia da custodia.")
    private OffsetDateTime inicio;

    @UISchema(label = "Fim", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "event")
    @Schema(
            description = "Fim da custodia; nulo se alocacao ainda ativa.")
    private OffsetDateTime fim;

    @UISchema(label = "Status", controlType = FieldControlType.SELECT, icon = "toggle_on")
    @Schema(
            description = "Ciclo de vida da alocacao; AlocacaoStatus.")
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


