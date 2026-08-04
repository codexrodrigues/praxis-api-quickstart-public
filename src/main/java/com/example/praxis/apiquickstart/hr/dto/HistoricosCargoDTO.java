package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.LocalDate;

/**
 * DTO da movimentação de cargo do funcionário.
 *
 * <p>Registra a carreira funcional em termos de cargo, vigência
 * e observações, complementando o histórico salarial com a dimensão de lotação.
 */
@Schema(
        name = "HistoricosCargoDTO",
        description = "Movimentação de cargo (função) do colaborador no tempo: o papel organizacional, distinto do salário em HistoricoSalarialDTO. "
                + "Cada registro liga o funcionário a um cargo do catálogo e a uma vigência; compõe a trilha de carreira.")
public class HistoricosCargoDTO {
    @Schema(
            description = "Chave do período de lotação nesse cargo. O histórico é uma sequência de intervalos; sobreposições devem ser evitadas pela operação (regra fora do DTO).",
            example = "1")
    private Integer id;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP, required = true, icon = "badge", order = 10,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            tableHidden = true, helpText = "Colaborador promovido ou transferido.")
    @Schema(
            description = "Colaborador cuja carreira está sendo registrada; referência ao recurso de funcionário.",
            example = "2")
    private Integer funcionarioId;

    @NotNull
    @UISchema(label = "Cargo", controlType = FieldControlType.SELECT, required = true, icon = "work", order = 20,
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.CARGOS_JOB_ROLE_LOOKUP_OPTIONS,
            tableHidden = true, helpText = "Novo cargo assumido.")
    @Schema(
            description = "Referência a CargoDTO (catálogo de funções, nível e faixa salarial). Define o título e a função nesse período.",
            example = "3")
    private Integer cargoId;

    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, helpText = "Nome do colaborador (preenchido automaticamente).", icon = "badge")
    @Schema(description = "Nome do colaborador (leitura) para tabelas de carreira; espelha o funcionarioId.")
    private String funcionarioNome;

    @UISchema(label = "Cargo", readOnly = true, formHidden = true, helpText = "Nome do cargo (preenchido automaticamente).", icon = "work")
    @Schema(description = "Nome do cargo (leitura) alinhado ao cargoId para grelhas sem join explicito no cliente.")
    private String cargoNome;

    @NotNull
    @UISchema(label = "Data Início", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, required = true, helpText = "Primeiro dia de exercício no cargo.", icon = "event", order = 30)
    @Schema(
            description = "Início (inclusive) do exercício deste cargo pelo colaborador: transferências, promoções e reestruturações entram com nova data e novo cargo.",
            example = "2022-03-15")
    private LocalDate dataInicio;

    @UISchema(label = "Data Fim", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, helpText = "Último dia de exercício (se aplicável).", icon = "event_available", order = 40)
    @Schema(
            description = "Fim (inclusive) do período nesse cargo, ou nulo enquanto o colaborador o ocupa; encerre-o antes de atribuir outro registro consecutivo.",
            example = "2025-01-10")
    private LocalDate dataFim;

    @Size(max = 2000)
    @UISchema(label = "Observações", controlType = FieldControlType.TEXTAREA, maxLength = 2000, helpText = "Notas sobre a transição ou alocação.", icon = "notes", order = 50)
    @Schema(
            description = "Notas de RH (substituição, comissão temporária ou detalhe de alocação) complementares ao motivo de alteração salarial, se existir; texto livre para auditoria administrativa.",
            example = "Interino durante reforma de cargo superior")
    private String observacoes;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public Integer getCargoId() { return cargoId; }
    public void setCargoId(Integer cargoId) { this.cargoId = cargoId; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public void setFuncionarioNome(String funcionarioNome) { this.funcionarioNome = funcionarioNome; }
    public String getCargoNome() { return cargoNome; }
    public void setCargoNome(String cargoNome) { this.cargoNome = cargoNome; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
