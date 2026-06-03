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
                + "GenericFilter / POST /filter no demo RH; cuidado com dados sensiveis em buscas amplas (demo).")
public class DependenteFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Nome do Dependente", controlType = FieldControlType.INPUT, maxLength = 200, order = 10, helpText = "Buscar dependentes por nome.", icon = "family_restroom")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Nome do dependente; LIKE em telas de atendimento (demo).")
    private String nomeCompleto;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Filtrar dependentes de um colaborador específico.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador titular; EQUAL por id (FK) para restringir dependencias (demo).")
    private Integer funcionarioId;

    @UISchema(label = "Grau de Parentesco", controlType = FieldControlType.INPUT, maxLength = 100, order = 30, helpText = "Filtrar pelo grau de parentesco.", icon = "family_restroom")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Filtro por grau (filho, conjuge, etc.); LIKE sobre o texto de parentesco.")
    private String parentesco;

    @UISchema(label = "Data de Nascimento", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 40, helpText = "Buscar nascidos entre duas datas.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataNascimento")
    @Schema(
            description = "Janela de nascimento (idade); BETWEEN duas LocalDate; ver tambem nascimento num dia (abaixo). (demo).")
    private List<LocalDate> dataNascimentoBetween;

    @UISchema(label = "Data de Nascimento (Na Data)", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 50, helpText = "Buscar nascidos em uma data específica.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.ON_DATE, relation = "dataNascimento")
    @Schema(
            description = "Nascimentos exatamente neste dia civil; operacao ON_DATE (criterio alternativo ao intervalo) (GenericFilter) (demo).")
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
