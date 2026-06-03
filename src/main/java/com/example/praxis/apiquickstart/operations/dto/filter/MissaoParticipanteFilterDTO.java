package com.example.praxis.apiquickstart.operations.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.enums.PapelMissao;
import com.example.praxis.apiquickstart.operations.enums.ResultadoMissao;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "MissaoParticipanteFilterDTO",
        description = "Criterios de busca na escala missao x heroi (papel, lider, resultado); nao e a alocacao a editar so com filtrar. "
                + "GenericFilter / POST /filter (demo).")
public class MissaoParticipanteFilterDTO implements GenericFilterDTO {
    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.MISSOES + "/options/filter", icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "missao.id")
    @Schema(
            description = "Apenas escalados nesta missao; EQUAL (FK) (demo).")
    private Integer missaoId;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Todas as missoes de um colaborador; EQUAL (FK) (demo).")
    private Integer funcionarioId;

    @UISchema(controlType = FieldControlType.SELECT, order = 30, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Papel unico; EQUAL PapelMissao (lider, suporte, etc.) (demo).")
    private PapelMissao papel;

    @UISchema(type = FieldDataType.BOOLEAN, controlType = FieldControlType.TOGGLE, order = 40, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Se e referencia/ lider de pelotao; EQUAL boolean (demo).")
    private Boolean principal;

    @UISchema(controlType = FieldControlType.SELECT, order = 50, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Resultado pessoal no encerramento; EQUAL ResultadoMissao (demo).")
    private ResultadoMissao resultado;

    public Integer getMissaoId() { return missaoId; }
    public void setMissaoId(Integer missaoId) { this.missaoId = missaoId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public PapelMissao getPapel() { return papel; }
    public void setPapel(PapelMissao papel) { this.papel = papel; }
    public Boolean getPrincipal() { return principal; }
    public void setPrincipal(Boolean principal) { this.principal = principal; }
    public ResultadoMissao getResultado() { return resultado; }
    public void setResultado(ResultadoMissao resultado) { this.resultado = resultado; }
}
