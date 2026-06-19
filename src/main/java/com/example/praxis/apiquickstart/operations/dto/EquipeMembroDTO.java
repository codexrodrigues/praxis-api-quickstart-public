package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.PapelEquipe;
import jakarta.validation.constraints.NotNull;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.LocalDate;

@Schema(
        name = "EquipeMembroDTO",
        description = "Nomeacao de colaborador a equipe (papel, vigencia de entrada e saida). "
                + "Materializa a composicao historica da equipe e suporta escala, disponibilidade e leitura de responsabilidade operacional.")
public class EquipeMembroDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @NotNull
    @UISchema(label = "Equipe", controlType = FieldControlType.ENTITY_LOOKUP, group = "Relacionamentos", order = 10,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Operations.EQUIPES_TEAM_LOOKUP_OPTIONS,
            tableHidden = true, icon = "groups")
    @Schema(
            description = "FK; unidade tactica (equipeId).")
    private Integer equipeId;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP, group = "Relacionamentos", order = 20,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            tableHidden = true, icon = "badge")
    @Schema(
            description = "FK; colaborador alocado (funcionarioId).")
    private Integer funcionarioId;

    // Campos de exibição (somente grade)
    @UISchema(label = "Equipe", readOnly = true, formHidden = true, group = "Relacionamentos", order = 11, icon = "groups")
    @Schema(
            description = "Nome da equipe denormalizado (read model).")
    private String equipeNome;

    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, group = "Relacionamentos", order = 21, icon = "badge")
    @Schema(
            description = "Nome do colaborador denormalizado (read model).")
    private String funcionarioNome;

    @UISchema(label = "Papel", controlType = FieldControlType.SELECT, group = "Principal", order = 10, icon = "label")
    @Schema(
            description = "Funcao no quadro; PapelEquipe.")
    private PapelEquipe papel;

    @NotNull
    @UISchema(label = "Data de Entrada", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, group = "Principal", order = 20, icon = "event")
    @Schema(
            description = "Inicio de vigencia na equipe (alistamento).")
    private LocalDate dataEntrada;

    @UISchema(label = "Data de Saída", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, group = "Principal", order = 30, icon = "event")
    @Schema(
            description = "Fim de vigencia; nulo se escalado de forma continua.")
    private LocalDate dataSaida;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getEquipeId() { return equipeId; }
    public void setEquipeId(Integer equipeId) { this.equipeId = equipeId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getEquipeNome() { return equipeNome; }
    public void setEquipeNome(String equipeNome) { this.equipeNome = equipeNome; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public void setFuncionarioNome(String funcionarioNome) { this.funcionarioNome = funcionarioNome; }
    public PapelEquipe getPapel() { return papel; }
    public void setPapel(PapelEquipe papel) { this.papel = papel; }
    public LocalDate getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDate dataEntrada) { this.dataEntrada = dataEntrada; }
    public LocalDate getDataSaida() { return dataSaida; }
    public void setDataSaida(LocalDate dataSaida) { this.dataSaida = dataSaida; }
}


