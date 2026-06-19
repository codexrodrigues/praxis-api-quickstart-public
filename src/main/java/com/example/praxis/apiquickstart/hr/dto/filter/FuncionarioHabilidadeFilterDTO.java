package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.util.List;

@Schema(
        name = "FuncionarioHabilidadeFilterDTO",
        description = "Criterios de busca na matriz de habilidades de colaboradores. "
                + "Relaciona pessoas, capacidades, nivel de proficiencia e origem da competencia para selecao operacional.")
public class FuncionarioHabilidadeFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Colaborador", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, helpText = "Filtrar pelo colaborador que possui a habilidade.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador cujas habilidades e proficiencias devem ser consultadas.")
    private Integer funcionarioId;

    @UISchema(label = "Habilidade", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.HABILIDADES_SKILL_LOOKUP_OPTIONS, helpText = "Filtrar pela habilidade desejada.", icon = "psychology")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "habilidade.id")
    @Schema(
            description = "Habilidade especifica usada para localizar colaboradores que possuem essa competencia.")
    private Integer habilidadeId;

    @UISchema(label = "Nível de Proficiência", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 30, helpText = "Filtrar por nível de proficiência.", icon = "trending_up")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "proficiencia")
    @Schema(
            description = "Faixa de proficiencia atribuida ao colaborador naquela habilidade, usada em mapa de talentos e escala operacional.")
    private List<Integer> proficienciaBetween;

    @UISchema(label = "Origem da Habilidade", controlType = FieldControlType.INPUT, maxLength = 120, order = 40, helpText = "Filtrar pela origem da proficiência.", icon = "psychology")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho da origem da competencia, como treinamento, certificacao, avaliacao ou missao que comprovou a habilidade.")
    private String origem;

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public Integer getHabilidadeId() { return habilidadeId; }
    public void setHabilidadeId(Integer habilidadeId) { this.habilidadeId = habilidadeId; }
    public List<Integer> getProficienciaBetween() { return proficienciaBetween; }
    public void setProficienciaBetween(List<Integer> proficienciaBetween) { this.proficienciaBetween = proficienciaBetween; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
}
