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
        description = "Criterios de busca no relacionamento N-N heroi x habilidade (nao e a liga a persistir so por filtrar). "
                + "Afinidade e escala; GenericFilter / POST /filter (demo).")
public class FuncionarioHabilidadeFilterDTO implements GenericFilterDTO {
    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Filtrar pelo colaborador que possui a habilidade.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Habilidades de um colaborador; EQUAL (FK) (demo).")
    private Integer funcionarioId;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.HABILIDADES + "/options/filter", helpText = "Filtrar pela habilidade desejada.", icon = "psychology")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "habilidade.id")
    @Schema(
            description = "Quem detem esta competencia; EQUAL por Habilidade (demo).")
    private Integer habilidadeId;

    @UISchema(label = "Nível de Proficiência", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 30, helpText = "Filtrar por nível de proficiência.", icon = "trending_up")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "proficiencia")
    @Schema(
            description = "Nivel 1-10 (ou faixa de UI); BETWEEN sobre proficiencia (mapa de talento) (demo).")
    private List<Integer> proficienciaBetween;

    @UISchema(label = "Origem da Habilidade", controlType = FieldControlType.INPUT, maxLength = 120, order = 40, helpText = "Filtrar pela origem da proficiência.", icon = "psychology")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Origem certificacao, treino ou atribuicao (ex.: Academia, missao 42); LIKE (demo).")
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
