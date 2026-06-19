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
        name = "DependenteFilterDTO",
        description = "Criterios de busca de dependentes para IR/beneficio (nao e o registo Dependente em edicao). "
                + "Apoia atendimento cadastral, validacao de elegibilidade e revisao de vinculos familiares com minimizacao de dados pessoais.")
public class DependenteFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Nome do Dependente", controlType = FieldControlType.INPUT, maxLength = 200, order = 10, helpText = "Buscar dependentes por nome.", icon = "family_restroom")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Nome civil do dependente usado para localizar vinculos familiares em atendimento ou revisao cadastral.")
    private String nomeCompleto;

    @UISchema(label = "Colaborador titular", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, helpText = "Filtrar dependentes de um colaborador específico.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador titular do vinculo familiar; restringe a busca aos dependentes associados a uma pessoa especifica.")
    private Integer funcionarioId;

    @UISchema(label = "Grau de Parentesco", controlType = FieldControlType.INPUT, maxLength = 100, order = 30, helpText = "Filtrar pelo grau de parentesco.", icon = "family_restroom")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Filtro por grau (filho, conjuge, etc.); LIKE sobre o texto de parentesco.")
    private String parentesco;

    @UISchema(label = "Data de Nascimento", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 40, helpText = "Buscar nascidos entre duas datas.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataNascimento")
    @Schema(
            description = "Intervalo de datas de nascimento usado para estimar faixa etaria e validar elegibilidade temporal de beneficios.")
    private List<LocalDate> dataNascimentoBetween;

    @UISchema(label = "Nascimento em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 50, helpText = "Busca dependentes nascidos em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "dataNascimento")
    @Schema(
            description = "Data civil exata de nascimento quando a consulta precisa confirmar identidade ou localizar aniversariantes especificos.")
    private LocalDate dataNascimentoOn;

    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
    public List<LocalDate> getDataNascimentoBetween() { return dataNascimentoBetween; }
    public void setDataNascimentoBetween(List<LocalDate> dataNascimentoBetween) { this.dataNascimentoBetween = dataNascimentoBetween; }
    public LocalDate getDataNascimentoOn() { return dataNascimentoOn; }
    public void setDataNascimentoOn(LocalDate dataNascimentoOn) { this.dataNascimentoOn = dataNascimentoOn; }
}
