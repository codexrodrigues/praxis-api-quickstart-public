package com.example.praxis.apiquickstart.operationalassets.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operationalassets.enums.EquipamentoStatus;
import com.example.praxis.apiquickstart.operationalassets.enums.EquipamentoTipo;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "EquipamentoDTO",
        description = "Item operacional do inventario, como equipamento, traje ou ferramenta, com classificacao tatica, resistencia, custodiante e status de disponibilidade.")
public class EquipamentoDTO {
    @Schema(description = "Identificador do equipamento no inventario; referencia o recurso em URLs, alocacoes de custodia e relacionamentos operacionais.")
    private Integer id;

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Nome", controlType = FieldControlType.INPUT, required = true, maxLength = 200, icon = "badge")
    @Schema(
            description = "Designacao de inventario (ex. escudo, traje).")
    private String nome;

    @UISchema(label = "Tipo", controlType = FieldControlType.SELECT, icon = "category")
    @Schema(
            description = "Categoria tatica; EquipamentoTipo.")
    private EquipamentoTipo tipo;

    @Min(0)
    @UISchema(label = "Resistência", type = FieldDataType.NUMBER, icon = "inventory_2")
    @Schema(
            description = "Indice de protecao ou integridade (escala de dominio; 0 = baseline).")
    private Integer resistencia;

    @UISchema(label = "Proprietário", controlType = FieldControlType.ENTITY_LOOKUP,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            tableHidden = true, icon = "inventory_2")
    @Schema(
            description = "FK; colaborador com custodia formal (proprietarioId).")
    private Integer proprietarioId;

    @UISchema(label = "Proprietário", readOnly = true, formHidden = true, icon = "inventory_2")
    @Schema(
            description = "Nome do proprietario denormalizado para tabela (read model).")
    private String proprietarioNome;

    @NotNull
    @UISchema(label = "Status", controlType = FieldControlType.SELECT, required = true, icon = "toggle_on")
    @Schema(
            description = "Ciclo de vida do item; EquipamentoStatus.")
    private EquipamentoStatus status;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public EquipamentoTipo getTipo() { return tipo; }
    public void setTipo(EquipamentoTipo tipo) { this.tipo = tipo; }
    public Integer getResistencia() { return resistencia; }
    public void setResistencia(Integer resistencia) { this.resistencia = resistencia; }
    public Integer getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(Integer proprietarioId) { this.proprietarioId = proprietarioId; }
    public String getProprietarioNome() { return proprietarioNome; }
    public void setProprietarioNome(String proprietarioNome) { this.proprietarioNome = proprietarioNome; }
    public EquipamentoStatus getStatus() { return status; }
    public void setStatus(EquipamentoStatus status) { this.status = status; }
}



