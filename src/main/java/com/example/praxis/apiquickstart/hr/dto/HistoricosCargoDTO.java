package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.LocalDate;

/**
 * DTO da movimentacao de cargo do funcionario.
 *
 * <p>Registra a carreira funcional em termos de cargo, vigencia
 * e observacoes, complementando o historico salarial com a dimensao de lotacao.
 */
@Schema(
        name = "HistoricosCargoDTO",
        description = "Movimentacao de cargo (funcao) do colaborador no tempo: o papel organizacional, distinto do salario em HistoricoSalarialDTO. "
                + "Cada registo liga o funcionario a um cargo do catalogo e vigencia; compoem a trilha de carreira (demo).")
public class HistoricosCargoDTO {
    @Schema(
            description = "Chave do periodo de lotacao nesse cargo. Historico e uma sequencia de intervalos; solapamentos devem ser evitados na operacao (regra fora do DTO).",
            example = "1")
    private Integer id;

    @NotNull
    @UISchema(label = "Funcionário", controlType = FieldControlType.SELECT, required = true, icon = "badge",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS,
            tableHidden = true, helpText = "Colaborador promovido ou transferido.")
    @Schema(
            description = "Colaborador cuja carreira (cargo) se esta a registar; FK ao recurso de funcionario.",
            example = "2")
    private Integer funcionarioId;

    @NotNull
    @UISchema(label = "Cargo", controlType = FieldControlType.SELECT, required = true, icon = "work",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.CARGOS,
            tableHidden = true, helpText = "Novo cargo assumido.")
    @Schema(
            description = "Referencia a CargoDTO (catalogo de funcoes, nivel, faixa salarial de referencia). Define o titulo/funcao nesse periodo.",
            example = "3")
    private Integer cargoId;

    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, helpText = "Nome do colaborador (preenchido automaticamente).", icon = "badge")
    @Schema(description = "Nome do colaborador (leitura) para tabelas de carreira; espelha o funcionarioId.")
    private String funcionarioNome;

    @UISchema(label = "Cargo", readOnly = true, formHidden = true, helpText = "Nome do cargo (preenchido automaticamente).", icon = "work")
    @Schema(description = "Nome do cargo (leitura) alinhado ao cargoId para grelhas sem join explicito no cliente.")
    private String cargoNome;

    @NotNull
    @UISchema(label = "Data Início", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, required = true, helpText = "Primeiro dia de exercício no cargo.", icon = "event")
    @Schema(
            description = "Inicio (inclusive) do exercicio deste cargo pelo colaborador: transferencias, promocoes e reestruturacoes entram com nova data e novo cargo (demo).",
            example = "2022-03-15")
    private LocalDate dataInicio;

    @UISchema(label = "Data Fim", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, helpText = "Último dia de exercício (se aplicável).", icon = "event_available")
    @Schema(
            description = "Fim (inclusive) do periodo nesse cargo ou nulo se ainda o titular ocupa; encerrar antes de atribuir outro registo consecutivo.",
            example = "2025-01-10")
    private LocalDate dataFim;

    @Size(max = 2000)
    @UISchema(label = "Observações", controlType = FieldControlType.TEXTAREA, maxLength = 2000, helpText = "Notas sobre a transição ou alocação.", icon = "notes")
    @Schema(
            description = "Notas de RH (substituicao, comissao temporaria, detalhe de alocacao) complementares ao motivo de alteracao salarial, se existir; livre, auditavel em sentido lato (demo).",
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
